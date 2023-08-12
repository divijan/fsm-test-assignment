package views

import play.api.libs.json.Writes
import play.api.libs.json._
import play.api.libs.functional.syntax._


// There should be one initial state
// we won't do terminal ones, because for this assignment there is no difference between a terminal state and one
// that has no transitions
case class State(name: String, isInit: Boolean, transitions: Set[String])

object State {
  implicit val stateWrites: Writes[State] =
  ((JsPath \ "name").write[String] and
    (JsPath \ "isInit").writeNullable[Boolean].contramap((b: Boolean) => if (b) Some(true) else None) and
    (JsPath \ "transitions").write[Set[String]]
  )(unlift(State.unapply))

  implicit val stateReads =
    ((JsPath \ "name").read[String] and
      (JsPath \ "isInit").readNullable[Boolean].map(_.getOrElse(false)) and
      (JsPath \ "transitions").read[Set[String]]
      )(State.apply _)
}

