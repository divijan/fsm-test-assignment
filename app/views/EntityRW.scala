package views

import models.Entity
import play.api.libs.json.{JsObject, Json, OWrites, Writes}

object EntityRW {
  implicit def entityWrites = Json.writes[Entity]

  implicit def entitiesWrites = new OWrites[Seq[Entity]] {
    override def writes(o: Seq[Entity]): JsObject = Json.obj("entities" ->
      Json.toJson(o)(Writes.iterableWrites2))
  }
}
