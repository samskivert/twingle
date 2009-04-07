//
// $Id$

package com.twingle.spider

abstract class SpiderConfig {
  override def toString () = {
    val buf :StringBuffer = new StringBuffer
    toString(buf)
    buf.append("]").toString
  }

  def toString (buf :StringBuffer)
}
