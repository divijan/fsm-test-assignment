package controllers

import java.sql.SQLIntegrityConstraintViolationException

import javax.inject._
import models._
import play.api.Logging
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc._
import views.{Entity, EntityName, ErrorBody}
import views.EntityRW._

import scala.concurrent.{ExecutionContext, Future}

class Entities @Inject()( tables: DBTables,
                          cc: ControllerComponents
                        )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with Logging {

  /**
   * The index action.
   */
  def index = Action.async {
    tables.getEntities map { seq =>
      val entities = seq map Entity.tupled
      Ok(entities)
    }
  }


  def show(name: String) = Action.async {
    tables.getEntity(name) map (_.fold(NotFound(ErrorBody("Requested entity does not exist")))
                                      (e => Ok(Entity.tupled(e))))
  }


  def create = Action.async(parse.json) { implicit request =>
    (for {
      entityName <- Future(request.body.as[EntityName])
      created <- tables.createEntity(entityName.name)
    } yield Created(Entity.tupled(created))).recover {
        case e: JsResultException =>
          BadRequest(ErrorBody("Could not parse entity name from body"))
        case e: SQLIntegrityConstraintViolationException =>
          Conflict(ErrorBody("This entity already exists"))
        case e =>
          logger.error(e.toString)
          InternalServerError(e.toString)
    }
  }


  def delete(name: String) = Action.async {
    tables.deleteEntity(name) map { _ =>
      NoContent
    }
  }

  def reset(name: String) = Action.async {
    tables.resetEntity(name).map{ e =>
      Ok(Entity.tupled(e))
    }.recover {
      case e: NoSuchElementException if e.getMessage == "Action.withFilter failed" =>
        BadRequest(ErrorBody("Will not reset an entity that is already in init state"))
    }
  }
}
