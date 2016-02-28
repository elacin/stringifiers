package com.olvind.stringifiers

import org.scalatest.{Matchers, FunSuite}

class StringifierTest extends FunSuite with Matchers {

  def assertLeft[E](res: Either[DecodeFail, E]) =
    assertResult(true, res)(res.isLeft)

  case class WS(value: String)

  test("work for string wrapper"){

    implicit val S = Stringifier[String].xmap(WS)(_.value)
    println(S.typename)
    val WS1 = WS("1")

    val ok = Stringifier.decode[WS](Stringifier.encode(WS1))
    val fail = Stringifier.decode[WS]("")

    assertResult(Right(WS1), WS1)(ok)
    fail should be (Left(ValueNotValid("", Typename("WS"), Some("AssertionError: assertion failed"))))
    assertLeft(fail)
  }

  test("work for int wrapper"){
    case class WI(value: Int)
    implicit val S = Stringifier.xmap(WI)(_.value)
    val W1 = WI(1)

    val ok = Stringifier.decode[WI](Stringifier.encode(W1))
    val fail = Stringifier.decode[WI]("a")

    assertLeft(fail)
    assertResult(Right(W1), W1)(ok)
  }

  test("work for restricted values"){
    case class WR(value: Char)
    implicit val S = Stringifier.xmap(WR)(_.value).withEnumValues(Set('a', 'b', 'c').map(WR))
    val W1   = WR('a')
    val ok   = Stringifier.decode[WR](Stringifier.encode(W1))
    val fail = Stringifier.decode[WR]("1")
    assertResult(Right(W1), W1)(ok)
    assertLeft(fail)
  }

  test("combinators work"){
    type T = Either[Int, Int]
    implicit val S = Stringifier[Int].optional.xmap[T](oi => oi.filter(_ < 42).toRight[Int](42))(_.fold(Some.apply, _ => None))

    val res0  = Stringifier.decode[T]("41")
    val res1  = Stringifier.decode[T]("42")
    val res2  = Stringifier.decode[T]("")
    val res3  = Stringifier.decode[T]("43")
    val fail1 = Stringifier.decode[T]("a")

    assertResult(Right(Right(41)))(res0)
    assertResult(Right(Left(42)))(res1)
    assertResult(Right(Left(42)))(res2)
    assertResult(Right(Left(42)))(res3)
    assertLeft(fail1)
  }

  test("evenInt"){
    case class EvenInt(value: Int){
      require(value % 2 == 0)
    }
    implicit val S = Stringifier.xmap(EvenInt)(_.value)

    val res1 = Stringifier.decode[EvenInt]("1")
    val res2 = Stringifier.decode[EvenInt]("2")

    assertLeft(res1)
    assertResult(Right(EvenInt(2)))(res2)
  }
}
