package log.effect

import java.util.{ logging => jul }

import cats.Show
import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.show._
import com.github.ghik.silencer.silent
import log.effect.LogWriter.{ Failure, Jul, Log4s }
import org.{ log4s => l4s }

sealed trait LogWriterConstructor1[T, F[_]] {
  type LogWriterType
  def evaluation: F[LogWriterType] => F[LogWriter[F]]
}

object LogWriterConstructor1 extends LogWriterConstructor1Instances {

  @inline final def apply[F[_]]: LogWriterConstructor1Partially[F] =
    new LogWriterConstructor1Partially()

  private[effect] type AUX[T, F[_], LWT] =
    LogWriterConstructor1[T, F] { type LogWriterType = LWT }

  final private[effect] class LogWriterConstructor1Partially[F[_]](private val d: Boolean = true)
      extends AnyVal {

    @inline @silent def apply[T](t: T)(
      implicit F: Sync[F],
      LWC: LogWriterConstructor1[T, F]
    ): F[LWC.LogWriterType] => F[LogWriter[F]] =
      LWC.evaluation
  }
}

sealed private[effect] trait LogWriterConstructor1Instances {

  implicit def log4sConstructor[F[_]](
    implicit F: Sync[F]
  ): LogWriterConstructor1.AUX[Log4s, F, l4s.Logger] =
    new LogWriterConstructor1[Log4s, F] {

      type LogWriterType = l4s.Logger

      def evaluation: F[LogWriterType] => F[LogWriter[F]] =
        _ map { l4sLogger =>
          new LogWriter[F] {
            def write[A: Show, L <: LogLevel: Show](level: L, a: =>A): F[Unit] = {

              val l4sLevel = level match {
                case LogLevels.Trace => l4s.Trace
                case LogLevels.Debug => l4s.Debug
                case LogLevels.Info  => l4s.Info
                case LogLevels.Error => l4s.Error
                case LogLevels.Warn  => l4s.Warn
              }

              F.delay {
                a match {
                  case Failure(msg, th) => l4sLogger(l4sLevel)(th)(msg)
                  case _                => l4sLogger(l4sLevel)(a.show)
                }
              }
            }
          }
        }
    }

  implicit def julConstructor[F[_]](
    implicit F: Sync[F]
  ): LogWriterConstructor1.AUX[Jul, F, jul.Logger] =
    new LogWriterConstructor1[Jul, F] {

      type LogWriterType = jul.Logger

      def evaluation: F[LogWriterType] => F[LogWriter[F]] =
        _ map { julLogger =>
          new LogWriter[F] {
            def write[A: Show, L <: LogLevel: Show](level: L, a: =>A): F[Unit] = {

              val jdkLevel = level match {
                case LogLevels.Trace => jul.Level.FINEST
                case LogLevels.Debug => jul.Level.FINE
                case LogLevels.Info  => jul.Level.INFO
                case LogLevels.Warn  => jul.Level.WARNING
                case LogLevels.Error => jul.Level.SEVERE
              }

              F.delay {
                if (julLogger.isLoggable(jdkLevel)) {
                  julLogger.log(
                    a match {
                      case Failure(msg, th) =>
                        val r = new jul.LogRecord(jdkLevel, msg)
                        r.setThrown(th)
                        r
                      case _ => new jul.LogRecord(jdkLevel, a.show)
                    }
                  )
                }
              }
            }
          }
        }
    }
}
