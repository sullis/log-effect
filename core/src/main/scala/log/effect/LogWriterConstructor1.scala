package log.effect

import java.util.{ logging => jul }

import com.github.ghik.silencer.silent
import log.effect.LogWriter.{ Failure, Jul, Log4s, Scribe }
import log.effect.internal.syntax._
import log.effect.internal.{ EffectSuspension, Functor, Show }
import org.{ log4s => l4s }

sealed trait LogWriterConstructor1[T, F[_]] {
  type LogWriterType
  def construction: F[LogWriterType] => F[LogWriter[F]]
}

object LogWriterConstructor1 extends LogWriterConstructor1Instances {

  @inline final def apply[F[_]]: LogWriterConstructor1Partially[F] =
    new LogWriterConstructor1Partially()

  private[effect] type AUX[T, F[_], LWT] =
    LogWriterConstructor1[T, F] { type LogWriterType = LWT }

  final private[effect] class LogWriterConstructor1Partially[F[_]](private val d: Boolean = true)
      extends AnyVal {

    @inline @silent def apply[T](t: T)(
      implicit F: EffectSuspension[F],
      LWC: LogWriterConstructor1[T, F]
    ): F[LWC.LogWriterType] => F[LogWriter[F]] =
      LWC.construction
  }
}

sealed private[effect] trait LogWriterConstructor1Instances {

  implicit def log4sConstructor[F[_]: Functor](
    implicit F: EffectSuspension[F]
  ): LogWriterConstructor1.AUX[Log4s, F, l4s.Logger] =
    new LogWriterConstructor1[Log4s, F] {

      type LogWriterType = l4s.Logger

      val construction: F[LogWriterType] => F[LogWriter[F]] =
        _ map { l4sLogger =>
          new LogWriter[F] {
            def write[A: Show, L <: LogLevel: Show](level: L, a: =>A): F[Unit] = {

              val beLevel = level match {
                case LogLevels.Trace => l4s.Trace
                case LogLevels.Debug => l4s.Debug
                case LogLevels.Info  => l4s.Info
                case LogLevels.Error => l4s.Error
                case LogLevels.Warn  => l4s.Warn
              }

              F.suspend(
                a match {
                  case Failure(msg, th) => l4sLogger(beLevel)(th)(msg)
                  case _                => l4sLogger(beLevel)(a.show)
                }
              )
            }
          }
        }
    }

  implicit def julConstructor[F[_]: Functor](
    implicit F: EffectSuspension[F]
  ): LogWriterConstructor1.AUX[Jul, F, jul.Logger] =
    new LogWriterConstructor1[Jul, F] {

      type LogWriterType = jul.Logger

      val construction: F[LogWriterType] => F[LogWriter[F]] =
        _ map { julLogger =>
          new LogWriter[F] {
            def write[A: Show, L <: LogLevel: Show](level: L, a: =>A): F[Unit] = {

              val beLevel = level match {
                case LogLevels.Trace => jul.Level.FINEST
                case LogLevels.Debug => jul.Level.FINE
                case LogLevels.Info  => jul.Level.INFO
                case LogLevels.Warn  => jul.Level.WARNING
                case LogLevels.Error => jul.Level.SEVERE
              }

              F.suspend(
                if (julLogger.isLoggable(beLevel)) {
                  julLogger.log(
                    a match {
                      case Failure(msg, th) =>
                        val r = new jul.LogRecord(beLevel, msg)
                        r.setThrown(th)
                        r
                      case _ => new jul.LogRecord(beLevel, a.show)
                    }
                  )
                }
              )
            }
          }
        }
    }

  implicit def scribeConstructor[F[_]: Functor](
    implicit F: EffectSuspension[F]
  ): LogWriterConstructor1.AUX[Scribe, F, scribe.Logger] =
    new LogWriterConstructor1[Scribe, F] {

      type LogWriterType = scribe.Logger

      val construction: F[LogWriterType] => F[LogWriter[F]] =
        _ map { scribeLogger =>
          new LogWriter[F] {
            def write[A: Show, L <: LogLevel: Show](level: L, a: =>A): F[Unit] = {

              val beLevel = level match {
                case LogLevels.Trace => scribe.Level.Trace
                case LogLevels.Debug => scribe.Level.Debug
                case LogLevels.Info  => scribe.Level.Info
                case LogLevels.Warn  => scribe.Level.Warn
                case LogLevels.Error => scribe.Level.Error
              }

              F.suspend(
                a match {
                  case Failure(msg, th) => scribeLogger.log(beLevel, msg, Some(th))
                  case _                => scribeLogger.log(beLevel, a.show, None)
                }
              )
            }
          }
        }
    }
}
