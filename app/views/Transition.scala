package views

import java.time.Instant

import play.api.libs.json.{JsObject, Json, OWrites, Writes}

case class Transition(entity: String, from: Option[String], to: String, timestamp: Instant)

object Transition {
  implicit def transitionWrites = Json.writes[Transition]

  implicit def transitionSeqWrites = new OWrites[Seq[Transition]] {
    override def writes(o: Seq[Transition]): JsObject = Json.obj("transitions" ->
      Json.toJson(o)(Writes.iterableWrites2))
  }
}
