package com.fproj.tinderbot

import zio._

import java.nio.file.Paths
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe._
import io.circe.yaml.parser

object Profiles {

  case class Profile(bio: String, jobTitle: String, company: String, pics: List[String])

  val profiles = ZIO.attempt {
    parser
      .parse(scala.io.Source.fromFile("profiles.yaml").mkString)
      .fold(throw _, identity)
      .as[List[Profile]]
      .fold(throw _, identity)
  }
}