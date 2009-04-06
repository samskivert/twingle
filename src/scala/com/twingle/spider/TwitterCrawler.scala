package com.twingle.spider

import java.text.SimpleDateFormat
import java.util.Date

import scala.xml.{Node, XML}

import com.twingle.Log.log

/** Describes a Twitter user whose friend statuses are to be crawled. */
class TwitterUser (val username :String, val password :String)

object TwitterCrawlerApp {
  def main (args :Array[String]) {
    // read command-line arguments
    if (args.length < 2) {
      log.warning("No username and password specified.")
      exit
    }
    val username = args(0)
    val password = args(1)

    // construct the user list to be queried
    val users = List(new TwitterUser(username, password))

    // query twitter for the latest statuses
    val crawler = new TwitterCrawler(new URLFetcher)
    val statuses = crawler.crawl(users)
    statuses.foreach(log.info(_))

    0
  }
}

class TwitterCrawler (urlFetcher :URLFetcher) {
  /** Simple status record to contain a tweet. */
  class TwitterStatus {
    var createdAt :Date = null
    var tweetId :Int = 0
    var text :String = null
    var userId :Int = 0
    var userName :String = null
    var screenName :String = null

    override def toString () = {
      val buf :StringBuffer = new StringBuffer
      buf.append("[createdAt=").append(createdAt)
      buf.append(", tweetId=").append(tweetId)
      buf.append(", text=").append(text)
      buf.append(", userId=").append(userId)
      buf.append(", userName=").append(userName)
      buf.append(", screenName=").append(screenName)
      buf.toString
    }
  }

  def crawl (users :Seq[TwitterUser]) :Seq[Seq[TwitterStatus]] = {
    users.map(getFriendsTimeline(_))
  }

  def getFriendsTimeline (user :TwitterUser) :Seq[TwitterStatus] = {
    val url = "http://twitter.com/statuses/friends_timeline.xml"
    val rsp = urlFetcher.getAuthedUrl(url, "twitter.com", user.username,
                                      user.password)
    if (rsp.resultCode != 200) {
      log.warning("Error fetching friends timeline",
                  "result", ""+rsp.resultCode, "body", rsp.body)
      return null
    }

    val doc = XML.loadString(rsp.body)
    if (doc == null) {
      return null
    }
    
    val statuses = (doc \\ "status")
    statuses.map(parseStatusElement(_))
  }

  protected def parseStatusElement (e :Node) :TwitterStatus = {
    val status = new TwitterStatus

    status.createdAt =
      TwitterClient.dateFormatter.parse((e \ "created_at").text)
    status.tweetId = (e \ "id").text.toInt
    status.text = (e \ "text").text

    val userElem = (e \ "user")
    status.userId = (userElem \ "id").text.toInt
    status.userName = (userElem \ "name").text
    status.screenName = (userElem \ "screen_name").text

    status
  }
}

object TwitterCrawler {
  val dateFormatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy")
}
