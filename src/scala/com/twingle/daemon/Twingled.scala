//
// $Id$

package com.twingle.daemon

import scala.actors.Actor
import scala.actors.Actor._

import com.twingle.Log.log
import com.twingle.persist.Database
import com.twingle.persist.TrivialDatabase

/**
 * The main entry point for the Twingle daemon.
 */
object Twingled
{
  def main (args :Array[String]) {
    val tdb = new TrivialDatabase // TODO

    // create an environment that executes jobs one after another
    val env = new Actor with Env {
      val db = tdb
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
    env.start

    // pass a scheduler to the environment to get things started
    env ! new Scheduler

    log.info("Twingle daemon running.")
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
}
