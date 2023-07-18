package com.scalahealth.fhir

import zio.json._
class HelloSpec extends munit.FunSuite {
  test("say hello") {
   // assertEquals(Hello.greeting, "hello")
  }

  test("say hello2") {
    val a = """
      |{"a": "b"}
      |""".stripMargin
      .toJsonAST

      println(a)
  }
}
