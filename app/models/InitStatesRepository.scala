package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * Repository for keeping an initial state. There should only be one by design
 * @param dbConfigProvider
 * @param ec
 */
@Singleton
class InitStatesRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private class InitStatesTable(tag: Tag) extends Table[String](tag, "init_states") {

    def name = column[String]("name", O.PrimaryKey)

    def * = name
  }
  
  private val initStates = TableQuery[InitStatesTable]

  def replaceWith(name: String): Future[Int] = db.run {
    initStates.delete andThen (initStates += name)
  }

  def get(): Future[String] = db.run {
    initStates.result.head
  }
}
