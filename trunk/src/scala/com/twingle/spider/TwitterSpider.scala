//
// $Id$

package com.twingle.spider

import java.text.SimpleDateFormat
import java.util.Date

import scala.xml.{Node, XML}

import com.twingle.Log.log

class TwitterSpider (urlFetcher :URLFetcher) extends Spider(urlFetcher)
{
  import TwitterSpider._

  def crawl (configs :Seq[Spider.Config]) :Seq[Spider.Result] =
    configs.map(c => getFriendsTimeline(c.asInstanceOf[Config]))

  def getFriendsTimeline (config :Config) :Result = {
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
    Result(statuses)
  }

  protected def parseStatusElement (e :Node) :Status = {
    val createdAt = _dateFormat.parse((e \ "created_at").text)
    val tweetId = (e \ "id").text.toInt
    val text = (e \ "text").text

    val userElem = (e \ "user")
    val userId = (userElem \ "id").text.toInt
    val userName = (userElem \ "name").text
    val screenName = (userElem \ "screen_name").text

    Status(tweetId, createdAt, userId, userName, screenName, text)
  }

  private[this] val _dateFormat =
    new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy")
}

/**
 * Utility methods for TwitterSpider.
 */
object TwitterSpider
{
  /** Contains configuration information for the Twitter spider. */
  class Config extends Spider.Config {
    /** The Twitter account's username. */
    def username () :String = reqA(stringM, "username").data

    /** The Twitter account's password. TODO: can we encyrpt this? */
    def password () :String = reqA(stringM, "password").data
  }

  /** Simple status record to contain a tweet. */
  case class Status (val tweetId :Int,
                     val createdAt :Date,
                     val userId :Int,
                     val userName :String,
                     val screenName :String,
                     val text :String)

  case class Result (val entries :Seq[Status]) extends Spider.Result

  def configBuilder () = new Spider.ConfigBuilder {
    def username (username :String) = { add("username", username); this }
    def password (password :String) = { add("password", password); this }
    def build :TwitterSpider.Config = build(new TwitterSpider.Config)
  }
}

object TwitterSpiderApp
{
  def main (args :Array[String]) {
    // read command-line arguments
    if (args.length < 2) {
      log.warning("No username and password specified.")
      exit
    }
    val username = args(0)
    val password = args(1)

    // construct the user list to be queried
    val configs = List(TwitterSpider.configBuilder.
                       enabled(true).runEvery(60).username(username).password(password).build)

    // query twitter for the latest statuses
    val spider = new TwitterSpider(new URLFetcher)
    val results = spider.crawl(configs)
    results.foreach(log.info(_))
  }
}
