package com.olvind.stringifiers

import scala.reflect.ClassTag

final case class Typename(value: String) extends AnyVal

object Typename {
  def apply[E: ClassTag] =
    new Typename(implicitly[ClassTag[E]].runtimeClass.getName)
}