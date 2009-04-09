//
// $Id$

package com.twingle.spider

import com.twingle.persist.DatabaseObject

/**
 * Defines the configuration elements common to all spiders.
 */
abstract class SpiderConfig extends DatabaseObject
{
  /** Whether or not this spider is enabled. */
  def enabled () :Boolean = reqA(booleanM, "enabled").data

  /** The frequency (in seconds) at which we run this spider. */
  def runEvery () :Int = reqA(intM, "runEvery").data
}

/**
 * SpiderConfig utility methods.
 */
object SpiderConfig
{
  abstract class Builder extends DatabaseObject.Builder {
    def enabled (enabled :Boolean) :this.type = { add("enabled", enabled); this }
    def runEvery (runEvery :Int) :this.type = { add("runEvery", runEvery); this }
  }
}
