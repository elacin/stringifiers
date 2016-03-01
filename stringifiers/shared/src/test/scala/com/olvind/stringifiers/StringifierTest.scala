package com.olvind.stringifiers

import org.scalatest.{FunSuite, Matchers}

class StringifierTest extends FunSuite with Matchers {

  case class WS(value: String)
  case class WI(value: Int)
  case class WC(value: Char)
  case class EvenInt(value: Int){
    require(value % 2 == 0)
  }

  test("work for string wrapper"){
    implicit val S: Stringifier[WS] = Stringifier[String].xmap(WS)(_.value)
    val value = WS("1")

    S.decode(Stringifier.encode(value)) should be (Right(value))
    S.decode("") should be (Left(ValueNotValid("", Typename("WS"), Some("AssertionError: assertion failed"))))
  }

  test("work for int wrapper"){
    implicit val S: Stringifier[WI] = Stringifier.instance(WI)(_.value)
    val W1 = WI(1)

    val ok = Stringifier.decode[WI](Stringifier.encode(W1))
    val fail = Stringifier.decode[WI]("a")

    fail should be (Left(ValueNotValid("a", Typename("WI"), Some("NumberFormatException: For input string: \"a\""))))
    assertResult(Right(W1), W1)(ok)
  }

  test("work for int wrapper with enums"){
    implicit val S: Stringifier[WI] = Stringifier.instance(WI)(_.value).withEnumValues(Set(1,2,3) map WI)
    Stringifier.decode[WI]("1") should be (Right(WI(1)))
    Stringifier.decode[WI]("0") should be (Left(ValueNotInSet("0",Typename("WI"),Set("1", "2", "3"))))
  }

  test("work for restricted values"){
    val S: Stringifier[WC] =
      Stringifier
        .instance(WC)(_.value)
        .withEnumValues(Set('a', 'b', 'c') map WC)

    val W1 = WC('a')
    S.decode(S.encode(W1)) should be(Right(W1))
    S.decode("1") should be(Left(ValueNotInSet("1", Typename("WC"), Set("a", "b", "c"))))
  }

  test("combinators work"){
    type T = Either[Int, Int]
    val S: Stringifier[T] =
      Stringifier[Int]
      .optional
      .xmap[T](oi => (oi filter (_ < 42)).toRight(42))(_.fold(Some.apply, _ => None))

    S.decode("41") should be (Right(Right(41)))
    S.decode("42") should be (Right(Left(42)))
    S.decode("")   should be (Right(Left(42)))
    S.decode("43") should be (Right(Left(42)))
    S.decode("a")  should be (Left(ValueNotValid("a",Typename("Either"),Some("NumberFormatException: For input string: \"a\""))))
  }

  test("evenInt"){
    val S: Stringifier[EvenInt] =
      Stringifier.instance(EvenInt)(_.value)

    S.decode("1") should be (Left(ValueNotValid("1",Typename("EvenInt"),Some("IllegalArgumentException: requirement failed"))))
    S.decode("2") should be (Right(EvenInt(2)))
  }
}
