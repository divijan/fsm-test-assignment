import java.time.Instant

package object models {
  type State = String

  case class Entity(name: String, state: State) extends Serializable //required for Slick

  case class Transition(entity: String, from: Option[String], to: String, timestamp: Instant) extends Serializable
}
