package controllers

import javax.inject._
import models._
import play.api.Logging
import play.api.cache.AsyncCacheApi
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc._
import views.{ErrorBody, StateName, TransitionRW, EntityRW}
import EntityRW._
import TransitionRW._
import Json._

import scala.concurrent.{ExecutionContext, Future}

class Transitions @Inject()(appRepo: AppRepository,
                            transitionLog: TransitionLog,
                            cache : AsyncCacheApi,
                            cc    : ControllerComponents
                           )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with Logging {

  /**
   * The index action shows the whole transition log
   * @return
   */
  def index = Action.async {
    transitionLog.getTransitions() map (Ok(_))
  }


  def show(name: String) = Action.async {
    transitionLog.getTransitionsFor(name).map { seq =>
      if (seq.isEmpty) {
        NotFound(ErrorBody("This entity does not exist"))
      } else {
        Ok(seq)
      }
    }
  }

  // todo: how about separate appRepo actions from transactionLog?
  def move(entityName: String) = Action.async(parse.json) { implicit request =>
    def isTransitionValidCached(currentState: String, newState: String): Future[Boolean] = {
      val stt = cache.getOrElseUpdate("STT")(appRepo.getStt())
      stt.map(_.isTransitionValid(currentState, newState))
    }

    lazy val entityFromDb = appRepo.getEntity(entityName).map(_.get)

    (for {
      newState <- Future(request.body.as[StateName].state)
      entity <- entityFromDb
      isValid <- isTransitionValidCached(entity.state, newState)
      if isValid
      _  <- transitionLog.recordTransition(entityName, newState)
    } yield Ok(entity.copy(state = newState))).recover {
      case e: NoSuchElementException if e.getMessage == "Future.filter predicate is not satisfied" =>
        BadRequest(ErrorBody("Requested transition is invalid"))
      case _: NoSuchElementException => NotFound(ErrorBody("This entity does not exist"))
      case _: JsResultException => BadRequest(ErrorBody("Could not parse state name from body"))
      case e =>
        logger.error(e.toString)
        InternalServerError(e.toString)
    }
  }
}

