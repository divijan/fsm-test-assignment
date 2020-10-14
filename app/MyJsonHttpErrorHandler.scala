import com.google.inject.{Inject, Singleton}
import play.api.{Environment, Logging, OptionalSourceMapper}
import play.api.http.JsonHttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Result, Results}
import views.ErrorBody
import controllers._

import scala.concurrent.Future

@Singleton
class MyJsonHttpErrorHandler @Inject()(env: Environment, sourceMapper: OptionalSourceMapper) extends
    JsonHttpErrorHandler(env, sourceMapper) with Logging {
  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = statusCode match {
    case 404 => Future.successful(Results.NotFound(ErrorBody("Requested resource was not found")))
    case _ => super.onClientError(request, statusCode, message)
  }
}
