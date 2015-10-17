package com.olvind.stringifiers

sealed trait Failed{
  def v:                String
  def typename:         String
}

case class FailedParsing(
  v:                String,
  typename:         String,
  th:               Throwable
) extends Failed

case class FailedNotInSet(
  v:                String,
  typename:         String,
  restrictedKeys:   Set[String]
) extends Failed
