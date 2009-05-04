//
// $Id$

package com.twingle.daemon

import java.io.FileNotFoundException
import java.io.FileReader
import java.util.Date
import java.util.Properties

import com.twingle.Log.log
import com.twingle.spider.TwitterSpider
import com.twingle.persist.Database
import com.twingle.persist.DerbyDatabase

/**
 * The main entry point for the Twingle daemon.
 */
object Twingled
{
  def main (args :Array[String]) {
    val db = new DerbyDatabase

    // register a hook to shutdown our database when the daemon exits
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run () {
        println("Shutting down...")
        db.shutdown()
      }
    })

    // create our job executor and http server
    val exec = new JobExecutor(db)
    val http = new HttpServer(exec.env, 8080)

    // load up and register our spiders
    val props = new Properties
    try {
      props.load(new FileReader("twingled.properties"))
    } catch {
      case fnf :FileNotFoundException => // no problem
    }
    val spiders = props.getProperty("spiders")
    if (spiders == null) {
      log.warning("No spiders configured. Not doing much.")
    } else {
      spiders.split("\\t").flatMap(id => createSpider(props, id)).foreach(exec.addSpider)
    }

    // start everything up and running
    exec.start()
    http.start()

    log.info("Twingle daemon running.")
  }

  protected def createSpider (props :Properties, id :String) = id match {
    case "twitter" => Some(TwitterSpider.configBuilder.slurp(props, id+".").build)
    case _ => log.warning("Unknown spider type: " + id); None
  }
}
