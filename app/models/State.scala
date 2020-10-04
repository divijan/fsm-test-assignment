package models

import java.util.NoSuchElementException

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

// There should be one initial state
// we won't do terminal ones, because for this assignment there is no difference between a terminal state and one
// that has no transitions
case class State(name: String, isInit: Boolean = false)

object State {


}