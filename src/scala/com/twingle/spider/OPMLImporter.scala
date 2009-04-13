//
// $Id$

package com.twingle.spider

import java.io.{BufferedReader, FileReader}

import scala.xml.{Node, XML}

import com.twingle.Log.log

class OPMLImporter
{
  /** Simple record detailing an RSS feed to which a user is subscribed. */
  case class Feed (val title :String,
                   val feedType :String,
                   val xmlUrl :String,
                   val htmlUrl :String)

  /**
   * Parses the OPML file at the specified path and returns all {@link Feed} records
   * described therein.
   */
  def parseFile (path :String) :Seq[Feed] = {
    val doc = XML.load(new BufferedReader(new FileReader(path)))
    val outlineElements = (doc \\ "outline")
    outlineElements.flatMap(e => parseOutlineElement(e))
  }

  protected def parseOutlineElement (e :Node) :Option[Feed] = {
    // OPML files may have nested "outline" elements for hierarchical categorization.  these
    // elements contain no xml url feed attribute, so for now we just skip them.
    val xmlUrlElement = (e \ "@xmlUrl")
    if (xmlUrlElement.isEmpty) {
      return None
    }

    val xmlUrl = xmlUrlElement.text
    val title = (e \ "@title").text
    val feedType = (e \ "type").text
    val htmlUrl = (e \ "@htmlUrl").text
    Some(Feed(title, feedType, xmlUrl, htmlUrl))
  }
}

object OPMLImporterApp
{
  def main (args :Array[String]) {
    // read command-line arguments
    if (args.length < 1) {
      log.warning("No opml file path specified.")
      exit
    }
    val opmlPath = args(0)

    val importer = new OPMLImporter
    val feeds = importer.parseFile(opmlPath)
    feeds.foreach(log.info(_))
  }
}
