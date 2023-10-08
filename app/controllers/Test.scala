package controllers

import play.api.Logging
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.play.json.compat._, json2bson.{ toDocumentReader, toDocumentWriter }

import models.{State, StateTransitionTable}
import views.StateTransitionTableRW._
import models.db.mongo.StateRepr
import play.api.libs.json.Json
import reactivemongo.api.Cursor
import reactivemongo.api.bson.collection.BSONCollection

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class Test @Inject()(cc: ControllerComponents,
                     val reactiveMongoApi: ReactiveMongoApi
                    )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with MongoController with ReactiveMongoComponents with Logging {

  def index = Action.async {
    implicit def ec: ExecutionContext = cc.executionContext

    /*
     * Resolves a JSONCollection
     * (a Collection implementation that is designed to work with JsObject,
     * Reads and Writes).
     *
     * The deprecated `.db` function should be replaced as there by `.database`.
     *
     * Note that the `collection` is not a `val`, but a `def`. We do _not_ store
     * the collection reference to avoid potential problems in development with
     * Play hot-reloading.
     */
    val states: Future[BSONCollection] =
      reactiveMongoApi.database.map(_.collection[BSONCollection]("states"))
    states.flatMap {
      _.find(Json.obj()).cursor[StateRepr]()
        .collect[List](err = Cursor.FailOnError[List[StateRepr]]())
        .map { list =>
          var initState = ""
          val map = list.foldLeft(Map.empty[State, Set[State]]) { case (map, stateRepr) =>
            if (stateRepr.init.exists(identity)) {
              initState = stateRepr.name
            }
            map + (stateRepr.name -> stateRepr.transitions.toSet)
          }
          StateTransitionTable(initState, map)
        }
    }.map(Ok(_))
  }

}
