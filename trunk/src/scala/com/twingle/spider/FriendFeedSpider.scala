//
// $Id$

package com.twingle.spider

import java.text.SimpleDateFormat
import java.util.Date

import scala.xml.{Node, XML}

import com.twingle.Log.log

/** Details a single user's FriendFeed configuration. */
case class FriendFeedSpiderConfig  (val username :String, val remoteKey :String) 
  extends SpiderConfig {
  def toString (buf :StringBuffer) = {
    buf.append("username=").append(username)
    buf.append(", remoteKey=").append(remoteKey)
  }
}

/** Describes a service for which posts may be seen in a user's FriendFeed feed. */
case class FriendFeedService (val profileUrl :String,
                              val iconUrl :String,
                              val entryType :String,
                              val name :String) {
  override def toString () = {
    val buf :StringBuffer = new StringBuffer
    buf.append("[profileUrl=").append(profileUrl)
    buf.append(", iconUrl=").append(iconUrl)
    buf.append(", entryType=").append(entryType)
    buf.append(", name=").append(name)
    buf.append("]").toString
  }
}

/** Describes a FriendFeed user's profile. */
case class FriendFeedProfile (val profileUrl :String,
                              val nickname :String,
                              val ffId :String,
                              val name :String) {
  override def toString () = {
    val buf :StringBuffer = new StringBuffer
    buf.append("[profileUrl=").append(profileUrl)
    buf.append(", nickname=").append(nickname)
    buf.append(", ffId=").append(ffId)
    buf.append(", name=").append(name)
    buf.append("]").toString
  }
}

/** Describes a single post in a user's FriendFeed feed. */
// TODO: anonymous, hidden, id(internal)
case class FriendFeedEntry (val updated :Date, 
                            val service :FriendFeedService,
                            val title :String,
                            val link :String,
                            val published :Date,
                            val ffId :String,
                            val userProfile :FriendFeedProfile)
{
  override def toString () = {
    val buf :StringBuffer = new StringBuffer
    buf.append("[updated=").append(updated)
    buf.append(", service=").append(service)
    buf.append(", title=").append(title)
    buf.append(", link=").append(link)
    buf.append(", published=").append(published)
    buf.append(", ffId=").append(ffId)
    buf.append(", userProfile=").append(userProfile)
    buf.append("]").toString
  }
}

case class FriendFeedSpiderResult (val entries :Seq[FriendFeedEntry])
  extends SpiderResult {
  def toString (buf :StringBuffer) = {
    buf.append("entries=").append(entries)
  }
}

/**
 * The main crawler for fetching a user's posts to their FriendFeed feed.
 *
 * See http://code.google.com/p/friendfeed-api/wiki/ApiDocumentation
 * for api documentation.
 *
 * A user's remote key is available when logged in at:
 *
 * https://friendfeed.com/account/api
 *
 * A remote key is required for accessing private user feeds.
 */
class FriendFeedSpider (urlFetcher :URLFetcher) extends Spider(urlFetcher) {
  def crawl (configs :Seq[SpiderConfig]) :Seq[SpiderResult] =
    configs.map(_ match { case c :FriendFeedSpiderConfig => getUserPosts(c) })

  def getUserPosts (config :FriendFeedSpiderConfig) :SpiderResult = {
    // build the url to retrieve this user's entries
    val url = 
      "http://friendfeed.com/api/feed/user/" + config.username + "?format=xml"

    // submit the request, only using authentication if required
    val rsp = if (config.remoteKey != null) {
      urlFetcher.getAuthedUrl(url, "friendfeed.com", config.username, 
                              config.remoteKey)

    } else {
      urlFetcher.getUrl(url)
    }

    // turn the response into an xml document
    val doc = XML.loadString(rsp.body)
    if (doc == null) {
      return null
    }

    // parse out the user's posted entries
    val entryElements = (doc \\ "entry")
    val entries = entryElements.map(parseEntryElement(_))

    // create the final spider result
    FriendFeedSpiderResult(entries)
  }

  protected def parseEntryElement (e :Node) :FriendFeedEntry = {
    // parse the service record
    val serviceElement = (e \ "service")
    val service = FriendFeedService((serviceElement \ "profileUrl").text,
                                    (serviceElement \ "iconUrl").text,
                                    (serviceElement \ "entryType").text,
                                    (serviceElement \ "name").text)

    // parse the user profile record
    val userElement = (e \ "user")
    val profile = FriendFeedProfile((userElement \ "profileUrl").text,
                                    (userElement \ "nickname").text,
                                    (userElement \ "id").text,
                                    (userElement \ "name").text)

    // construct the overall entry record for this post
    FriendFeedEntry(_dateFormat.parse((e \ "updated").text),
                    service,
                    (e \ "title").text,
                    (e \ "link").text,
                    _dateFormat.parse((e \ "published").text),
                    (e \ "id").text,
                    profile)
  }

  /** Date format parser for FriendFeed dates which are in RFC 3339 format. */
  private[this] val _dateFormat = new SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss'Z'")
}

object FriendFeedSpiderApp {
  def main (args :Array[String]) {
    // read command-line arguments
    if (args.length < 1) {
      log.warning("No username specified.")
      exit
    }
    val username = args(0)
    val remoteKey = if (args.length > 1) args(1) else null

    // construct the user list to be queried
    val configs = List(FriendFeedSpiderConfig(username, remoteKey))

    // query friendfeed for the latest posts
    val crawler = new FriendFeedSpider(new URLFetcher)
    val results = crawler.crawl(configs)
    results.foreach(log.info(_))
  }
}

