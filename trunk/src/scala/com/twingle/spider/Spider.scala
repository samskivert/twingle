//
// $Id$

package com.twingle.spider

abstract class Spider (val urlFetcher :URLFetcher) {
  def crawl (configs :Seq[SpiderConfig]) :Seq[SpiderResult]
}
