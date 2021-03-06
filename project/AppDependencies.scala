import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile = Seq(

    "uk.gov.hmrc"             %% "govuk-template"           % "5.26.0-play-25",
    "uk.gov.hmrc"             %% "play-ui"                  % "7.32.0-play-25",
    "uk.gov.hmrc"             %% "bootstrap-play-25"        % "4.9.0",
    "org.scalaz"              %% "scalaz-core"              % "7.2.27",
    "uk.gov.hmrc"             %% "customs-api-common"       % "1.36.0"
  )

  val test = Seq(
    "org.scalatest"           %% "scalatest"                % "3.0.4"                 % "test",
    "org.jsoup"               %  "jsoup"                    % "1.10.2"                % "test",
    "com.typesafe.play"       %% "play-test"                % current                 % "test",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"                 % "test, it",
    "uk.gov.hmrc"             %% "service-integration-test" % "0.2.0"                 % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "2.0.0"                 % "test, it"
  )

}
