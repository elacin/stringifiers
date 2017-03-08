package com.olvind.stringifiers

import argonaut.{Argonaut, CodecJson, DecodeJson, DecodeResult, EncodeJson}
import org.scalatest.{FunSuite, Matchers}

/**
  * This is an example of how `Stringifier` can be used to derive type class instances,
  *  in this case support for argonaut codecs.
  *
  * It is simpler than it looks too - it's just a matter of function composition
  *  and handling errors by threading through the information provided by `DecodeFail`
  */
object ArgonautSupport {
  import scala.language.implicitConversions

  implicit def toEncoder[T](implicit S: Stringifier[T]): EncodeJson[T] =
    EncodeJson[T](S.encode andThen Argonaut.jString)

  implicit def toDecoder[T](implicit S: Stringifier[T]): DecodeJson[T] =
    DecodeJson[T](
      c => c.as[String](DecodeJson.StringDecodeJson) map S.decode flatMap {
        case Right(t) =>
          DecodeResult ok t
        case Left(ValueNotValid(v, t, oe)) =>
          DecodeResult fail (s"«$v» is not a valid ${t.value}${oe.fold("")(": " + _)}", c.history)
        case Left(ValueNotInSet(v, t, ss)) =>
          DecodeResult fail (s"«$v» is not a valid ${t.value}. Not among ${ss.mkString("[«", "», «", "»]")}", c.history)
      }
    )
}

class ArgonautSupportTest extends FunSuite with Matchers {
  /* define a few classes to play with */
  case class WrapChar(value: Char)
  case class WrapOddInt(value: Int){
    require(value % 2 == 1, "Only accepts odd values")
  }
  case class MyClass(c: WrapChar, i: WrapOddInt, oi: Option[WrapOddInt])

  /* define Stringifiers for the two wrapper types WC and WI */
  implicit val WCStringifier: Stringifier[WrapChar]   = Stringifier.instance(WrapChar)(_.value)
  implicit val WIStringifier: Stringifier[WrapOddInt] = Stringifier.instance(WrapOddInt)(_.value)

  import Argonaut._
  import ArgonautSupport._

  /* use the derived codecs for `WC` and `WI` to create a codec for `A` */
  implicit val ADecodeJson: CodecJson[MyClass] = casecodec3(MyClass.apply, MyClass.unapply)("char", "int", "intopt")

  test("work"){
    MyClass(WrapChar('a'), WrapOddInt(3), Some(WrapOddInt(5))).asJson.nospaces should be ("""{"char":"a","int":"3","intopt":"5"}""")
  }

  test("Have reasonable error messages (normal)"){
    """"2"""".decodeEither[WrapOddInt] should be (
      Left("«2» is not a valid WrapOddInt: IllegalArgumentException: requirement failed: Only accepts odd values: CursorHistory(List())")
    )
  }

  test("Have reasonable error messages (enum)"){
    case class Meh(value: Int)
    implicit val S: Stringifier[Meh] = Stringifier[Int].withEnumValues(Set(1, 3)).xmap(Meh)(_.value)

    """"5"""".decodeEither[Meh] should be (
      Left("«5» is not a valid Meh. Not among [«1», «3»]: CursorHistory(List())")
    )
  }
  test("secrets"){
    case class Secrets(value: Long)
    implicit val S: Stringifier[Secrets] = Stringifier.instance(Secrets)(_.value)

    implicitly[EncodeJson[Secrets]]
    implicitly[DecodeJson[Secrets]]
  }
}

