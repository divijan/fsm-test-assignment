package models

case class StateTransitionTable(initialState: State, table: Map[State, Set[State]]) {
  def isTransitionValid(from: State, to: State) = table.get(from).exists(_ contains to)
}

//todo: define inputs in addition to states
object StateTransitionTable {
  def from(init: String, seq: Seq[(String, String)]) = StateTransitionTable(init,
    seq.groupBy(_._1)
      .view.map { case (k, v) => (k, v.map(_._2).toSet) }
      .toMap)
}