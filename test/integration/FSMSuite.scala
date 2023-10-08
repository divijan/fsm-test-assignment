package integration

import models.{Entity, Transition}
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import views.{EntityRW, ErrorBody, TransitionRW}
import TransitionRW._
import EntityRW._
import models.db.slick.DBTables

import scala.concurrent.Future
import scala.util.Using

class FSMSuite extends PlaySpec with GuiceOneAppPerSuite with Results with Injecting
    with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    val db = inject[DBTables]
    db.clearAll()
  }

  "FSM application" should {
    val statesJs               = Using(getClass.getResourceAsStream("../states.json"))(Json.parse _).get

    "fail to get a non-existent STT" in {
      val request                = FakeRequest(GET, "/states")
      val result: Future[Result] = route(app, request).get
      val bodyJs                 = contentAsJson(result)

      status(result) mustBe 404
      bodyJs mustBe Json.parse("""{"error": "State Transition Table is not defined in the system"}""")
    }

    "return an empty list when no transitions in the log" in {
      val request = FakeRequest(GET, "/transitions")
      val result = route(app, request).get
      val bodyJs = contentAsJson(result)

      status(result) mustBe 200
      bodyJs mustEqual Json.parse("""{ "transitions": [] }""")
    }

    "respond with Not Found if trying to get transitions for non-existent entity" in {
      val getTransitionsFor1 = FakeRequest(GET, "/transitions/1")
      val transitionsResult = route(app, getTransitionsFor1).get
      val errorJs = contentAsJson(transitionsResult)

      status(transitionsResult) mustBe 404
      errorJs mustEqual Json.parse("""{ "error": "This entity does not exist" }""")
    }

    "get an empty list of entities when there are none" in {
      val request = FakeRequest(GET, "/entities")
      val result = route(app, request).get
      val bodyJs = contentAsJson(result)

      status(result) mustBe 200
      bodyJs mustEqual Json.parse("""{"entities": []}""")
    }

    "refuse to create an entity without STT" in {
      val createdEntity = route(app, FakeRequest(POST, "/entities").withBody(Json.parse("""{ "name": "1" }"""))).get

      status(createdEntity) mustBe 400
      contentAsJson(createdEntity) mustEqual Json.toJson(ErrorBody("Cannot create an entity with no STT in the system"))
    }

    "return a single transition for just created entity" in {
      val createSTTRequest       = FakeRequest(POST, "/states").withBody(statesJs)
      val createdSTT = route(app, createSTTRequest).get
      status(createdSTT) mustBe 201

      val createdEntity = route(app, FakeRequest(POST, "/entities").withBody(Json.parse("""{ "name": "1" }"""))).get
      status(createdEntity) mustBe 201

      val getTransitionsFor1 = FakeRequest(GET, "/transitions/1")
      val transitionsResult = route(app, getTransitionsFor1).get
      val bodyJs = contentAsJson(transitionsResult)
      val transitions = bodyJs.as[Seq[Transition]]

      status(transitionsResult) mustBe 200
      transitions.size mustEqual 1
      val t = transitions.head
      t.entity mustEqual "1"
      t.from mustBe None
      t.to mustBe "init"
    }

    "refuse to make an invalid transition" in {
      val moveRequest = FakeRequest(PATCH, "/entities/1/state").withBody(Json.parse("""{ "state": "finished" }"""))
      val result = route(app, moveRequest).get
      val errorJs = contentAsJson(result)

      status(result) mustBe 400
      errorJs mustEqual Json.parse("""{"error": "Requested transition is invalid"}""")
    }

    "refuse to move a non-existent entity" in {
      val moveRequest = FakeRequest(PATCH, "/entities/2/state").withBody(Json.parse("""{ "state": "pending" }"""))
      val result = route(app, moveRequest).get
      val errorJs = contentAsJson(result)

      status(result) mustBe 404
      errorJs mustEqual Json.parse("""{"error": "This entity does not exist"}""")
    }

    "make a valid transition" in {
      val moveRequest = FakeRequest(PATCH, "/entities/1/state").withBody(Json.parse("""{ "state": "pending" }"""))
      val result = route(app, moveRequest).get
      val bodyJs = contentAsJson(result)
      val response = bodyJs.as[Entity]

      status(result) mustBe 200
      response.state mustEqual "pending"
    }

    "still return a valid log of transitions for one entity when second entity exists" in {
      val createdEntity = route(app, FakeRequest(POST, "/entities").withBody(Json.parse("""{ "name": "2" }"""))).get
      status(createdEntity) mustBe 201

      val getTransitionsFor1 = FakeRequest(GET, "/transitions/1")
      val transitionsResult = route(app, getTransitionsFor1).get
      val bodyJs = contentAsJson(transitionsResult)
      val transitions = bodyJs.as[Seq[Transition]]

      transitions.size mustBe 2
      transitions.map(t => (t.entity, t.from, t.to)) mustEqual Seq(("1", None, "init"), ("1", Some("init"), "pending"))
    }

    "refuse to reset an entity in init state" in {
      val resetReq = FakeRequest(PATCH, "/entities/2")
      val resetResponse = route(app, resetReq).get
      val errorJs = contentAsJson(resetResponse)

      status(resetResponse) mustBe 400
      errorJs mustEqual Json.parse("""{ "error": "Will not reset an entity that is already in init state" }""")
    }

    "reset an entity correctly" in {
      val resetReq = FakeRequest(PATCH, "/entities/1")
      val resetResponse = route(app, resetReq).get

      status(resetResponse) mustBe 200

      val transitions = await(inject[DBTables].getTransitionsFor("1"))
      val tWithoutTimestamps = transitions.map { t =>
        val Transition(_, from, to, _) = t
        (from, to)
      }
      tWithoutTimestamps mustEqual Seq(
        (None, "init"),
        (Some("init"), "pending"),
        (None, "init")
      )
    }

    "replace an STT" in {
      val db = inject[DBTables]
      val states2 = Using(getClass.getResourceAsStream("../states2.json"))(Json.parse _).get

      val createSTTRequest = FakeRequest(POST, "/states").withBody(states2)
      val createdSTT = route(app, createSTTRequest).get
      status(createdSTT) mustBe 201

      val entitiesAfter = await(db.getEntities())
      entitiesAfter must contain only (Entity("1", "s0"), Entity("2", "s0"))

      val transitionsAfter = await(db.getTransitions())
      transitionsAfter.map(_.entity) must contain only ("1", "2")
      transitionsAfter.map(_.to) must contain only ("s0")
    }

    "drop transition log when deleting an entity" in {
      val deleteReq = FakeRequest(DELETE, "/entities/1")
      val deleteResponse = route(app, deleteReq).get
      status(deleteResponse) must be(204)

      val transitions = await(inject[DBTables].getTransitionsFor("1"))
      transitions mustBe empty
    }

  }
}
