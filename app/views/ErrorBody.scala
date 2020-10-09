package views

import play.api.libs.json.Json

case class ErrorBody(error: String)

object ErrorBody {
  implicit val errBodyWrites = Json.writes[ErrorBody]
}
