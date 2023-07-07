package com.scalahealth.fhir.indexer

class HelloSpec extends munit.FunSuite {
  test("say hello") {
    assertEquals(Hello.greeting, "hello")
  }
}
