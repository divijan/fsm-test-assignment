import play.api.libs.json.{JsValue, Json, Writes}
import play.api.http.Writeable

package object controllers {
  implicit def writableAsJson[T](implicit writesT: Writes[T]) = {
    val jsWritable = implicitly[Writeable[JsValue]]
    val transform = (t: T) => jsWritable.transform(Json.toJson(t))
    new Writeable(transform, jsWritable.contentType)
  }
}
