package views

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.json._
import play.api.libs.json.Reads._
import models.StateTransitionTable


object StateTransitionTableRW {
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

  implicit val stateTransitionTableWrites: Writes[StateTransitionTable] = new OWrites[StateTransitionTable] {
    override def writes(stt: StateTransitionTable): JsObject = {
      val seq = stt.table.map { case (state, transitions) => State(state, state == stt.initialState, transitions) }.toSeq
      Json.obj("states" -> Json.toJson(seq))
    }
  }
}
