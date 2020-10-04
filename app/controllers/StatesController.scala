package controllers

import javax.inject._
import models._
import play.api.data.validation.Constraints._
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._
import views.StateTransitionTable

import scala.concurrent.{ExecutionContext, Future}

class StatesController @Inject()(initStatesRepo: InitStatesRepository,
                                 stateTransitionsRepo: StateTransitionsRepository,
                                 cc: MessagesControllerComponents
                                )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {


  def index = Action.async { implicit request =>
    val initStateResult = initStatesRepo.get()
    val stateTransitionsResult = stateTransitionsRepo.list()
    for {
      initState <- initStateResult
      stateTransitionsList <- stateTransitionsResult
    } yield {
      val table = stateTransitionsList.groupBy(_._1).view.mapValues(_.map(_._2).toSet).toMap
      // Include states with no transitions (aka terminal?)
      // val terminalStates = stateTransitionsList.collect{case (from, to) if !table.isDefinedAt(to) => to}.distinct
      Ok(StateTransitionTable(initState, table))
    }
  }

  def replace = Action.async(parse.json) { implicit request =>
    import StateTransitionTable._

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

  /**
   * A REST endpoint that gets all the people as JSON.
   */
  def show(name: String) = Action.async { implicit request =>
    repo.list().map { people =>
      Ok(Json.toJson(people))
    }
  }
}

/**
 * The create person form.
 *
 * Generally for forms, you should define separate objects to your models, since forms very often need to present data
 * in a different way to your models.  In this case, it doesn't make sense to have an id parameter in the form, since
 * that is generated once it's created.
 */
case class CreatePersonForm(name: String, age: Int)
