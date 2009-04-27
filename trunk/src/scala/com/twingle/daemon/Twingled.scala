//
// $Id$

package com.twingle.daemon

import java.util.Date

import com.twingle.Log.log
import com.twingle.persist.Database
import com.twingle.persist.TrivialDatabase

import com.twingle.model._ // TEMP

/**
 * The main entry point for the Twingle daemon.
 */
object Twingled
{
  def main (args :Array[String]) {
    val db = new TrivialDatabase // TODO

    val exec = new JobExecutor(db)
    exec.start()

    // temp hackity
    tempPopulateData(db)
    // end temp

    val http = new HttpServer(exec.env, 8080)
    http.start()

    log.info("Twingle daemon running.")
  }

  protected def tempPopulateData (db :Database) {
    val now = new Date
    val mdb = Person.builder.
      name("Michael Bayne").
      twitter("samskivert").
      build
    val shaper = Person.builder.
      name("Walter Korman").
      twitter("shaper").
      build
    val tweet = Message.builder.
      location("twitter://1231232").
      name("Tweet tweet").
      text("@shaper Lulz!").
      created(now).
      lastModified(now).
      author(mdb.id).
      recipients(List(shaper.id)).
      build
    db.store(mdb)
    db.store(shaper)
    db.store(tweet)
  }
}
