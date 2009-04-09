//
// $Id$

package com.twingle.daemon

/**
 * An interface that must be implemented by our periodic jobs.
 */
trait Job
{
  /** Runs this job. */
  def run (env :Env) :Unit
}
