package models

import javax.inject.{Inject, Singleton}
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

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

  private class StateTransitionsTable(tag: Tag) extends Table[(String, String)](tag, "state_transitions") {
    def from = column[String]("from")
    def to = column[String]("to")
    def pk = primaryKey("pk_state_transitions", (from, to))
    /**
     * This is the tables default "projection".
     */
    def * = (from, to)
  }

  //separate table because it makes no sense to repeat isInit in every row
  private class InitStatesTable(tag: Tag) extends Table[String](tag, "init_states") {
    def name = column[String]("name", O.PrimaryKey)
    def * = name
  }

  private class EntitiesTable(tag: Tag) extends Table[(String, String)](tag, "entities") {
    def name = column[String]("name", O.PrimaryKey)
    def stateName = column[String]("state")
    def * = (name, stateName)
  }

  private object Queries {
    val states = TableQuery[StateTransitionsTable]

    val initStates = TableQuery[InitStatesTable]

    val entities = TableQuery[EntitiesTable]

    def queryInitState =
      initStates.result.head

    def queryEntities = entities.result

    def queryEntity(name: String) = entities.filter(_.name === name)

    def queryCreateEntity(name: String) =
      entities forceInsertQuery (initStates.take(1).map { is => (name, is.name) })

    /**
     * List all the valid transitions.
     */
    def queryTransitions = states.result

    def queryIsTransitionValid(from: String, to: String) =
      states.filter(s => (s.from === from) && (s.to === to)).exists.result

    def querySTT = queryTransitions.map(
      _.groupBy(_._1)
        .view.mapValues(
        _.map(_._2).toSet
      ).toMap)

    def queryISTransitions = for {
      initState <- queryInitState
      transitions <- queryTransitions
    } yield (initState, transitions)
  }

  import Queries._

  def getSTT: Future[(String, Seq[(String, String)])] = db.run(queryISTransitions)
  def getInitState: Future[String] = db.run(queryInitState)
  def replaceSTT(initState: String, transitions: Seq[(String, String)]) = db.run {
    DBIO.seq(
      initStates.delete,
      (initStates += initState),
      states.delete,
      (states ++= transitions)
    )
  }



  def getEntities: Future[Seq[(String, String)]] = db.run(queryEntities)
  def getEntity(name: String): Future[Option[(String, String)]] = db.run(queryEntity(name).take(1).result.headOption)
  def createEntity(name: String): Future[Option[(String, String)]] = db.run(
    queryCreateEntity(name) andThen
    queryEntity(name).take(1).result.headOption
  )
  def deleteEntity(name: String) = db.run(queryEntity(name).delete)
  def resetEntity(name: String) = for {
    initState <- getInitState
    updated <- db.run(queryEntity(name).map(_.stateName).update(initState))
  } yield (name, initState)
  def clearEntities = db.run(entities.delete)
}
