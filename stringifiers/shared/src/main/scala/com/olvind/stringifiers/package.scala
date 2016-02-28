package com.olvind

import scala.reflect.ClassTag
import scala.util.Try

package object stringifiers {
  private[stringifiers] def toTryF[T, R](f: T => R): T => Try[R] =
    t => Try(f(t))

  private[stringifiers] val nonEmpty: String ⇒ String =
    _.ensuring(_.nonEmpty)
}
