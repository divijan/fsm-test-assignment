package unit

import models.StateTransitionTable
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import views.StateTransitionTableRW._

class JsonWritesSuite extends PlaySpec {
  "JsonWrites" should {
    "write a state transition table representation correctly" in {
      val stt = StateTransitionTable("init", Map(
        "init" -> Set("pending", "finished"),
        "pending" -> Set("finished")
      ))
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
      Json.toJson(stt) mustBe expectedJson
    }

  }
}
