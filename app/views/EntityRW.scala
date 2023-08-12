package views

import models.Entity
import play.api.libs.json._

object EntityRW {
  implicit val entityWrites = Json.writes[Entity]

  val standaloneEntityWrites = new OWrites[Entity] {
    override def writes(o: Entity): JsObject = Json.obj("entity" ->
      Json.toJson(o))
  }

  implicit val entitiesWrites = new OWrites[Seq[Entity]] {
    override def writes(o: Seq[Entity]): JsObject = Json.obj("entities" ->
      Json.toJson(o)(Writes.iterableWrites2))
  }

  implicit val entityObjectReads: Reads[Entity] =
    (JsPath \ "entity").read[Entity](Json.reads[Entity])

  implicit val entityNameReads = Json.reads[EntityName]
}
