import org.scalatest.{ Matchers, WordSpecLike }

final class LogWriterSyntaxResolutionTest extends WordSpecLike with Matchers {

  "the LogWriter's syntax" should {

    "be inferred without extra import" in {

      """
        |import log.effect.LogWriter
        |
        |def l[F[_]]: LogWriter[F] = ???
        |
        |l.trace("test")
        |l.trace("test", new Throwable("test"))
        |
        |l.debug("test")
        |l.debug("test", new Throwable("test"))
        |
        |l.info("test")
        |l.info("test", new Throwable("test"))
        |
        |l.error("test")
        |l.error("test", new Throwable("test"))
        |
        |l.warn("test")
        |l.warn("test", new Throwable("test"))
      """.stripMargin should compile
    }
  }

  "the LogWriter's alias for the singleton companion" should {

    "be inferred allowing a boilerplate free mtl-style syntax" in {

      """
        |import log.effect.LogWriter
        |import log.effect.Trace
        |import cats.instances.string.catsStdShowForString
        |
        |def f[F[_]: LogWriter] = LogWriter.write(Trace, "test")
      """.stripMargin should compile
    }

    "be inferred allowing a boilerplate free mtl-style syntax for errors" in {

      """
        |import log.effect.LogWriter
        |import log.effect.Error
        |import log.effect.LogWriter.Failure
        |import cats.instances.string.catsStdShowForString
        |
        |def f[F[_]: LogWriter] = LogWriter.write(Error, Failure("test", new Exception("test exception")))
      """.stripMargin should compile
    }
  }
}
