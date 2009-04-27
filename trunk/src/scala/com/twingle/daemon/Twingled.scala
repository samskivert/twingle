//
// $Id$

package com.twingle.daemon

import com.twingle.Log.log
import com.twingle.persist.Database
import com.twingle.persist.TrivialDatabase

/**
 * The main entry point for the Twingle daemon.
 */
object Twingled
{
  def main (args :Array[String]) {
    val db = new TrivialDatabase // TODO

    val exec = new JobExecutor(db)
    exec.start()

    val http = new HttpServer(8080)
    http.start()

    log.info("Twingle daemon running.")
  }
}
