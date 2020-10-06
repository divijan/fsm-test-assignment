package views

import play.api.libs.json.Writes
import play.api.libs.json._
import play.api.libs.functional.syntax._


// There should be one initial state
// we won't do terminal ones, because for this assignment there is no difference between a terminal state and one
// that has no transitions
case class State(name: String, isInit: Boolean)

object State {
  implicit val stateWrites: Writes[State] =
  ((JsPath \ "name").write[String] and
    (JsPath \ "isInit").writeNullable[Boolean].contramap((b: Boolean) => if (b) Some(true) else None)
  )(unlift(State.unapply))

}

