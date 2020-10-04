package views

import java.util.NoSuchElementException

import models.State
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class StateTransitionTable(initialState: String, table: Map[String, Set[String]])

object StateTransitionTable {
  implicit val stateTransitionReads: Reads[(State, Set[String])] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "isInit").readNullable[Boolean].map(_.getOrElse(false)) and
      (JsPath \ "transitions").read[Set[String]]).apply{(name, isInit, transitions) =>
    State(name, isInit) -> transitions}

  implicit val stateTransitionTableReads: Reads[StateTransitionTable] =
    (JsPath \ "states").read[Map[State, Set[String]]].map { m =>
      val initStateCount = m.keys.count(_.isInit)
      if (initStateCount != 1) {
        throw new IllegalArgumentException("There should be exactly one init state!")
      } else {
        /* this ensures that all the state names in transitions list are present in the map. Redundant
        m.view.mapValues(_.map { name =>
          try {
            m.find(_._1.name == name).get._1
          } catch {
            case e: NoSuchElementException => throw new IllegalArgumentException(s"Undefined state in transitions: $name")
          }
        }).toMap*/
        val initState = m.find(_._1.isInit).get._1.name
        StateTransitionTable(initState, m.map {case (k,v) => k.name -> v} )
      }
    }


}
