package unit

import controllers.States
import models.DBTables
import org.scalatest.TestSuite
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

class DBSuite extends PlaySpec with GuiceOneAppPerSuite with Injecting {
  "States table" should {
    "store and retrieve states" in {
      val tables = inject[DBTables]
      implicit val ec = inject[ExecutionContext]
      val start = "Start"
      val states = Seq(start -> "finish") //Using(getClass.getResourceAsStream("../states.json"))(Json.parse _).get.as[Seq[State]]

      val readStt = for {
        _ <- tables.replaceSTT(start, states)
        read <- tables.getSTT
      } yield read

      readStt.map { case (init, seq) =>
        init mustBe "start"
        seq mustEqual states
      }
      val (init, seq) = await(readStt)
      init mustBe start
      seq mustEqual states
    }
  }
}