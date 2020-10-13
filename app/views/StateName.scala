package views

import play.api.libs.json.Json

case class StateName(state: String)

object StateName {
  implicit val stateNameReads = Json.reads[StateName]
}
