//
// $Id$

package com.twingle.daemon

import com.twingle.persist.Database

/**
 * The environment in which jobs run. Provides access to the database and allows additional jobs to
 * be queued up for execution.
 */
trait Env
{
  /** Provides access to the database. */
  val db :Database

  /** Queues up a job for later execution. */
  def queueJob (job :Job)
}
