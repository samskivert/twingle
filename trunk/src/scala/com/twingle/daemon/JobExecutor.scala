//
// $Id$

package com.twingle.daemon

import scala.actors.Actor
import scala.actors.Actor._

import com.twingle.Log.log
import com.twingle.persist.Database

/**
 * Handles the processing of jobs in the Twingle daemon.
 */
class JobExecutor (db :Database)
{
  val env :Env = _env

  def start () {
    // start our environment and pass it a scheduler to get things started
    _env.start
    _env ! new Scheduler
  }

  private class Scheduler {
    def schedule (env :Actor) {
      log.info("TODO!") // TODO
      requeue(env)
    }

    def requeue (env :Actor) {
      actor {
        Thread.sleep(5000L)
        env ! Scheduler.this
      }
    }
  }

  // create an environment that executes jobs one after another
  private[this] val _env = new Actor with Env {
    val db = JobExecutor.this.db

    def queueJob (job :Job) {
      this ! job
    }

    def act () {
      loop {
        react {
          case (job :Job) => job.run(this)
          case (sched :Scheduler) => sched.schedule(this)
          case (msg :AnyRef) => log.warning("Got unknown message", "msg", msg)
        }
      }
    }
  }
}
