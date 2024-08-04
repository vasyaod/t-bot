package com.fproj.tinderbot

import com.microsoft.playwright.BrowserContext.StorageStateOptions
import com.microsoft.playwright.{Browser, BrowserContext, BrowserType, Page, Playwright}
import zio._
import zio.Console._
import cron4zio._
import com.cronutils.model.Cron

import scala.jdk.CollectionConverters._
import java.nio.file.{Path, Paths}

object MyApp extends ZIOAppDefault {

  val bios = List(
    """Gym, CrossFit, Cycling are my spare time friends. A few times per an year i’m cycling in random places across the world with a tent for long distances, when a trip is more unpredictable, weird and “dangerous” then better.
      |
      |Sometimes I think that I can "speak" with machines much better than humans.
      |
      |Personal connection is preferable, let's say "no" to bots and scammers 😎...""".stripMargin,

    """Have passion to my work, sometimes, maybe, too much.
      |Can speak and whisper with machines and machines love me.
      |Gym, CrossFit and Cycling are my spare time friends.""".stripMargin,

    """Have passion to my work, sometimes, maybe, too much.
      |The best place for time together is in a Gym and I'm seriously.""".stripMargin,
  )

  val playwrightLayer = ZLayer.scoped {
    ZIO.acquireRelease {
      ZIO.attempt {
        Playwright.create()
      }
    } { playwright =>
      ZIO
        .attempt {
          playwright.close()
        }
        .orDie
    }
  }

  def makeBrowser = ZLayer.scoped {
    ZIO.acquireRelease {
      ZIO.serviceWithZIO[Playwright] { playwright =>
        ZIO.attempt {
          playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false))
        }
      }
    } { browser =>
      ZIO
        .attempt {
          browser.close()
        }
        .orDie
    }
  }

  def makeBrowserContext = ZLayer.scoped {
    ZIO.acquireRelease {
      ZIO.serviceWithZIO[Browser] { browser =>
        ZIO.attempt {
          val storageState = scala.io.Source.fromFile("./browser-context").mkString
          val bc = browser
            .newContext(
              new Browser.NewContextOptions()
                .setViewportSize(1920, 1200)
                .setGeolocation(37.34162470271984, -121.98211248104863)
                .setPermissions(List("geolocation", "notifications").asJava)
                .setStorageState(storageState)
            )
          bc
        }
      }
    } { browserContext =>
      ZIO
        .attempt {
          browserContext.close()
        }
        .orDie
    }
  }

  def makePage = ZLayer.scoped {
    ZIO.acquireRelease {
      ZIO.serviceWithZIO[BrowserContext] { browserContext =>
        ZIO.attempt {
          browserContext.newPage()
        }
      }
    } { page =>
      ZIO
        .attempt {
          page.close()
        }
        .orDie
    }
  }

  def run = myAppLogic
    .provide(playwrightLayer, makeBrowser, makeBrowserContext, makePage, PageLock.make)
//    .onInterrupt(printLine("Stopped").orDie)

  val randomBio =
    for {
      i <- Random.nextIntBetween(0, bios.size)
      _ <- Commands.editProfile(bios(i), "ML and Data Engineer")
    } yield ()

  val readCommand = for {
    command <- readLine
    _ <- command match {
      case s"bio ${index}" => Commands.editProfile(bios(index.toInt), "ML and Data Engineer")
      case "boost" => Commands.applyBoost
      case "p1" => Commands.editPic(List("pic3.jpg", "pic5.jpg", "pic7.jpg", "pic6.jpg"))
      case "count" => Commands.countPic()
      case "move" => Commands.movePic(6, 1)
      case "q" => ZIO.attempt { java.lang.System.exit(0) }
      case _ => ZIO.logInfo("Unknown command")
    }
    _ <- ZIO.logInfo("Command has been executed")
  } yield ()

  val timeForBoost: Cron = unsafeParse("0 0 20 * * ?")

  val myAppLogic =
    for {
//      page <- ZIO.service[Page]
      _ <- Commands.auth.forkDaemon
      _ <- Commands.auth.schedule(Schedule.spaced(1.hours)).forkDaemon
      _ <- randomBio.schedule(Schedule.spaced(247.minutes)).forkDaemon
      _ <- repeatEffectForCron(Commands.applyBoost, timeForBoost).unit.forkDaemon
      _ <- printLine("Type command here:")
      _ <- readCommand.logError.ignore.forever
    } yield ()
}