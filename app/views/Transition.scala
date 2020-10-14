package views

import java.time.Instant

import play.api.libs.json.Json

case class Transition(from: Option[String], to: String)

object Transition {

  def fromTuple(tup: (String, Option[String], String, Instant)): Transition = {
    Transition(tup._2, tup._3)
  }

}

case class TransitionLog(entityName: String, transitions: Seq[Transition])

object TransitionLog {
  implicit val transitionWrites = Json.writes[Transition]
  implicit val transitionLogWrites = Json.writes[TransitionLog]
  //implicit val generalTransitionLogWrites = new OWrites[Seq[TransitionLog]]

  implicit val transitionReads = Json.reads[Transition]
  implicit val transitionLogReads = Json.reads[TransitionLog]
}
