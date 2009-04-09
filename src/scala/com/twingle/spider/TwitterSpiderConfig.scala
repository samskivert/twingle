//
// $Id$

package com.twingle.spider

/**
 * Contains configuration information for the Twitter spider.
 */
class TwitterSpiderConfig extends SpiderConfig
{
  /** The Twitter account's username. */
  def username () :String = reqA(stringM, "username").data

  /** The Twitter account's password. TODO: can we encyrpt this? */
  def password () :String = reqA(stringM, "password").data
}

/**
 * Utility methods for TwitterSpiderConfig.
 */
object TwitterSpiderConfig
{
  def builder () = new SpiderConfig.Builder {
    def username (username :String) = { add("username", username); this }
    def password (password :String) = { add("password", password); this }
    def build :TwitterSpiderConfig = build(new TwitterSpiderConfig)
  }
}
