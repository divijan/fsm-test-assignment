import play.api.libs.json.{JsValue, Json, Writes => JsonWrites}
import play.api.http.Writeable

package object controllers {
  /*implicit class JsStatus(internal: Status) {
    def withJson[T](content: T)(implicit writesT: Writes[T]) = internal.apply(Json.toJson(content))
  } weird, but this doesn't work in controllers because their Status is <ControllerName>.this.Status */

  implicit def writableAsJson[T](implicit writesT: JsonWrites[T]): Writeable[T] = {
    val jsWritable: Writeable[JsValue] = Writeable.writeableOf_JsValue
    val transform = (t: T) => jsWritable.transform(Json.toJson(t))
    new Writeable(transform, jsWritable.contentType)
  }
}
