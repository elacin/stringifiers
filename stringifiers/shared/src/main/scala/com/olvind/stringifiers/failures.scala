package com.olvind.stringifiers

sealed trait DecodeFail {
  def value:    String
  def typename: Typename
}

case class ValueNotValid(
  value:    String,
  typename: Typename,
  error:    Option[String]
) extends DecodeFail

case class ValueNotInSet(
  value:    String,
  typename: Typename,
  enumKeys: Set[String]
) extends DecodeFail
