package integration

import controllers.Entities
import models.{DBTables, StateTransitionTable}
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import views.ErrorBody
import ErrorBody._

import scala.concurrent.{ExecutionContext, Future}

//TODO: make tests run sequentially or else race condition in a shared db could occur
class EntitiesControllerSuite extends PlaySpec with GuiceOneAppPerSuite with Results with Injecting with BeforeAndAfterAll {
  override def beforeAll(): Unit = {
    implicit val execCtx = inject[ExecutionContext]
    val tables = inject[DBTables]
    val initState = "init"
    val states = Map(initState -> Set("finished"))
    val stt = StateTransitionTable(initState, states)
    await(
      for {
        _ <- tables.clearAll()
        _ <- tables.replaceStt(stt)
      } yield None
    )
  }

  "Entities controller" should {
    val entitiesController = new Entities(inject[DBTables], inject[DBTables], Helpers.stubControllerComponents())(inject[ExecutionContext])
    val entity1NameJs      = Json.parse("""{"name": "1"}""")
    val entity1Js          = Json.parse("""{"entity": {"name": "1", "state": "init"}}""")
    val entityNotExists    = Json.toJson(ErrorBody("Requested entity does not exist"))

    "create an entity and initialize it to init state" in {
      val request                = FakeRequest(POST, "/entities").withBody(entity1NameJs)
      val result: Future[Result] = entitiesController.create().apply(request)
      val responseBody           = contentAsJson(result)

      status(result) mustBe 201
      responseBody mustBe entity1Js
    }

    "respond with 400 Bad Request to wrong JSON" in {
      val request                = FakeRequest(POST, "/entities").withBody(Json.toJson(ErrorBody("something")))
      val result: Future[Result] = entitiesController.create().apply(request)
      val responseBody           = contentAsJson(result)

      status(result) mustBe 400
      responseBody mustBe Json.parse("""{"error": "Could not parse entity name from body"}""")
    }

    "respond with Conflict when asked to create an existing entity" in {
      val request                = FakeRequest(POST, "/entities").withBody(entity1NameJs)
      val result: Future[Result] = entitiesController.create().apply(request)
      val responseBody           = contentAsJson(result)

      status(result) mustBe 409
      responseBody mustBe Json.toJson(ErrorBody("This entity already exists"))
    }

    "show an existing entity" in {
      val request                = FakeRequest(GET, "/entities/1")
      val result: Future[Result] = entitiesController.show("1").apply(request)
      val responseBody           = contentAsJson(result)

      status(result) mustBe 200
      responseBody mustBe entity1Js
    }

    "respond with NotFound about a non-existent entity" in {
      val request                = FakeRequest(GET, "/entities/1a")
      val result: Future[Result] = entitiesController.show("1a").apply(request)
      val responseBody           = contentAsJson(result)

      status(result) mustBe 404
      responseBody mustBe entityNotExists
    }

    "delete an entity successfully" in {
      val deleteRequest          = FakeRequest(DELETE, "/entities/1")
      val deleteResult           = entitiesController.delete("1").apply(deleteRequest)

      status(deleteResult) mustBe 204

      val getRequest             = FakeRequest(GET, "/entities/1")
      val getResult              = entitiesController.show("1").apply(getRequest)
      val responseBody           = contentAsJson(getResult)

      status(getResult) mustBe 404
      responseBody mustBe entityNotExists
    }

  }
}