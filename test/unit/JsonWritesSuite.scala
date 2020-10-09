package unit

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import views.State
import org.scalatestplus.play._
import play.api.test._
//import views.StateTransitionTable._

class JsonWritesSuite extends PlaySpec {
  "JsonWrites" should {
    "write a state transition table representation correctly" in {
      val stt = Set(
        State("init", true, Set("pending", "finished")),
        State("pending", false, Set("finished"))
      )
      val expectedJson = Json.parse(
        """{"states": [
          |  {
          |    "name": "init",
          |    "isInit": true,
          |    "transitions": ["pending","finished"]
          |  },
          |  {
          |    "name": "pending",
          |    "transitions": ["finished"]
          |  }
          |]}""".stripMargin)
      Json.toJson(stt)(views.StateTransitionTable.stateTransitionTableWrites) mustBe expectedJson
    }

  }
}
