package models

import java.lang.IllegalStateException
import java.time.Instant

import javax.inject.{Inject, Singleton}
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.{ExecutionContext, Future}

/**
 * A class containing the DB-related stuff
 *
 * @param dbConfigProvider The Play db config provider. Play will inject this for you.
 */
@Singleton
class DBTables @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  /**
   * Stores the transitions table
   */
  private class StateTransitionsTable(tag: Tag) extends Table[(String, String)](tag, "state_transitions") {
    def from = column[String]("from")
    def to = column[String]("to")
    def pk = primaryKey("pk_state_transitions", (from, to))
    def * = (from, to)
  }

  //separate table because it makes no sense to repeat isInit in every row
  private class InitStatesTable(tag: Tag) extends Table[String](tag, "init_states") {
    def name = column[String]("name", O.PrimaryKey)
    def * = name
  }
  //todo: remove this table because it duplicates the contents of Transitions table
  private class EntitiesTable(tag: Tag) extends Table[(String, String)](tag, "entities") {
    def name = column[String]("name", O.PrimaryKey)
    def stateName = column[String]("state")
    def * = (name, stateName)
  }

  /**
   * Stores history of transitions throughout the system
   * @param tag
   */
  private class TransitionsTable(tag: Tag) extends Table[(String, Option[String], String, Instant)](tag, "transitions") {
    def entityName = column[String]("entity_name")
    def from = column[Option[String]]("from")
    def to = column[String]("to")
    //we never use default functionality and h2 can only RETURNING an AutoInc column, so no default here
    def timestamp = column[Instant]("timestamp")
    def * = (entityName, from, to, timestamp)
  }

  private object Queries {
    val states = TableQuery[StateTransitionsTable]

    val initStates = TableQuery[InitStatesTable]

    val entities = TableQuery[EntitiesTable]

    val transitions = TableQuery[TransitionsTable]

    def queryInitState =
      initStates.result.head

    def queryEntities = entities.result

    def queryEntity(name: String) = entities.filter(_.name === name)

    /**
     * List all the valid transitions.
     */
    def queryValidTransitions = states.result

    def queryIsTransitionValid(from: String, to: String) =
      states.filter(s => (s.from === from) && (s.to === to)).exists.result

    def queryISTransitions = for {
      initState <- queryInitState
      transitions <- queryValidTransitions
    } yield (initState, transitions)
  }


  import Queries._

  def getSTT(): Future[(String, Seq[(String, String)])] = db.run(queryISTransitions)
  def getInitState(): Future[String] = db.run(queryInitState)
  def replaceSTT(initState: String, transitions: Seq[(String, String)]) = db.run {
    DBIO.seq(
      initStates.delete,
      (initStates += initState),
      states.delete,
      (states ++= transitions)
    )
  }
  def isTransitionValid(from: String, to: String) = db.run {
    queryIsTransitionValid(from, to)
  }


  def getEntities(): Future[Seq[(String, String)]] = db.run(queryEntities)
  def getEntity(name: String): Future[Option[(String, String)]] = db.run(queryEntity(name).take(1).result.headOption)
  def createEntity(name: String): Future[(String, String)] = db.run(
    for {
      stateName <- initStates.take(1).result.head
      _ <- (entities += (name, stateName))
      _  <- (transitions += (name, None, stateName, Instant.now()))
    } yield (name, stateName)
  ).recover { case e: NoSuchElementException => throw new NoSuchElementException("No STT. Init state is undefined") }
  def deleteEntity(name: String) = db.run(
    queryEntity(name).delete andThen
    transitions.filter(_.entityName === name).delete
  )
  def resetEntity(name: String) = for {
    initState <- getInitState()
    _ <- db.run {
      queryEntity(name).filter(_.stateName =!= initState).map(_.stateName).update(initState).filter(_ == 1) andThen
        (transitions += (name, None, initState, Instant.now()))
    }.recover { case e: NoSuchElementException => throw new IllegalStateException("Will not reset an entity which is already in init state") }
  } yield (name, initState)
  def clearEntities() = db.run(entities.delete)

  def recordTransition(entityName: String, currentState: String, newState: String) =
    db.run {
      (for {
        _   <- queryEntity(entityName).map(_.stateName) update newState
        now <- DBIO.successful(Instant.now())
        _   <- (transitions += (entityName, Some(currentState), newState, now))
       } yield (entityName, Some(currentState), newState, now)).transactionally
    }

  def getTransitionsFor(entityName: String): Future[Seq[(String, Option[String], String, Instant)]] = db.run {
    transitions.filter(_.entityName === entityName).sortBy(_.timestamp).result
  }
  def getTransitions(): Future[Seq[(String, Option[String], String, Instant)]] = db.run {
    transitions.sortBy(t => (t.entityName, t.timestamp)).result
  }

  def clearAll() = db.run {
    DBIO.seq(
      transitions.delete,
      entities.delete,
      states.delete,
      initStates.delete
    )
  }
}
