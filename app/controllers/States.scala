package controllers

import javax.inject._
import models._
import play.api.data.validation.Constraints._
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._
import views.{State, StateTransitionTable}

import scala.concurrent.{ExecutionContext, Future}

class States @Inject()(initStatesRepo: InitStatesRepository,
                       stateTransitionsRepo: StateTransitionsRepository,
                       cc: MessagesControllerComponents
                                )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {
  import StateTransitionTable._

  def index = Action.async { implicit request =>
    val initStateResult = initStatesRepo.get()
    val stateTransitionsResult = stateTransitionsRepo.list()
    for {
      initState <- initStateResult
      stateTransitionsList <- stateTransitionsResult
    } yield {
      val table = stateTransitionsList.groupBy(_._1).view.map{ case (k, v) =>
        State(k, k == initState) -> v.map(_._2).toSet }.toSeq
      // Include states with no transitions (aka terminal)?
      // val terminalStates = stateTransitionsList.collect{case (from, to) if !table.isDefinedAt(to) => to}.distinct
      Ok(Json.toJson(table))
    }
  }


  def replace = Action.async(parse.json) { implicit request =>
    val returnValue = request.body

    val stt = request.body.as[StateTransitionTable]
    val initStateReplaced = initStatesRepo.replaceWith(stt.initialState)

    val flatTransitions = stt.table.toList.flatMap{ case (name, states) => states.map(name -> _)}
    val transitionsReplaced = stateTransitionsRepo.replace(flatTransitions)
    for {
      _ <- initStateReplaced
      _ <- transitionsReplaced
    } yield {
      Created(returnValue)
    }

  }

}
