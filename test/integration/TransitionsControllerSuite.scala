package integration

import controllers.States
import models.DBTables
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import views.StateTransitionTable
import views.StateTransitionTable._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

class TransitionsControllerSuite extends PlaySpec with GuiceOneAppPerSuite with Results with Injecting {
  //TODO: test no STT error message
  "States controller" should {
    val statesController = new States(inject[DBTables], Helpers.stubControllerComponents())(inject[ExecutionContext])
    val statesJs               = Using(getClass.getResourceAsStream("../states.json"))(Json.parse _).get

    "update state transition table" in {
      val request                = FakeRequest(POST, "/states").withBody(statesJs)
      val result: Future[Result] = statesController.replace().apply(request)
      val body                   = contentAsJson(result)

      status(result) mustBe 201
      body mustBe statesJs
    }

    "return the same state transition table" in {
      val request                = FakeRequest(GET, "/states")
      val result: Future[Result] = statesController.index().apply(request)
      val bodyJs                 = contentAsJson(result)
      val receivedStt            = bodyJs.as[StateTransitionTable]

      status(result) mustBe 200
      receivedStt.copy(table = receivedStt.table concat Seq("closed" -> Set.empty[String])) mustEqual statesJs.as[StateTransitionTable]
    }

    def invalidSttTest(description: String)(invalidTable: String) = description in {
      import play.api.libs.json.Reads._
      import play.api.libs.json._

      val noInitTable = Json.parse(invalidTable)
      val noInitJs = Json.toJson(noInitTable)
      val request                = FakeRequest(POST, "/states").withBody(noInitJs)
      val result: Future[Result] = statesController.replace().apply(request)
      val body                   = contentAsJson(result)

      status(result) mustBe 400
      (body \ "error").as[String] mustBe "There should be exactly one init state!"
    }

    invalidSttTest("refuse to update state transition table with one with no init states")(
      """{"states": [
        |  {
        |    "name": "init",
        |    "transitions": ["pending","finished"]
        |  },
        |  {
        |    "name": "pending",
        |    "transitions": ["finished"]
        |  }
        |]}""".stripMargin)

    invalidSttTest("refuse to update state transition table with one with two init states")(
      """{"states": [
        |  {
        |    "name": "init",
        |    "isInit": true,
        |    "transitions": ["pending","finished"]
        |  },
        |  {
        |    "name": "pending",
        |    "isInit": true,
        |    "transitions": ["finished"]
        |  }
        |]}""".stripMargin)
  }
}