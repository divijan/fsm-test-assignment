package models

case class StateTransitionTable(initialState: String, table: Map[String, Set[String]]) {
  def isTransitionValid(from: String, to: String) = table.get(from).exists(_ contains to)
}

object StateTransitionTable {
  def from(init: String, seq: Seq[(String, String)]) = StateTransitionTable(init, seq.groupMap(_._1)(_._2)
                                                                                     .view.mapValues(_.toSet).toMap)
}