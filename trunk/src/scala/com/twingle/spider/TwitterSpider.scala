//
// $Id$

package com.twingle.spider

import java.text.SimpleDateFormat
import java.util.Date

import scala.xml.{Node, XML}

import com.twingle.Log.log

/** Describes a Twitter user whose friend statuses are to be crawled. */
case class TwitterSpiderConfig (val username :String, val password :String) 
  extends SpiderConfig {
  def toString (buf :StringBuffer) = {
    buf.append("username=").append(username)
    buf.append(", password=").append(password)
  }
}

object TwitterSpiderApp {
  def main (args :Array[String]) {
    // read command-line arguments
    if (args.length < 2) {
      log.warning("No username and password specified.")
      exit
    }
    val username = args(0)
    val password = args(1)

    // construct the user list to be queried
    val configs = List(TwitterSpiderConfig(username, password))

    // query twitter for the latest statuses
    val spider = new TwitterSpider(new URLFetcher)
    val results = spider.crawl(configs)
    results.foreach(log.info(_))
  }
}

case class TwitterSpiderResult (val entries :Seq[TwitterStatus])
  extends SpiderResult {
  def toString (buf :StringBuffer) = {
    buf.append("entries=").append(entries)
  }
}

/** Simple status record to contain a tweet. */
case class TwitterStatus (val createdAt :Date, 
                          val tweetId :Int,
                          val text :String,
                          val userId :Int, 
                          val userName :String,
                          val screenName :String)
{
  override def toString () = {
    val buf :StringBuffer = new StringBuffer
    buf.append("[createdAt=").append(createdAt)
    buf.append(", tweetId=").append(tweetId)
    buf.append(", text=").append(text)
    buf.append(", userId=").append(userId)
    buf.append(", userName=").append(userName)
    buf.append(", screenName=").append(screenName)
    buf.append("]").toString
  }
}

class TwitterSpider (urlFetcher :URLFetcher) extends Spider(urlFetcher) {
  def crawl (configs :Seq[SpiderConfig]) :Seq[SpiderResult] =
    configs.map(_ match { case c :TwitterSpiderConfig => getFriendsTimeline(c) })

  def getFriendsTimeline (config :TwitterSpiderConfig) :SpiderResult = {
    // submit request for specified user's latest friend status timeline
    val url = "http://twitter.com/statuses/friends_timeline.xml"
    val rsp = urlFetcher.getAuthedUrl(url, "twitter.com", config.username,
                                      config.password)

    // turn the response into an xml document
    val doc = XML.loadString(rsp.body)
    if (doc == null) {
      return null
    }
    
    // parse out each user status entry
    val statusElements = (doc \\ "status")
    val statuses = statusElements.map(parseStatusElement(_))

    // create the final spider result
    TwitterSpiderResult(statuses)
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
