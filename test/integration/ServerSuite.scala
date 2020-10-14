package integration


import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import scala.concurrent.{ExecutionContext, Future}

class ServerSuite extends PlaySpec with GuiceOneServerPerSuite with Results with Injecting {

  "Router should respond with Not Found for invalid path" in {
    val wsClient              = app.injector.instanceOf[WSClient]
    val myPublicAddress       = s"localhost:$port"
    val baseURL = s"http://$myPublicAddress"
    val invalidURL = baseURL + "/callback"
    val response = await(wsClient.url(invalidURL).get())

    response.status mustBe 404
    response.json mustBe Json.parse("""{"error": "Requested resource was not found"}""")
  }

}