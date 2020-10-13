package views

import java.time.Instant

import play.api.libs.json.{JsObject, JsPath, Json, OWrites, Writes}

case class Transition(entity: String, from: Option[String], to: String, timestamp: Instant)

object Transition {
  implicit val transitionWrites = Json.writes[Transition]

  implicit val transitionSeqWrites = new OWrites[Seq[Transition]] {
    override def writes(o: Seq[Transition]): JsObject = Json.obj("transitions" ->
      Json.toJson(o)(Writes.iterableWrites2))
  }

  implicit val transitionReads = Json.reads[Transition]
  implicit val transitionSeqReads = (JsPath \ "transitions").read[Seq[Transition]]
}
