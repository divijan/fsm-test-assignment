import play.api.Logger
import play.api.libs.json.{JsResultException, JsValue, Json, Writes}
import play.api.http.Writeable
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError}
import views.ErrorBody

package object controllers {
  implicit def writableAsJson[T](implicit writesT: Writes[T]) = {
    val jsWritable = implicitly[Writeable[JsValue]]
    val transform = (t: T) => jsWritable.transform(Json.toJson(t))
    new Writeable(transform, jsWritable.contentType)
  }

  def genericExceptionMapper(parsing: String, logger: Logger): PartialFunction[Throwable, Result] = {
    case e: JsResultException => BadRequest(ErrorBody(s"Could not parse $parsing from body"))
    case catchAll =>
      logger.error(catchAll.toString)
      InternalServerError(catchAll.toString)
  }
}
