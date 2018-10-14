package log.effect

import com.github.ghik.silencer.silent
import log.effect.LogWriter.{ Console, NoOp }
import log.effect.internal.syntax._
import log.effect.internal.{ EffectSuspension, NoAction, Show }

trait LogWriterConstructor0[T, F[_]] {

  def construction[LL <: LogLevel]: LL => LogWriter[F]

  def construction: () => LogWriter[F] =
    () => construction(LogLevels.Trace)
}

object LogWriterConstructor0 extends LogWriterConstructor0Instances {

  @inline final def apply[F[_]]: LogWriterConstructor0Partially[F] =
    new LogWriterConstructor0Partially()

  final private[effect] class LogWriterConstructor0Partially[F[_]](private val d: Boolean = true)
      extends AnyVal {

    @inline @silent def apply[T](t: T)(
      implicit LWC: LogWriterConstructor0[T, F]
    ): () => LogWriter[F] = () => LWC.construction()

    @inline @silent def apply[T, LL <: LogLevel](t: T, minLevel: LL)(
      implicit LWC: LogWriterConstructor0[T, F]
    ): () => LogWriter[F] = () => LWC.construction(minLevel)
  }
}

sealed private[effect] trait LogWriterConstructor0Instances {

  implicit def consoleConstructor0[F[_]](
    implicit F: EffectSuspension[F]
  ): LogWriterConstructor0[Console, F] =
    new LogWriterConstructor0[Console, F] {

      def construction[LL <: LogLevel]: LL => LogWriter[F] =
        ll =>
          new LogWriter[F] {
            private val minLogLevel = ll

            def write[A: Show, L <: LogLevel: Show](level: L, a: =>A): F[Unit] =
              if (level >= minLogLevel)
                F.suspend(
                  println(
                    s"[${level.show.toLowerCase}] - [${Thread.currentThread().getName}] ${a.show}"
                  )
                )
              else F.unit
        }
    }

  implicit def noOpConstructor0[F[_]](
    implicit F: NoAction[F]
  ): LogWriterConstructor0[NoOp, F] =
    new LogWriterConstructor0[NoOp, F] {

      def construction[LL <: LogLevel]: LL => LogWriter[F] =
        _ =>
          new LogWriter[F] {
            def write[A: Show, L <: LogLevel: Show](level: L, a: =>A): F[Unit] = F.unit
        }
    }
}
