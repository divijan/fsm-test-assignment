package controllers

import javax.inject._

import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class TransitionController @Inject()(repo: EntitiesRepository,
                                 cc: MessagesControllerComponents
                                )(implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {

  /**
   * The index action.
   */
  def index = Action { implicit request =>
    Ok(views.html.index(personForm))
  }

  def addTransition = Action.async { implicit request =>
    // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle succes.
    personForm.bindFromRequest.fold(
      // The error function. We return the index page with the error form, which will render the errors.
      // We also wrap the result in a successful future, since this action is synchronous, but we're required to return
      // a future because the person creation function returns a future.
      errorForm => {
        Future.successful(Ok(views.html.index(errorForm)))
      },
      // There were no errors in the from, so create the person.
      person => {
        repo.create(person.name, person.age).map { _ =>
          // If successful, we simply redirect to the index page.
          Redirect(routes.TransitionController.index).flashing("success" -> "user.created")
        }
      }
    )
  }


  def getTransitions = Action.async { implicit request =>
    repo.list().map { people =>
      Ok(Json.toJson(people))
    }
  }
}

