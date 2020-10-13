package views

import java.time.Instant

import play.api.libs.json.Json

case class Transition(entity: String, from: Option[String], to: String, timestamp: Instant)

object Transition {
  implicit def transitionWrites = Json.writes[Transition]
}
