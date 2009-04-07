//
// $Id$

package com.twingle.spider

import java.text.SimpleDateFormat
import java.util.Date

import scala.xml.{Node, XML}

import com.twingle.Log.log

/** 
 * Describes a FriendFeed user whose posts are to be crawled, suitable
 * for passing to the {@link FriendFeedCrawler}.
 */
case class FriendFeedUser (val username :String, val password :String) {
  override def toString () = {
    val buf :StringBuffer = new StringBuffer
    buf.append("[username=").append(username)
    buf.append(", password=").append(password)
    buf.append("]").toString
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

object FriendFeedCrawlerApp {
  def main (args :Array[String]) {
    // read command-line arguments
    if (args.length < 1) {
      log.warning("No username specified.")
      exit
    }
    val username = args(0)
    val password = if (args.length > 1) args(1) else null

    // construct the user list to be queried
    val users = List(FriendFeedUser(username, password))

    // query friendfeed for the latest posts
    val crawler = new FriendFeedCrawler(new URLFetcher)
    val posts = crawler.crawl(users)
    posts.foreach(log.info(_))
  }
}

/**
 * The main crawler for fetching a user's posts to their FriendFeed feed.
 *
 * See http://code.google.com/p/friendfeed-api/wiki/ApiDocumentation
 * for api documentation.
 *
 * The user "password" must be the remote key as available when logged 
 * in at:
 *
 * https://friendfeed.com/account/api
 *
 * A remote key is required for accessing private user feeds.
 */
class FriendFeedCrawler (urlFetcher :URLFetcher) {
  def crawl (users :Seq[FriendFeedUser]) :Seq[Seq[FriendFeedEntry]] = {
    users.map(getUserPosts(_))
  }

  def getUserPosts (user :FriendFeedUser) :Seq[FriendFeedEntry] = {
    // build the url to retrieve this user's entries
    val url = "http://friendfeed.com/api/feed/user/" + user.username + "?format=xml"

    // submit the request, only using authentication if required
    val rsp = if (user.password != null) {
      urlFetcher.getAuthedUrl(url, "friendfeed.com", user.username, user.password)
    } else {
      urlFetcher.getUrl(url)
    }

    // turn the response into an xml document
    val doc = XML.loadString(rsp.body)
    if (doc == null) {
      return null
    }

    // parse out the user's posted entries
    val entries = (doc \\ "entry")
    entries.map(parseEntryElement(_))
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
