package models

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

  private object Queries {
    val states = TableQuery[StateTransitionsTable]

    val initStates = TableQuery[InitStatesTable]

    def getInitState =
      initStates.result.head

    /**
     * List all the valid transitions.
     */
    def listTransitions = states.result

    def isTransitionValid(from: String, to: String) =
      states.filter(s => (s.from === from) && (s.to === to)).exists.result

    def getTransitions = listTransitions.map(
      _.groupBy(_._1)
        .view.mapValues(
        _.map(_._2).toSet
      ).toMap)

    def STTQuery = for {
      initState <- getInitState
      transitions <- listTransitions
    } yield (initState, transitions)
  }

  import Queries._

  def replaceSTT(initState: String, transitions: Seq[(String, String)]) = db.run {
    DBIO.seq(
      initStates.delete,
      (initStates += initState),
      states.delete,
      (states ++= transitions)
    )
  }

  def getSTT: Future[(String, Seq[(String, String)])] = db.run(STTQuery)

  private val schema = states.schema ++ initStates.schema
  db.run(DBIO.seq(
    schema.createIfNotExists
  ))
}
