package views

import java.util.NoSuchElementException

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class StateTransitionTable(initialState: String, table: Map[String, Set[String]])

object StateTransitionTable {
  import State._

  case class NotOneInitStateException(msg: String) extends Exception

  implicit val stateTransitionTableReads: Reads[StateTransitionTable] =
    (JsPath \ "states").read[Set[State]].map { set =>
      val initStates = set.filter(_.isInit)
      if (initStates.size != 1) {
        throw new NotOneInitStateException("There should be exactly one init state!")
      } else {
        //TODO: warn about unreachable states
        //val nonInitStates = set.filterNot(_.isInit)
        val map = set.map(s => s.name -> s.transitions).toMap
        StateTransitionTable(initStates.head.name, map)
      }
    }

  implicit val stateTransitionTableWrites: Writes[Set[State]] = new Writes[Set[State]] {
    override def writes(o: Set[State]): JsValue = Json.obj("states" ->
      Json.toJson(o)(Writes.iterableWrites2))
  }
}
