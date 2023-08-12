package models

import java.time.Instant
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * A class containing the DB-related stuff
 *
 * @param dbConfigProvider The Play db config provider. Play will inject this for you.
 */
@Singleton
class DBTables @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
extends AppRepository with TransitionLog {
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

  private class EntitiesTable(tag: Tag) extends Table[Entity](tag, "entities") {
    def name = column[String]("name", O.PrimaryKey)
    def stateName = column[String]("state")
    def * = (name, stateName).mapTo[Entity]
  }

  private class TransitionsTable(tag: Tag) extends Table[Transition](tag, "transitions") {
    def entityName = column[String]("entity_name")
    def from = column[Option[String]]("from")
    def to = column[String]("to")
    //we never use default functionality and h2 can only RETURNING an AutoInc column, so no default here
    def timestamp = column[Instant]("timestamp")
    def * = (entityName, from, to, timestamp).mapTo[Transition]
  }

  private object Queries {
    val states = TableQuery[StateTransitionsTable]

    val initStates = TableQuery[InitStatesTable]

    val entities = TableQuery[EntitiesTable]

    val transitions = TableQuery[TransitionsTable]

    def queryInitState = initStates.result.head

    def queryEntities = entities.result

    def queryEntity(name: String) = entities.filter(_.name === name)

    /**
     * List all the valid transitions.
     */
    def queryValidTransitions = states.result

    def queryIsTransitionValid(from: State, to: State) =
      states.filter(s => (s.from === from) && (s.to === to)).exists.result

    def queryISTransitions = for {
      initState <- queryInitState
      transitions <- queryValidTransitions
    } yield StateTransitionTable(initState, transitions.groupMap(_._1)(_._2).view.mapValues(_.toSet).toMap)
  }


  import Queries._

  override def getStt(): Future[StateTransitionTable] = db.run(queryISTransitions)

  override def getInitState(): Future[State] = db.run(queryInitState)

  override def replaceStt(stt: StateTransitionTable) = db.run {
    DBIO.seq(
      initStates.delete,
      initStates += stt.initialState,
      states.delete,
      states ++= stt.table.flatMap { case (state, set) => set.map(state -> _).toSeq }
    )
  }

  override def isTransitionValid(from: String, to: String) = db.run {
    queryIsTransitionValid(from, to)
  }


  override def getEntities(): Future[Seq[Entity]] = db.run(queryEntities)

  override def getEntity(name: String): Future[Option[Entity]] = db.run(
    queryEntity(name)
      .take(1)
      .result
      .headOption)

  override def createEntity(name: String): Future[Entity] = db.run(
    for {
      stateName <- initStates.take(1).result.head
      newEntity = Entity(name, stateName)
      _ <- entities += newEntity
      _  <- transitions += Transition(name, None, stateName, Instant.now())
    } yield newEntity
  ).recover {
    case _: NoSuchElementException =>
      throw new NoSuchElementException("State Transition Table is not defined. Init state is undefined")
  }

  override def deleteEntity(name: String): Future[Unit] = db.run(
    queryEntity(name).delete andThen
    transitions.filter(_.entityName === name).delete
  ).map(_ => ())

  override def resetEntity(name: String) = for {
    initState <- getInitState()
    _ <- db.run {
      queryEntity(name).filter(_.stateName =!= initState).map(_.stateName).update(initState).filter(_ == 1) andThen
        (transitions += Transition(name, None, initState, Instant.now()))
    }
  } yield (name, initState)

  override def clearEntities() = db.run(entities.delete).map(_ => ())

  override def recordTransition(entityName: String, newState: String) = db.run {
    (for {
      currentState <- queryEntity(entityName).map(_.stateName).result.headOption
      _   <- queryEntity(entityName).map(_.stateName) update newState
      now <- DBIO.successful(Instant.now())
      _   <- transitions += Transition(entityName, currentState, newState, now)
     } yield ()).transactionally
  }

  override def getTransitionsFor(entityName: String): Future[Seq[Transition]] = db.run {
    transitions
      .filter(_.entityName === entityName)
      .sortBy(_.timestamp)
      .result
  }

  override def getTransitions(): Future[Seq[Transition]] = db.run {
    transitions.sortBy(t => (t.entityName, t.timestamp)).result
  }

  override def clearAll(): Future[Unit] = db.run { //todo: parallelize queries if possible
    DBIO.seq(
      transitions.delete,
      entities.delete,
      states.delete,
      initStates.delete
    )
  }
}
