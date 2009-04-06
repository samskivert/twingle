package com.twingle.spider

import java.util.Date

import scala.io.Source

import com.twingle.Log.log

/**
 * Simple app to fetch browser bookmark and history page url content and
 * index it for subsequent long-term storage and searching in the Twingle
 * system.
 */
object BrowserApp {
  def main (args :Array[String]) {
    // parse command-line arguments
    if (args.length == 0) {
      log.warning("No url file path specified.")
      exit
    }
    val fetchPath = args(0)

    // load list of urls to fetch.  eventually we'll get this
    // from the local browser bookmark and history.
    log.info("Loading urls to fetch [path=" + fetchPath + "].")
    val urls = Source.fromFile(fetchPath).getLines

    // fetch each page and create a Document record
    val docs = urls.map(u => processUrl(u.stripLineEnd)).toList

    // someday we'll actually do something with these
    docs
  }

  protected def fetchUrl (url :String) :String = Source.fromURL(url).toString()

  protected def processUrl (url :String) = {
    log.info("Processing [url=" + url + "].")

    val doc = new Document
    doc.created = new Date
    doc.location = url
    doc.text = fetchUrl(url)
    doc.lastModified = new Date

    log.info("Processed [doc=" + doc + "].")

    doc
  }
}
