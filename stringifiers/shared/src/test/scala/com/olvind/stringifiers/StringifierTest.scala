package com.olvind.stringifiers

import org.scalatest.FunSuite

class StringifierTest extends FunSuite {

  def assertLeft[E](res: Either[Failed, E]) =
    assertResult(true, res)(res.isLeft)

  test("work for string wrapper"){
    case class WS(value: String)
    implicit val S = Stringifier(WS)(_.value)
    val WS1 = WS("1")

    val ok = Stringifier.decode[WS](Stringifier.encode(WS1))
    val fail = Stringifier.decode[WS]("")

    assertResult(Right(WS1), WS1)(ok)
    assertLeft(fail)
  }

  test("work for int wrapper"){
    case class WI(value: Int)
    implicit val S = Stringifier(WI)(_.value)
    val W1 = WI(1)

    val ok = Stringifier.decode[WI](Stringifier.encode(W1))
    val fail = Stringifier.decode[WI]("a")

    assertLeft(fail)
    assertResult(Right(W1), W1)(ok)
  }

  test("work for restricted values"){
    case class WR(value: Char)
    implicit val S = Stringifier(WR)(_.value).restricted(Set('a', 'b', 'c').map(WR))
    val W1   = WR('a')
    val ok   = Stringifier.decode[WR](Stringifier.encode(W1))
    val fail = Stringifier.decode[WR]("1")
    assertResult(Right(W1), W1)(ok)
    assertLeft(fail)
  }

  test("combinators work"){
    type T = Either[Int, Int]
    implicit val S = Stringifier.SInt.optional.xmap[T](oi => oi.filter(_ < 42).toRight[Int](42))(_.fold(Some.apply, _ => None))

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
    implicit val S = Stringifier(EvenInt)(_.value)

    val res1 = Stringifier.decode[EvenInt]("1")
    val res2 = Stringifier.decode[EvenInt]("2")

    assertLeft(res1)
    assertResult(Right(EvenInt(2)))(res2)
  }
}
