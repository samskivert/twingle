//
// $Id$

package com.twingle.spider

import java.text.{ParseException, SimpleDateFormat}
import java.util.Date

import scala.xml.{Node, XML}

import com.twingle.Log.log
import com.twingle.daemon.{Env, Job}
import com.twingle.model.Document

/**
 * The main crawler for fetching posts from an RSS feed.
 */
class RSSFeedSpider (val urlFetcher :URLFetcher) extends Spider
{
  import RSSFeedSpider._

  def getFeedArticles (config :Config) :Seq[Article] = {
    // submit the request for the latest feed xml contents
    val rsp = urlFetcher.getUrl(config.url)

    // turn the response into an xml document
    val doc = XML.loadString(rsp.body)

    if (isAtomFeed(doc)) {
      // parse out any posted articles
      log.info("Parsing atom feed.")
      val entryElements = (doc \\ "entry")
      entryElements.flatMap(parseAtomEntryElement(_))

    } else {
      log.info("Unknown feed format.")

      // just return an empty list
      List[Article]()
    }
  }

  /** Returns whether the RSS feed rooted under the supplied XML node is an Atom feed. */
  protected def isAtomFeed (doc :Node) :Boolean = {
    // XXX for some reason below returns nothing, need to investigate
    // ((doc \ "feed" \ "@xmlns").text == "http://www.w3.org/2005/Atom")
    true
  }

  /** Parses the supplied xml node representing an "entry" element in an Atom-formatted RSS feed. */
  protected def parseAtomEntryElement (e :Node) :Option[Article] = {
    val publishedStr = (e \ "published").text
    val published = parseDate(publishedStr) match {
      case (Some(s)) => s
      case None => {
        log.debug("Unparseable date", "date", publishedStr)
        // fall back to the current time
        new Date
      }
    }

    val title = (e \ "title").text
    val guid = (e \ "id").text
    val link = (e \ "link" \ "@href").text
    val content = (e \ "content").text

    val updatedStr = (e \ "updated").text
    val updated = parseDate(updatedStr) match {
      case (Some(s)) => s
      case None => {
        log.debug("Unparseable date", "date", updatedStr)
        // fall back to the current time
        new Date
      }
    }

    Some(Article(published, updated, guid, title, link, content))
  }

  protected def parseDate (date :String) :Option[Date] = {
    // try each of our supported date formats in order
    _dateFormats.foreach(f => {
      try {
        return Some(f.parse(date))
        
      } catch {
        case (pe :ParseException) => log.debug("Failed to parse.", pe)
      }
    })

    None
  }
  
  private[this] val _defaultDateFormat = new SimpleDateFormat
  private[this] val _rfc339DateFormat = new SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss'Z'")
  private[this] val _dateFormats = List(_defaultDateFormat, _rfc339DateFormat)
}

object RSSFeedSpider
{
  /** Details a single RSS feed configuration. */
  class Config extends Spider.Config {
    /** The main url from which the rss feed xml data is to be fetched. */
    def url () :String = reqA(stringM, "url").data

    def createJob () = new Job() {
      def run (env :Env) {
        val spider = new RSSFeedSpider(new URLFetcher)
        spider.getFeedArticles(Config.this).foreach(a => env.db.store(toDocument(a)))
      }

      def toDocument (a :Article) =
        Document.builder.name(a.title).created(a.published).lastModified(a.updated).
          location(a.link).text(a.content).build
    }
  }

  /** Describes a single article in an RSS feed. */
  case class Article (val published :Date,
                      val updated :Date,
                      val guid :String,
                      val title :String,
                      val link :String,
                      val content :String)

  def configBuilder () = new Spider.ConfigBuilder {
    def url (url :String) = { add("url", url); this }
    def build :RSSFeedSpider.Config = build(new RSSFeedSpider.Config)
  }
}

object RSSFeedSpiderApp
{
  def main (args :Array[String]) {
    // read command-line arguments
    if (args.length < 1) {
      log.warning("No sample rss feed url specified.")
      exit
    }

    val config = RSSFeedSpider.configBuilder.url(args(0)).build
    val spider = new RSSFeedSpider(new URLFetcher)
    val articles = spider.getFeedArticles(config)
    articles.foreach(a => log.info("Article ", "title", a.title, "link", a.link, "guid", a.guid))
  }
}
