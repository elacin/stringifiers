package com.olvind.stringifiers

trait ThrowableFormatter {
  def apply(th: Throwable): Option[String]
}

object ThrowableFormatter {
  object Nothing extends ThrowableFormatter {
    override def apply(th: Throwable): Option[String] =
      None
  }

  object GetMessage extends ThrowableFormatter {
    override def apply(th: Throwable) =
      Some(th.getMessage)
  }

  object ClassAndMessage extends ThrowableFormatter {
    override def apply(th: Throwable) =
      Some(s"${th.getClass.getSimpleName}: ${th.getMessage}")
  }

//  object All extends ThrowableFormatter {
//    override def apply(th: Throwable) = {
//      val sw = new StringWriter()
//      val pw = new PrintWriter(sw)
//      th.printStackTrace(pw)
//      Some(sw.toString)
//    }
//  }
}
