package com.fproj.tinderbot

import io.circe.generic.auto._
import io.circe.parser._

import zio._
import zio.test.{test, _}
import zio.test.junit.JUnitRunnableSpec

object ProfilesTest extends JUnitRunnableSpec {
  def spec = suite("Profiles")(
    test("can be readable from a file") {
      for {
        profiles <- Profiles.profiles
      } yield {
        assertTrue(profiles.size > 0)
      }
    }
  )
}