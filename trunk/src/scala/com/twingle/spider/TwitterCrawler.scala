package com.twingle.spider

import com.twingle.Log.log

object TwitterCrawler {
  
  def main (args :Array[String]) {
    // read command-line arguments
    if (args.length < 2) {
      log.warning("No username and password specified.")
      exit
    }
    val username = args(0)
    val password = args(1)

    // query twitter for the latest statuses
    val client = new TwitterClient(new URLFetcher)
    val statuses = client.friendsTimeline(args(0), args(1))
    statuses.foreach(log.info(_))

    0
  }
}
