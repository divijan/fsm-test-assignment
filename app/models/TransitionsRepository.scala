/*
package models

import java.time.Instant

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * A repository for transitions.
 *
 * @param dbConfigProvider The Play db config provider. Play will inject this for you.
 */
@Singleton
class TransitionsRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private class TransitionsTable(tag: Tag) extends Table[(String, String, String, Instant)](tag, "states") {

    def entity = column[String]("entity")

    def from = column[String]("from") //may be null

    def to = column[String]("to")

    def timestamp = column[Instant]("timestamp")(O.Default(Instant.now))
    /**
     * This is the tables default "projection".
     */
    def * = (entity, from, to, timestamp)
  }
  
  private val states = TableQuery[TransitionsTable]

  /**
   * Add a transition entry
   */
  /*def create(entity: String, from: String, to: String, timestamp: Instant): Future[Transition] = db.run {
    // We create a projection of just the name and age columns, since we're not inserting a value for the id column
    (people.map(p => (p.name, p.age))
      // Now define it to return the id, because we want to know what id was generated for the person
      returning people.map(_.id)
      // And we define a transformation for the returned value, which combines our original parameters with the
      // returned id
      into ((nameAge, id) => Person(id, nameAge._1, nameAge._2))
    // And finally, insert the person into the database
    ) += (name, age)
  }

  /**
   * List all the transitions for all the entities that took place in the app.
   */
  def list(): Future[Seq[Transition]] = db.run {
    states.result
  }*/
}
*/
