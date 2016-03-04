package com.olvind.stringifiers

import org.scalatest.{FunSuite, Matchers}

class TypenameTest extends FunSuite with Matchers {

  case class DummyInner(value: String)

  test("in package"){
    Typename[DummyInPackage].value should be ("DummyInPackage")
  }
  test("inner"){
    Typename[DummyInner].value should be ("DummyInner")
  }
  test("method inner 1"){
    case class A()
    def a: Unit = {
      Typename[A].value should be ("A")
    }
  }
  test("method inner 2"){
    def a: Unit = {
      case class A()
      Typename[A].value should be ("A")
    }
  }
  test("method inner inner"){
    def a: Unit = {
      def b = {
        case class A()
        Typename[A].value should be ("DummyInnerInner")
      }
    }
  }
}

class DummyInPackage