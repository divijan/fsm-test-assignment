package controllers

import javax.inject._
import models._
import play.api.Logging
import play.api.mvc._
import play.api.cache.AsyncCacheApi
import views.ErrorBody
import views.StateTransitionTableRW._
import models.StateTransitionTable

import scala.concurrent.{ExecutionContext, Future}

class States @Inject()(appRepo: AppRepository,
                       cache: AsyncCacheApi,
                       cc: ControllerComponents
                      )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with Logging {

  def index = Action.async {
    cache.getOrElseUpdate("STT") {
      appRepo.getStt()
    }.map(Ok(_))
     .recover { case _: NoSuchElementException =>
        NotFound(ErrorBody("State Transition Table is not defined in the system"))
      }
  }


  //todo: what happens with existing entities when we do this? Re-initialize them and drop the transaction log?
  def replace = Action.async(parse.json) { implicit request =>
    val returnValue = request.body

    try {
      val stt = request.body.as[StateTransitionTable]
      appRepo.replaceStt(stt)
      cache.set("STT", stt) map (_ => Created(returnValue))
    } catch {
      case NotOneInitStateException(m) => Future.successful(BadRequest(ErrorBody(m)))
    }
  }

}
