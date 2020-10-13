package controllers

import javax.inject._
import models._
import play.api.Logging
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n._
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc._
import views.{ErrorBody, StateName, Transition}

import scala.concurrent.{ExecutionContext, Future}

class Transitions @Inject()(tables: DBTables,
                            cc: ControllerComponents
                           )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with Logging {

  /**
   * The index action shows the whole transition log
   * @return
   */
  def index = Action.async {
    tables.getTransitions().map(seq => Ok(seq.map((Transition.apply _).tupled)))
  }


  def show(name: String) = Action.async {
    tables.getTransitionsFor(name).map { seq =>
      if (seq.isEmpty) {
        NotFound(ErrorBody("This entity does not exist"))
      } else {
        Ok(seq.map((Transition.apply _).tupled))
      }
    }
  }


  def move(entity: String) = Action.async(parse.json) { implicit request =>
    (for {
      newState <- Future(request.body.as[StateName])
      created <- tables.recordTransition(entity, newState.state)
    } yield Ok((Transition.apply _).tupled(created))).recover {
      case e: NoSuchElementException => NotFound(ErrorBody(e.getMessage))
      case e: IllegalStateException => BadRequest(ErrorBody(e.getMessage))
      case e: JsResultException => BadRequest(ErrorBody("Could not parse state name from body"))
      case e =>
        logger.error(e.toString)
        InternalServerError(e.toString)
    }
  }
}

