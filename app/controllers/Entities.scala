package controllers

import java.sql.SQLIntegrityConstraintViolationException

import javax.inject._
import models._
import play.api.Logging
import play.api.libs.json.JsResultException
import play.api.mvc._
import views.{EntityName, ErrorBody}
import views.EntityRW._

import scala.concurrent.{ExecutionContext, Future}

class Entities @Inject()(appRepo: AppRepository,
                         transitionLog: TransitionLog,
                         cc: ControllerComponents
                        )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with Logging {

  def index = Action.async {
    appRepo.getEntities() map (Ok(_))
  }


  def show(name: String) = Action.async {
    implicit val entityWrites = standaloneEntityWrites

    appRepo.getEntity(name) map (_.fold(NotFound(ErrorBody("Requested entity does not exist")))
                                      (Ok.apply))
  }


  def create = Action.async(parse.json) { implicit request =>
    implicit val entityWrites = standaloneEntityWrites

    (for {
      entityName <- Future(request.body.as[EntityName])
      _ <- appRepo.createEntity(entityName.name)
    } yield Created(Entity(entityName.name, "initState"))).recover {
        case _: JsResultException =>
          BadRequest(ErrorBody("Could not parse entity name from body"))
        case _: SQLIntegrityConstraintViolationException =>
          Conflict(ErrorBody("This entity already exists"))
        case _: NoSuchElementException =>
          BadRequest(ErrorBody("Cannot create an entity with no STT in the system"))
        case e =>
          logger.error(e.toString)
          InternalServerError(e.toString)
    }
  }


  def delete(name: String) = Action.async {
    appRepo.deleteEntity(name) map { _ =>
      NoContent
    }
  }

  def reset(name: String) = Action.async {
    appRepo.resetEntity(name)
      .map(_ => Ok)
      .recover {
        case e: NoSuchElementException if e.getMessage == "Action.withFilter failed" =>
          BadRequest(ErrorBody("Will not reset an entity that is already in init state"))
      }
  }
}
