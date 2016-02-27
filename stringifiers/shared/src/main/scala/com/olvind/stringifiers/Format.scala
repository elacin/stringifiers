package com.olvind.stringifiers

sealed trait Format

object Format {
  case object Boolean  extends Format
  case object Date     extends Format
  case object DateTime extends Format
  case object Enum     extends Format
  case object Float    extends Format
  case object Int      extends Format
  case object Password extends Format
  case object Text     extends Format
  case object Time     extends Format
  case object Unit     extends Format
  case object Uri      extends Format
  case object Uuid     extends Format
}
