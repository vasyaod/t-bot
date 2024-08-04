package com.fproj.tinderbot

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.BoundingBox
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
            val bb: BoundingBox = page.frameLocator("[title=\"Sign in with Google Dialog\"]").locator("button :text(\"Continue as Vasiliy\")").boundingBox()
            page.mouse.click(bb.x + bb.width / 2, bb.y + bb.height - 5);
          }
          _ <- ZIO.logInfo("Authorization successful")
        } yield ()
      } else {
        ZIO.logInfo("Authorization is not necessary")
      }
      _ <- ZIO.sleep(zio.Duration.fromSeconds(60))
      likeStatus <- ZIO.attempt {
        page.locator("a#likes-you span span").textContent()
      }
      _ <- ZIO.logInfo("Like status: " + likeStatus)
    } yield ()
  }

  def editProfile(bio: String, jobTitle: String, company: String) = PageLock.lock {
    for {
      _ <- ZIO.logInfo("Set New BIO: " + bio)
      page <- ZIO.service[Page]
      _ <- navigate("https://tinder.com/app/profile/edit")
      _ <- ZIO.attempt {
        page.locator("textarea").first().fill(bio)
        page.locator("input#job_title").first().fill(jobTitle)
        page.locator("input#company").first().fill(company)
      }
      _ <- ZIO.sleep(zio.Duration.fromSeconds(1))
      _ <- ZIO.attempt {
        page.locator(":text(\"Save\")").nth(1).click()
      }
      _ <- ZIO.sleep(zio.Duration.fromSeconds(1))
      _ <- navigate("https://tinder.com/")
    } yield ()
  }

  def drugAndDrop(bb0: BoundingBox, bb1: BoundingBox) = {
    for {
      page <- ZIO.service[Page]
      _ <- ZIO.attemptBlocking {
        page.mouse.move(bb0.x + bb0.width / 2, bb0.y + bb0.height / 2)
        page.mouse.down()
//        page.mouse.move(bb1.x + bb1.width / 2, bb1.y + bb1.height / 2, new Mouse.MoveOptions().setSteps(20))
        page.mouse.move(bb1.x + bb1.width / 2, bb1.y + bb1.height / 2)
        page.mouse.up()
      }
    } yield ()
  }

  /** Upload a new pic in a free slot */
  def uploadPic(file: String, placeIndex: Int) = PageLock.lock {
    for {
      _ <- ZIO.logInfo(s"Upload pic from slot $file")
      page <- ZIO.service[Page]
      bb0 <- ZIO.attemptBlocking {
        val selector = "button .StretchedBox"
        val l = page.locator(selector).nth(0)
        val bbox = l.boundingBox()
        l.click()
        bbox
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
      _ <- ZIO.sleep(zio.Duration.fromSeconds(10))
      bb0 <- ZIO.attemptBlocking {
        page.locator(s"span .StretchedBox .StretchedBox").last().boundingBox()
      }
      bb1 <- ZIO.attemptBlocking {
        page.locator(s"span .StretchedBox .StretchedBox").nth(placeIndex).boundingBox()
      }
      _ <- drugAndDrop(bb0, bb1)
      _ <- ZIO.sleep(zio.Duration.fromSeconds(1))
    } yield ()
  }

  /** Upload a new pic in a free slot */
  def movePic(from: Int, to: Int) = PageLock.lock {
    for {
      page <- ZIO.service[Page]
      _ <- navigate("https://tinder.com/app/profile/edit")
      _ <- ZIO.sleep(zio.Duration.fromSeconds(5))
      bb0 <- ZIO.attemptBlocking {
        page.locator(s"span .StretchedBox .StretchedBox").nth(from).boundingBox()
      }
      bb1 <- ZIO.attemptBlocking {
        page.locator(s"span .StretchedBox .StretchedBox").nth(to).boundingBox()
      }
      _ <- drugAndDrop(bb0, bb1)
    } yield ()
  }

  /** Upload a new pic in a free slot */
  def removePic(picIndex: Int) = PageLock.lock {
    for {
      _ <- ZIO.logInfo(s"Remove pic from slot $picIndex")
      page <- ZIO.service[Page]
      _ <- ZIO.attemptBlocking {
        val selector = s".Hidden :text(\"${picIndex + 1}/9\")"
        if (page.isVisible(selector)) {
          page.locator(selector).click()
        }
      }
      _ <- ZIO.sleep(zio.Duration.fromSeconds(1))
      _ <- ZIO.attemptBlocking {
        page.locator("div[role=dialog]").locator("button").locator(":text(\"Delete\")").click()
        //page.mouse.click(bb.x + bb.width / 2, bb.y + bb.height / 2);
      }
      _ <- ZIO.sleep(zio.Duration.fromSeconds(1))
    } yield ()
  }

  def countPic() = PageLock.lock {
    for {
      _ <- navigate("https://tinder.com/app/profile/edit")
      _ <- ZIO.sleep(zio.Duration.fromSeconds(5))
      page <- ZIO.service[Page]
      _ <- ZIO.attemptBlocking {
        val x = page.locator(s".StretchedBox .Hidden :text(\"Remove Media\")").count()
        println(s">>>> ${x}")
      }
    } yield ()
  }

  def editPic(pics: Seq[String]) = PageLock.lock {
    for {
      _ <- ZIO.logInfo("Upload a new pictures")
      page <- ZIO.service[Page]
      _ <- navigate("https://tinder.com/app/profile/edit")
      _ <- ZIO.sleep(zio.Duration.fromSeconds(5))
      picCount <- ZIO.attemptBlocking {
        page.locator(s".StretchedBox .Hidden :text(\"Remove Media\")").count()
      }
      _ <- ZIO.logInfo(s"Initial Number of pictures: $picCount")
      // Remove old pics
      _ <- removePic(1).repeatN(picCount - 4)
      // Remove upload new ones
      _ <- ZIO.collectAll {
        pics.map(fileName => uploadPic(s"/home/vasyaod/work/tinder-bot/pics/$fileName", 1))
      }
      // Save data
      _ <- ZIO.attempt {
        page.locator(":text(\"Save\")").nth(1).click()
      }
      _ <- ZIO.sleep(zio.Duration.fromSeconds(3))
      // Send page back to title page
      _ <- navigate("https://tinder.com/")
    } yield ()
  }
}