import sbt._

object Dependencies {


  lazy val appDependencies: Seq[ModuleID] = AppDependencies.compile ++ AppDependencies.test ++ compile ++ test()


  val compile = Seq(
    "com.github.pureconfig" %% "pureconfig" % "0.10.2",
    "uk.gov.service.notify" % "notifications-java-client" % "3.14.2-RELEASE"
  )

  def test(scope: String = "test,it") = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % scope,
    "org.scalacheck" %% "scalacheck" % "1.14.0" % scope,
    "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % scope,
    "org.jsoup" % "jsoup" % "1.11.3" % scope
  )
}
