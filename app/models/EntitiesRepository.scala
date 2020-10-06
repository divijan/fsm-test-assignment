/*
package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ Future, ExecutionContext }

/**
 * A repository for entities.
 *
 * @param dbConfigProvider The Play db config provider. Play will inject this for you.
 */
@Singleton
class EntitiesRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

    private class EntitiesTable(tag: Tag) extends Table[Entity](tag, "entities") {

    /** The name column. We assume all entities have unique names, otherwise there is nothing to distinguish them by */
    def name = column[String]("name", O.PrimaryKey)

    /**
     * This is the tables default "projection".
     */
    def * = name <> (Entity.apply _, Entity.unapply)
  }

  private val entities = TableQuery[EntitiesTable]

  /**
   * Create an entity and set it into init state
   *
   * This is an asynchronous operation
   */
  def create(name: String): Future[Entity] = db.run {
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
   * List all the entities in the database.
   */
  def list(): Future[Seq[Entity]] = db.run {
    entities.result
  }
}
*/
