import play.api.mvc.Results.Ok
import play.api.libs.json.{Json, Writes}

package object controllers {
  def OkJs[T](content: T)(implicit writesT: Writes[T]) = Ok(Json.toJson(content))
}
