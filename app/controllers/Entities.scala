package controllers

import javax.inject._
import models._
import play.api.libs.json.Json
import play.api.mvc._
import views.{Entity, EntityName, ErrorBody}
import views.EntityRW._

import scala.concurrent.{ExecutionContext, Future}

class Entities @Inject()( tables: DBTables,
                          cc: ControllerComponents
                        )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  /**
   * The index action.
   */
  def index = Action.async {
    tables.getEntities map { seq =>
      val entities = seq map Entity.tupled
      OkJs(entities)
    }
  }


  def show(name: String) = Action.async {
    tables.getEntity(name) map { opt =>
      val entity = opt map Entity.tupled
      OkJs(entity)
    }
  }


  def create = Action.async(parse.json) { implicit request =>
    (for {
      entityName <- Future(request.body.as[EntityName])
      created <- tables.createEntity(entityName.name)
    } yield created.fold(
        InternalServerError(Json.toJson(ErrorBody("entity creation failed")))
      )(p => OkJs(Entity.tupled(p)))).recover(_ =>
      BadRequest(Json.toJson(ErrorBody("Couldn't parse entity name from body")))
    )
  }


  def delete(name: String) = Action.async {
    tables.deleteEntity(name) map { _ =>
      Ok
    }
  }

  def reset(name: String) = Action.async {
    tables.resetEntity(name) map { e =>
      OkJs(Entity.tupled(e))
    }
  }
}
