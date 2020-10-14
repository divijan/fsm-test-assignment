package controllers

import javax.inject._
import models._
import play.api.Logging
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc._
import views.{ErrorBody, StateName, Transition, TransitionLog}

import scala.concurrent.{ExecutionContext, Future}

class Transitions @Inject()(tables: DBTables,
                            cc: ControllerComponents
                           )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with Logging {

  /**
   * The index action shows the whole transition log
   * @return
   */
  def index = Action.async {
    tables.getTransitions().map { seq =>
      if (seq.isEmpty){
        NotFound(ErrorBody("Transition log is empty"))
      } else {
        val transitionLog = seq.groupBy(_._1)
          .map(kv => TransitionLog(kv._1,
            kv._2.map(Transition.fromTuple))
          )
        Ok(transitionLog)
      }
    }
  }


  def show(name: String) = Action.async {
    tables.getTransitionsFor(name).map { seq =>
      if (seq.isEmpty) {
        NotFound(ErrorBody("This entity does not exist"))
      } else {
        val transitionLog = TransitionLog(seq.head._1, seq.map(Transition.fromTuple))
        Ok(transitionLog)
      }
    }
  }


  def move(entity: String) = Action.async(parse.json) { implicit request =>
    val goodFlow = for {
      newState <- Future(request.body.as[StateName])
      created <- tables.recordTransition(entity, newState.state)
    } yield Ok(TransitionLog(created._1, Seq(Transition.fromTuple(created))))

    goodFlow.recover {
      case e: NoSuchElementException => NotFound(ErrorBody(e.getMessage))
      case e: IllegalStateException => BadRequest(ErrorBody(e.getMessage))
      case e: JsResultException => BadRequest(ErrorBody("Could not parse state name from body"))
      case e =>
        logger.error(e.toString)
        InternalServerError(e.toString)
    }
  }
}

