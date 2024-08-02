package com.fproj.tinderbot

import zio._
import zio.concurrent.ReentrantLock

trait PageLock {
  def get: ReentrantLock
}

object PageLock {

  def lock[R, E, A](effect: => ZIO[R, E, A]) = {
    val x = for {
      pageLock <- ZIO.service[PageLock]
      _ <- pageLock.get.lock
      res <- effect
      _ <- pageLock.get.unlock
    } yield res

    x.catchAll { t =>
      ZIO.serviceWithZIO[PageLock] (_.get.unlock).flatMap { _ => ZIO.fail(t) }
    }
  }

  val make =
    ZLayer {
      for {
        l <- ReentrantLock.make()
      } yield new PageLock {
        override def get: ReentrantLock = l
      }
    }
}