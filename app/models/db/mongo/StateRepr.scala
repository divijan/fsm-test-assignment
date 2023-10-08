package models.db.mongo

import models.State
import play.api.libs.json._

/*private[mongo]*/ case class StateRepr(name: State, init: Option[Boolean], transitions: Set[State], entities: Array[String])

object StateRepr {
  implicit val stateReprFormat: Format[StateRepr] = Json.format[StateRepr]
}
