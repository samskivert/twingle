//
// $Id$

package com.twingle.spider

import java.text.SimpleDateFormat
import java.util.Date

import scala.xml.{Node, XML}

import com.twingle.Log.log

/** Describes a Twitter user whose friend statuses are to be crawled. */
case class TwitterUser (username :String, password :String) {
  override def toString () = {
    val buf :StringBuffer = new StringBuffer
    buf.append("[username=").append(username)
    buf.append(", password=").append(password)
    buf.append("]").toString
  }
}

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
    val users = List(TwitterUser(username, password))

    // query twitter for the latest statuses
    val crawler = new TwitterCrawler(new URLFetcher)
    val statuses = crawler.crawl(users)
    statuses.foreach(log.info(_))
  }
}

class TwitterCrawler (urlFetcher :URLFetcher) {
  /** Simple status record to contain a tweet. */
  case class TwitterStatus(createdAt :Date, tweetId :Int, text :String,
                           userId :Int, userName :String, screenName :String)
  {
    override def toString () = {
      val buf :StringBuffer = new StringBuffer
      buf.append("[createdAt=").append(createdAt)
      buf.append(", tweetId=").append(tweetId)
      buf.append(", textLen=").append(if (text != null) text.size else 0)
      buf.append(", userId=").append(userId)
      buf.append(", userName=").append(userName)
      buf.append(", screenName=").append(screenName)
      buf.append("]").toString
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
    val createdAt = _dateFormat.parse((e \ "created_at").text)
    val tweetId = (e \ "id").text.toInt
    val text = (e \ "text").text

    val userElem = (e \ "user")
    val userId = (userElem \ "id").text.toInt
    val userName = (userElem \ "name").text
    val screenName = (userElem \ "screen_name").text

    TwitterStatus(createdAt, tweetId, text, userId, userName, screenName)
  }

  private[this] val _dateFormat =
    new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy")
}
