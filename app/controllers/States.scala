package controllers

import javax.inject._
import models._
import play.api.Logging
import play.api.mvc._
import play.api.cache.AsyncCacheApi
import views.ErrorBody
import views.StateTransitionTableRW
import models.StateTransitionTable

import scala.concurrent.{ExecutionContext, Future}

class States @Inject()(tables: DBTables,
                       cache: AsyncCacheApi,
                       cc: ControllerComponents
                      )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with Logging {
  import StateTransitionTableRW._

  def index = Action.async {
    cache.getOrElseUpdate("STT") {
      tables.getSTT().map { (StateTransitionTable.from _).tupled }
    }.map(x => Ok(x))
     .recover { case e: NoSuchElementException =>
        NotFound(ErrorBody("State Transition Table is not defined in the system"))
      }
  }


  def replace = Action.async(parse.json) { implicit request =>
    val returnValue = request.body

    try {
      val stt = request.body.as[StateTransitionTable]

      val flatTransitions = stt.table.toList.flatMap { case (name, states) => states.map(name -> _) }
      tables.replaceSTT(stt.initialState, flatTransitions)

      cache.set("STT", stt) map (_ => Created(returnValue))
    } catch {
      case NotOneInitStateException(m) => Future.successful(BadRequest(ErrorBody(m)))
    }
  }

}
