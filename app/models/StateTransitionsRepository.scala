package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * A repository for valid transitions between states.
 *
 * @param dbConfigProvider The Play db config provider. Play will inject this for you.
 */
@Singleton
class StateTransitionsRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  //separate table because it makes no sense to repeat isInit in every row
  private class StateTransitionsTable(tag: Tag) extends Table[(String, String)](tag, "state_transitions") {

    def from = column[String]("from")

    def to = column[String]("to")

    def pk = primaryKey("pk_state_transitions", (from, to))
    /**
     * This is the tables default "projection".
     */
    def * = (from, to)
  }
  
  private val states = TableQuery[StateTransitionsTable]

  def replace(transitions: Seq[(String, String)]) = db.run {
    states.delete andThen
      (states ++= transitions)
  }

  /**
   * List all the valid transitions.
   */
  def list(): Future[Seq[(String, String)]] = db.run {
    states.result
  }

  def isTransitionValid(from: String, to: String): Future[Boolean] = db.run {
    states.filter(s => (s.from === from) && (s.to === to)).exists.result
  }

  def getTransitions(): Future[Map[String, Set[String]]] = list().map(
    _.groupBy(_._1)
     .view.mapValues(
       _.map(_._2).toSet
      ).toMap)
}
