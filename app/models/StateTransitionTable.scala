package models

case class StateTransitionTable(initialState: String, table: Map[String, Set[String]]) {
  def isTransitionValid(from: String, to: String) = table.get(from).exists(_ contains to)
}

object StateTransitionTable {
  def from(init: String, seq: Seq[(String, String)]) = StateTransitionTable(init,
    seq.groupBy(_._1)
      .view.map { case (k, v) => (k, v.map(_._2).toSet) }
      .toMap)
}