package views

import play.api.libs.json.{JsObject, Json, OWrites, Writes}

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

  implicit val entityNameReads = Json.reads[EntityName]
}
