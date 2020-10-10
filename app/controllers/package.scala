import play.api.mvc.Results.Status
import play.api.mvc.Results.Ok
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.http.Writeable

package object controllers {
  def OkJs[T](content: T)(implicit writesT: Writes[T]) = Ok(Json.toJson(content))

  /*implicit class JsStatus(internal: Status) {
    def withJson[T](content: T)(implicit writesT: Writes[T]) = internal.apply(Json.toJson(content))
  } weird, but this doesn't work in controllers because their Status is <ControllerName>.this.Status */

  implicit def writableAsJson[T](implicit writesT: Writes[T]) = {
    val jsWritable = implicitly[Writeable[JsValue]]
    val transform = (t: T) => jsWritable.transform(Json.toJson(t))
    new Writeable(transform, jsWritable.contentType)
  }
}
