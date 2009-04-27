//
// $Id$

package com.twingle.daemon

import scala.actors.Actor
import scala.actors.Actor._

import com.twingle.Log.log
import com.twingle.spider.Spider
import com.twingle.persist.Database

/**
 * Handles the processing of jobs in the Twingle daemon.
 */
class JobExecutor (db :Database)
{
  def env () :Env = _env

  def start () {
    // start our environment and pass it a scheduler to get things started
    _env.start
    _env ! new Scheduler
  }

  def addSpider (spider :Spider.Config) {
    _spiders = new SpiderState(spider) :: _spiders
  }

  private class Scheduler {
    def schedule (env :Actor) {
      val now = System.currentTimeMillis
      _spiders.foreach(s => s.maybeCreateJob(now))
      requeue(env)
    }

    def requeue (env :Actor) {
      actor {
        Thread.sleep(5000L)
        env ! Scheduler.this
      }
    }
  }

  private class SpiderState (conf :Spider.Config) {
    def maybeCreateJob (now :Long) {
      if (now > _nextInvoke) {
        _nextInvoke = now + conf.runEvery * 1000L
        log.info("Queueing spider " + conf.getClass.getName + "...")
        _env.queueJob(conf.createJob)
      }
    }
    private[this] var _nextInvoke :Long = 0L
  }

  /** Our list of registered spiders. */
  private[this] var _spiders = List[SpiderState]()

  /** An environment that executes jobs one after another. */
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
