package com.fproj.tinderbot

import com.microsoft.playwright.Page
import com.microsoft.playwright.Page.WaitForDownloadOptions
import zio._

import java.nio.file.Paths

object Commands {

  def navigate(url: String) =
    for {
      page <- ZIO.service[Page]
      _ <- ZIO.attempt {
        page.navigate(url)
      }
      _ <- ZIO.attempt {
        page.waitForLoadState()
      }
      _ <- ZIO.sleep(zio.Duration.fromSeconds(3))
    } yield ()

  val applyBoost = PageLock.lock {
    for {
      _ <- ZIO.logInfo("Applying boost")
      page <- ZIO.service[Page]
      _ <- navigate("https://tinder.com/")
      _ <- ZIO.sleep(zio.Duration.fromSeconds(40))
      _ <- ZIO.attempt {
        page.locator(""".Hidden:text("Boost")""").click()
      }
      _ <- ZIO.sleep(zio.Duration.fromSeconds(3))
      _ <- ZIO.attempt {
        if (page.isVisible(""":text("Use 1 Boost for 30 mins")""")) {
          page.locator(""":text("Use 1 Boost for 30 mins")""").click()
        }
      }
    } yield ()
  }

  val auth = PageLock.lock {
    for {
      page <- ZIO.service[Page]
      _ <- navigate("https://tinder.com/")
      _ <- if (page.isVisible(":text(\"Log In\")")) {
        for {
          _ <- ZIO.attempt {
            page.locator(":text(\"Log In\")").click()
          }
          _ <- ZIO.sleep(zio.Duration.fromSeconds(3))
          _ <- ZIO.attempt {
            val bb = page.frameLocator("[title=\"Sign in with Google Dialog\"]").locator("button :text(\"Continue as Vasiliy\")").boundingBox()
            page.mouse.click(bb.x + bb.width / 2, bb.y + bb.height - 5);
          }
          _ <- ZIO.logInfo("Authorization successful")
        } yield ()
      } else {
        ZIO.logInfo("Authorization is not necessary")
      }
    } yield ()
  }

  def editProfile(bio: String) = PageLock.lock {
    for {
      _ <- ZIO.logInfo("Set New BIO: " + bio)
      page <- ZIO.service[Page]
      _ <- navigate("https://tinder.com/app/profile/edit")
      _ <- ZIO.attempt {
        page.locator("textarea").first().fill(bio)
      }
      _ <- ZIO.attempt {
        page.locator(":text(\"Save\")").nth(1).click()
      }
      _ <- navigate("https://tinder.com/")
    } yield ()
  }

  /** Upload a new pic in a free slot */
  def uploadPic(file: String) = PageLock.lock {
    for {
      page <- ZIO.service[Page]
      _ <- ZIO.attemptBlocking {
        val selector = "button .StretchedBox"
        page.locator(selector).nth(0).click()
      }
      _ <- ZIO.attemptBlocking {
        //page.waitForFileChooser(new Page.WaitForFileChooserOptions().setPredicate())
        val fileChooser = page.waitForFileChooser(new Runnable {
          override def run(): Unit = page.locator("[role=button] :text(\"Gallery\")").click()
        })
        fileChooser.setFiles(Paths.get(file))

      }
      _ <- ZIO.sleep(zio.Duration.fromSeconds(1))
      _ <- ZIO.attemptBlocking {
        page.locator("button :text(\"Choose\")").click()
      }
      _ <- ZIO.sleep(zio.Duration.fromSeconds(1))
    } yield ()
  }

  /** Upload a new pic in a free slot */
  def removePic(picIndex: Int) = PageLock.lock {
    for {
      page <- ZIO.service[Page]
      _ <- ZIO.attemptBlocking {
        val selector = "button .StretchedBox"
        page.locator(selector).nth(0).click()
      }
      _ <- ZIO.attemptBlocking {
        //page.waitForFileChooser(new Page.WaitForFileChooserOptions().setPredicate())
        val fileChooser = page.waitForFileChooser(new Runnable {
          override def run(): Unit = page.locator("[role=button] :text(\"Gallery\")").click()
        })
        fileChooser.setFiles(Paths.get(file))

      }
      _ <- ZIO.sleep(zio.Duration.fromSeconds(1))
      _ <- ZIO.attemptBlocking {
        page.locator("button :text(\"Choose\")").click()
      }
      _ <- ZIO.sleep(zio.Duration.fromSeconds(1))
    } yield ()
  }

  def editPic() = PageLock.lock {
    for {
//      _ <- ZIO.logInfo("Set New BIO: " + bio)
      page <- ZIO.service[Page]
      _ <- navigate("https://tinder.com/app/profile/edit")
      _ <- ZIO.sleep(zio.Duration.fromSeconds(5))
      _ <- uploadPic("/home/vasyaod/work/tinder-bot/pics/pic1.jpg")
      _ <- uploadPic("/home/vasyaod/work/tinder-bot/pics/pic2.jpg")
      //      _ <- ZIO.attemptBlocking {
//        val selector = ".Hidden :text(\"5/9\")"
//        if (page.isVisible(selector)) {
//          page.locator(selector).click()
//        }
//      }
//      _ <- ZIO.sleep(zio.Duration.fromSeconds(1))
//      _ <- ZIO.attemptBlocking {
//        page.locator("div[role=dialog]").locator("button").locator(":text(\"Delete\")").click()
//        //page.mouse.click(bb.x + bb.width / 2, bb.y + bb.height / 2);
//      }
//      _ <- ZIO.attempt {
//        page.locator(":text(\"Save\")").nth(1).click()
//      }
//      _ <- navigate("https://tinder.com/")
    } yield ()
  }
}