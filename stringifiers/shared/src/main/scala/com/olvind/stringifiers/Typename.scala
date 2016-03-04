package com.olvind.stringifiers

import scala.reflect.ClassTag

final case class Typename(value: String) extends AnyVal

object Typename {
  /* Try to get a de-mangled typename. This certainly has it's limits, but i think its good enough */
  def apply[E: ClassTag] = {
    val base = implicitly[ClassTag[E]].runtimeClass.getName
    new Typename(base.split("\\.").last.split("\\$").filterNot(_.forall(_.isDigit)).last)
  }
}