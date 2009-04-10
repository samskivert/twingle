//
// $Id$

package com.twingle.spider

import java.text.SimpleDateFormat
import java.util.Date

import scala.xml.{Node, XML}

import com.twingle.Log.log
import com.twingle.daemon.{Env, Job}
import com.twingle.model.Document

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
class FriendFeedSpider (val urlFetcher :URLFetcher) extends Spider
{
  import FriendFeedSpider._

  def getUserEntries (config :Config) :Seq[Entry] = {
    // build the url to retrieve this user's entries
    val url = 
      "http://friendfeed.com/api/feed/user/" + config.username + "?format=xml"

    // submit the request, only using authentication if required
    val rsp = config.remoteKey match {
      case Some(remoteKey) =>
        urlFetcher.getAuthedUrl(url, "friendfeed.com", config.username, remoteKey)
      case None => urlFetcher.getUrl(url)
    }

    // turn the response into an xml document
    val doc = XML.loadString(rsp.body)
    if (doc == null) {
      return null
    }

    // parse out the user's posted entries
    val entryElements = (doc \\ "entry")
    entryElements.map(parseEntryElement(_))
  }

  protected def parseEntryElement (e :Node) :Entry = {
    // parse the service record
    val serviceElement = (e \ "service")
    val service = Service((serviceElement \ "profileUrl").text,
                          (serviceElement \ "iconUrl").text,
                          (serviceElement \ "entryType").text,
                          (serviceElement \ "name").text)

    // parse the user profile record
    val userElement = (e \ "user")
    val profile = Profile((userElement \ "profileUrl").text,
                          (userElement \ "nickname").text,
                          (userElement \ "id").text,
                          (userElement \ "name").text)

    // construct the overall entry record for this post
    Entry(_dateFormat.parse((e \ "updated").text),
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

object FriendFeedSpider
{
  /** Details a single user's FriendFeed configuration. */
  class Config extends Spider.Config {
    /** The FriendFeed account's username. */
    def username () :String = reqA(stringM, "username").data

    /** The FriendFeed account's remote key. */
    def remoteKey () :Option[String] = optA(stringM, "remoteKey").data

    def createJob () = new Job() {
      def run (env :Env) {
        val spider = new FriendFeedSpider(new URLFetcher)
        spider.getUserEntries(Config.this).foreach(e => env.db.store(toDocument(e)))
      }

      def toDocument (e :Entry) =
        Document.builder.name(e.title).created(e.published).lastModified(e.updated).
          location(e.link).build
    }
  }

  /** Describes a service for which posts may be seen in a user's FriendFeed feed. */
  case class Service (val profileUrl :String,
                      val iconUrl :String,
                      val entryType :String,
                      val name :String)

  /** Describes a FriendFeed user's profile. */
  case class Profile (val profileUrl :String,
                      val nickname :String,
                      val ffId :String,
                      val name :String)

  /** Describes a single post in a user's FriendFeed feed. */
  // TODO: anonymous, hidden, id(internal)
  case class Entry (val updated :Date, 
                    val service :Service,
                    val title :String,
                    val link :String,
                    val published :Date,
                    val ffId :String,
                    val userProfile :Profile)

  def configBuilder () = new Spider.ConfigBuilder {
    def username (username :String) = { add("username", username); this }
    def remoteKey (remoteKey :String) = { add("remoteKey", remoteKey); this }
    def build :FriendFeedSpider.Config = build(new FriendFeedSpider.Config)
  }
}

object FriendFeedSpiderApp
{
  def main (args :Array[String]) {
    // read command-line arguments
    if (args.length < 1) {
      log.warning("No username specified.")
      exit
    }

    // construct the user config to be queried
    var builder = FriendFeedSpider.configBuilder.username(args(0))
    if (args.length > 1) {
      builder = builder.remoteKey(args(1))
    }
    val config = builder.build

    // query friendfeed for the user's latest entries
    val spider = new FriendFeedSpider(new URLFetcher)
    val entries = spider.getUserEntries(config)
    entries.foreach(log.info(_))
  }
}
