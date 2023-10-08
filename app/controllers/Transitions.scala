package controllers

import models._
import models.repositories.TransitionLog
import play.api.Logging
import play.api.cache.AsyncCacheApi
import play.api.libs.json.JsResultException
import play.api.mvc._
import views.EntityRW._
import views.TransitionRW._
import views.{ErrorBody, StateName}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class Transitions @Inject()(transitionLog: TransitionLog,
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

}

