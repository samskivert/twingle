//
// $Id$

package com.twingle.spider

import java.util.Date

import scala.io.Source

import com.twingle.Log.log

case class URLDocument (val location :String,
                        val name :String,
                        val text :String,
                        val bits :String,
                        val created :Date,
                        val lastModified :Date)

object URLCrawlerApp {
  def main (args :Array[String]) {
    // parse command-line arguments
    if (args.length == 0) {
      log.warning("No url file path specified.")
      exit
    }
    val fetchPath = args(0)

    // load list of urls to fetch
    log.info("Loading urls to fetch [path=" + fetchPath + "].")
    val urls = Source.fromFile(fetchPath).getLines.map(_.stripLineEnd).toList

    // crawl the urls
    val crawler = new URLCrawler(new URLFetcher)
    val docs = crawler.crawl(urls)
    log.info("Fetched documents", "docs", docs)
  }
}

class URLCrawler (urlFetcher :URLFetcher) {
  def crawl (urls :Seq[String]) :Seq[URLDocument] = {
    urls.map(u => {
      val created = new Date
      val location = u
      val rsp = _urlFetcher.getUrl(u)
      val text = rsp.body
      val lastModified = new Date
      URLDocument(location, null, text, null, created, lastModified)
    })
  }

  private[this] val _urlFetcher = new URLFetcher
}

package tests {
  import org.scalatest.Suite
  
  class URLCrawlerSuite extends Suite {
    def testCrawl () {
      val urls = List("http://www.yahoo.com", "http://www.news.com")
      val crawler = new URLCrawler(new URLFetcher)
      val docs = crawler.crawl(urls)

      val ydoc = docs(0)
      expect("http://www.yahoo.com") { ydoc.location }
      // TODO: finish this

      val ndoc = docs(1)
      expect("http://www.news.com") { ndoc.location }
      // TODO: finish this
    }
  }
}
