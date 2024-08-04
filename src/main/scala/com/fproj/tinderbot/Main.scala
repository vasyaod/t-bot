package com.fproj.tinderbot

import com.cronutils.model.Cron
import com.microsoft.playwright.{Browser, BrowserContext, BrowserType, Playwright}
import cron4zio._
import zio.Console._
import zio._

import scala.jdk.CollectionConverters._

object MyApp extends ZIOAppDefault {

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

//  val randomBio =
//    for {
//      i <- Random.nextIntBetween(0, bios.size)
//      _ <- Commands.editProfile(bios(i), "ML and Data Engineer")
//    } yield ()

  val randomProfile =
    for {
      profiles <- Profiles.profiles
      i <- Random.nextIntBetween(0, profiles.size)
      profile = profiles(i)
      _ <- Commands.editProfile(profile.bio, profile.jobTitle, profile.company)
      _ <- Commands.editPic(profile.pics)
    } yield ()

  val readCommand = for {
    command <- readLine
    _ <- command match {
//      case s"bio ${index}" => Commands.editProfile(bios(index.toInt), "ML and Data Engineer")
//      case "boost" => Commands.applyBoost
//      case "p1" => Commands.editPic(List("pic3.jpg", "pic5.jpg", "pic7.jpg", "pic6.jpg"))
      case "random" => randomProfile
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
      _ <- randomProfile.schedule(Schedule.spaced(6.hours)).forkDaemon
      _ <- repeatEffectForCron(Commands.applyBoost, timeForBoost).unit.forkDaemon
      _ <- printLine("Type command here:")
      _ <- readCommand.logError.ignore.forever
    } yield ()
}