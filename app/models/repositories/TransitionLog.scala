package models.repositories

import com.google.inject.ImplementedBy
import models.db.slick.DBTables
import models.{State, Transition}

import scala.concurrent.Future

@ImplementedBy(classOf[DBTables])
trait TransitionLog {
  def recordTransition(entityName: String, newState: State): Future[Unit]
  def getTransitionsFor(entityName: String): Future[Seq[Transition]]
  def getTransitions(): Future[Seq[Transition]]
}
