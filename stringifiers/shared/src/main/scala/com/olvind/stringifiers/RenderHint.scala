package com.olvind.stringifiers

sealed trait RenderHint

object RenderHint {
  case object Boolean  extends RenderHint
  case object Date     extends RenderHint
  case object DateTime extends RenderHint
  case object Enum     extends RenderHint
  case object Float    extends RenderHint
  case object Int      extends RenderHint
  case object Password extends RenderHint
  case object Text     extends RenderHint
  case object Time     extends RenderHint
  case object Unit     extends RenderHint
  case object Uri      extends RenderHint
  case object Uuid     extends RenderHint
}
