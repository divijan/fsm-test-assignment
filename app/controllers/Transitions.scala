package controllers

import javax.inject._
import models._
import play.api.Logging
import play.api.cache.AsyncCacheApi
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc._
import views.{ErrorBody, StateName, Transition}

import scala.concurrent.{ExecutionContext, Future}

class Transitions @Inject()(tables: DBTables,
                            cache : AsyncCacheApi,
                            cc    : ControllerComponents
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
    def isTransitionValidCached(currentState: String, newState: String): Future[Boolean] = {
      val stt = cache.getOrElseUpdate("STT")(tables.getSTT().map((StateTransitionTable.from _).tupled))
      stt.map(_.isTransitionValid(currentState, newState))
    }

    lazy val currentStateFromDB = tables.getEntity(entity).map(_.get._2)

    (for {
      newState <- Future(request.body.as[StateName].state)
      currentState <- currentStateFromDB
      _ <- isTransitionValidCached(currentState, newState).filter(identity).recover { case e: NoSuchElementException =>
        throw new IllegalStateException("Requested transition is invalid")
      }
      created  <- tables.recordTransition(entity, currentState, newState)
    } yield Ok((Transition.apply _).tupled(created))).recover {
      case e: IllegalStateException  => BadRequest(ErrorBody(e.getMessage))
      case e: NoSuchElementException => NotFound(ErrorBody("This entity does not exist"))
    }.recover(genericExceptionMapper("state name", logger))
  }
}

