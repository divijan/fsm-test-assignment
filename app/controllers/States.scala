package controllers

import javax.inject._
import models._
import play.api.libs.json.Json
import play.api.mvc._
import views.{ErrorBody, State, StateTransitionTable}

import scala.concurrent.{ExecutionContext, Future}

class States @Inject()(tables: DBTables,
                       cc: ControllerComponents
                      )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {
  import StateTransitionTable._

  def index = Action.async { implicit request =>
    tables.getSTT.map { case (initState, transitions) =>
      val table = transitions.groupBy(_._1).view.map { case (k, v) =>
        State(k, k == initState, v.map(_._2).toSet) }.toSet
      // Include states with no transitions (aka terminal)?
      // val terminalStates = stateTransitionsList.collect{case (from, to) if !table.isDefinedAt(to) => to}.distinct
      Ok(Json.toJson(table))
    } recover { case e: NoSuchElementException =>
      val message = "State Transition Table is not defined in the system"
      NotFound(Json.toJson(ErrorBody(message)))
    }
  }


  def replace = Action.async(parse.json) { implicit request =>
    val returnValue = request.body

    try {
      val stt = request.body.as[StateTransitionTable]
      val flatTransitions = stt.table.toList.flatMap { case (name, states) => states.map(name -> _) }
      tables.replaceSTT(stt.initialState, flatTransitions) map (_ => Created(returnValue))
    } catch {
      case NotOneInitStateException(m) => Future.successful(BadRequest(Json.toJson(ErrorBody(m))))
    }
  }

}
