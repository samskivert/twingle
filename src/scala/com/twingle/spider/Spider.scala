//
// $Id$

package com.twingle.spider

import com.twingle.daemon.Job
import com.twingle.persist.DatabaseObject

abstract class Spider

/**
 * Spider utility methods.
 */
object Spider
{
  /** Defines the configuration elements common to all spiders. */
  abstract class Config extends DatabaseObject {
    /** Whether or not this spider is enabled. */
    def enabled () :Boolean = reqA(booleanM, 'enabled).data

    /** The frequency (in seconds) at which we run this spider. */
    def runEvery () :Int = reqA(intM, 'runEvery).data

    /** Creates a job that runs this spider. */
    def createJob () :Job
  }

  abstract class ConfigBuilder extends DatabaseObject.Builder {
    def enabled (enabled :Boolean) :this.type = { add('enabled, enabled); this }
    def runEvery (runEvery :Int) :this.type = { add('runEvery, runEvery); this }
  }
}
