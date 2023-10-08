package controllers

import javax.inject._
import models._
import play.api.Logging
import play.api.mvc._
import play.api.cache.AsyncCacheApi
import views.ErrorBody
import views.StateTransitionTableRW._
import models.StateTransitionTable
import models.repositories.AppRepository

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


  def replace = Action.async(parse.json) { implicit request =>
    val returnValue = request.body

    try {
      val stt = request.body.as[StateTransitionTable]
      for { // need to make sure long replaceStt operation is completed before responding
        _ <- appRepo.replaceStt(stt)
        _ <- cache.set("STT", stt)
      } yield Created(returnValue)
    } catch {
      case MultipleInitStatesException(m) => Future.successful(BadRequest(ErrorBody(m)))
    }
  }

}
