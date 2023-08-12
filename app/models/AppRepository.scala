package models

import com.google.inject.ImplementedBy

import scala.concurrent.Future

@ImplementedBy(classOf[DBTables])
trait AppRepository {
  def getStt(): Future[StateTransitionTable]
  def getInitState(): Future[State]
  def replaceStt(newStt: StateTransitionTable): Future[Unit]
  def isTransitionValid(from: State, to: State): Future[Boolean]


  def getEntities(): Future[Seq[Entity]]
  def getEntity(name: String): Future[Option[Entity]]

  /**
   * creates a new entity and puts it into initial state. Also invokes TransitionLog.recordTransition
   */
  def createEntity(name: String): Future[Entity]
  def deleteEntity(name: String): Future[Unit]
  def resetEntity(name: String): Future[Unit] // Also invokes TransitionLog.recordTransition
  def clearEntities(): Future[Unit]

  def clearAll(): Future[Unit]
}
