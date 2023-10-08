package models.db.mongo

import com.fasterxml.jackson.annotation.JsonView
import models.{Entity, State, StateTransitionTable, Transition}
import models.repositories.{AppRepository, TransitionLog}
import play.api.libs.json.{JsObject, Json}
import reactivemongo.api.Cursor
import reactivemongo.api.ReadPreference
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.bson.{BSONDocumentHandler, Macros}
import reactivemongo.api.bson.collection.BSONCollection

import scala.concurrent.ExecutionContext

// BSON-JSON conversions/collection
import reactivemongo.play.json.compat._, json2bson._

import javax.inject.Inject
import scala.concurrent.Future

/*
  Data model:
  states: StateTransitionTable+entities
  { "name": String,
    "init": Option[Boolean],
    "transitions": Array[String],
    "entities": Array[String]
  } We store entities in the corresponding state because they are small
  transitions:
  { plain old Transition object
  }
 */

abstract class RepositoryImpl @Inject() (reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext)
  extends /*AppRepository with TransitionLog with*/ ReactiveMongoComponents {

  private def states: Future[BSONCollection] =
    reactiveMongoApi.database.map(_.collection[BSONCollection]("states"))

  def getStt(): Future[StateTransitionTable] = states.flatMap {
    _.find(Json.obj()).cursor[StateRepr]()
      .collect[List](err = Cursor.FailOnError[List[StateRepr]]())
      .map { list =>
        var initState = ""
        val map = list.foldLeft(Map.empty[State, Set[State]]){ case (map, stateRepr) =>
          if (stateRepr.init.exists(identity)) {
            initState = stateRepr.name
          }
          map + (stateRepr.name -> stateRepr.transitions.toSet)
        }
        StateTransitionTable(initState, map)
      }
  }

  def getInitState(): Future[String] = states.flatMap {
    _.find(
      selector   = Json.obj("init" -> true)
    ).requireOne[StateRepr]
      .map(_.name)
  }

  def replaceStt(newStt: StateTransitionTable): Future[Unit] //purges the old transitionLog

  def isTransitionValid(from: State, to: State): Future[Boolean]


  def getEntities(): Future[Seq[Entity]]

  def getEntity(name: String): Future[Option[Entity]]

  /**
   * creates a new entity and puts it into initial state. Also invokes TransitionLog.recordTransition
   */
  def createEntity(name: String): Future[Entity]

  def deleteEntity(name: String): Future[Unit] // Also deletes its transitions

  def resetEntity(name: String): Future[Unit] // Also invokes TransitionLog.recordTransition

  def clearEntities(): Future[Unit]

  def clearAll(): Future[Unit]

  def recordTransition(entityName: String, newState: State): Future[Unit]

  def getTransitionsFor(entityName: String): Future[Seq[Transition]]

  def getTransitions(): Future[Seq[Transition]]
}
