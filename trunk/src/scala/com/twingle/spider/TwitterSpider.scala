//
// $Id$

package com.twingle.spider

import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date

import scala.xml.{Node, XML}

import com.sleepycat.db.{BtreeStats, Database, DatabaseConfig, DatabaseEntry, DatabaseException,
                         DatabaseType, OperationStatus}
import com.sleepycat.bind.tuple.{TupleBinding, TupleInput, TupleOutput}

import com.twingle.Log.log
import com.twingle.daemon.{Env, Job}
import com.twingle.model.Document

class TwitterSpider (val urlFetcher :URLFetcher) extends Spider
{
  import TwitterSpider._

  def getFriendsTimeline (config :Config) :Seq[Tweet] = {
    // submit request for specified user's latest friend status timeline
    val url = "http://twitter.com/statuses/friends_timeline.xml"
    val rsp = urlFetcher.getAuthedUrl(url, "twitter.com", config.username, config.password)

    // turn the response into an xml document
    val doc = XML.loadString(rsp.body)
    if (doc == null) {
      return null
    }

    // parse out each user status entry
    val statusElements = (doc \\ "status")
    statusElements.map(parseStatusElement(_))
  }

  protected def parseStatusElement (e :Node) :Tweet = {
    val createdAt = _dateFormat.parse((e \ "created_at").text)
    val tweetId = (e \ "id").text.toInt
    val text = (e \ "text").text

    val userElem = (e \ "user")
    val userId = (userElem \ "id").text.toInt
    val userName = (userElem \ "name").text
    val screenName = (userElem \ "screen_name").text

    Tweet(tweetId, createdAt, userId, userName, screenName, text)
  }

  private[this] val _dateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy")
}

/**
 * Utility methods for TwitterSpider.
 */
object TwitterSpider
{
  /** Contains configuration information for the Twitter spider. */
  class Config extends Spider.Config {
    /** The Twitter account's username. */
    def username () :String = reqA(stringM, "username").data

    /** The Twitter account's password. TODO: can we encyrpt this? */
    def password () :String = reqA(stringM, "password").data

    def createJob () = new Job() {
      def run (env :Env) {
        val spider = new TwitterSpider(new URLFetcher)
        spider.getFriendsTimeline(Config.this).foreach(t => env.db.store(toDocument(t)))
      }

      def toDocument (t :Tweet) = Document.builder.name(t.text).created(t.createdAt).build
    }
  }

  /** Simple status record to contain a tweet. */
  case class Tweet (val tweetId :Int,
                    val createdAt :Date,
                    val userId :Int,
                    val userName :String,
                    val screenName :String,
                    val text :String)

  /** Persistence conversion for storage in BDB. */
  class TweetTupleBinding extends TupleBinding {
    def objectToEntry (obj :Object, to :TupleOutput) {
      val t :Tweet = obj.asInstanceOf[Tweet]

      to.writeInt(t.tweetId)
      to.writeLong(t.createdAt.getTime())
      to.writeInt(t.userId)
      to.writeString(t.userName)
      to.writeString(t.screenName)
      to.writeString(t.text)
    }

    def entryToObject (ti :TupleInput) :Object = {
      val tweetId = ti.readInt
      val createdAt = new Date(ti.readLong)
      val userId = ti.readInt
      val userName = ti.readString
      val screenName = ti.readString
      val text = ti.readString

      Tweet(tweetId, createdAt, userId, userName, screenName, text)
    }
  }

  def configBuilder () = new Spider.ConfigBuilder {
    def username (username :String) = { add("username", username); this }
    def password (password :String) = { add("password", password); this }
    def build :TwitterSpider.Config = build(new TwitterSpider.Config)
  }
}

/** Simple demo app to output all tweets stored in the twitter database. */
object TwitterSpiderShowApp
{
  def main (args :Array[String]) {
    // save each tweet keyed on the username
    try {
      // create the database interface object
      val dbConfig :DatabaseConfig = new DatabaseConfig
      val twitterDb :Database = new Database("/tmp/twitter.db", null, dbConfig)

      val binding = new TwitterSpider.TweetTupleBinding

      // normally we'd be looking for a tweet with a particular id or username, but for this test
      // app we want to iterate over all known keys and display them, so we get a total record
      // count to start off
      val recordCount :Int = twitterDb.getStats(null, null).asInstanceOf[BtreeStats].getNumData
      log.info("Reading tweets.", "count", ""+recordCount)

      // print out each record, looking up by record number
      for (i <- 1 until recordCount) {
        // set the record number to look up
        val dbKey :DatabaseEntry = new DatabaseEntry
        dbKey.setRecordNumber(i)

        // create a new database entry to hold the record data
        val dbData = new DatabaseEntry

        // look up the record
        val status :OperationStatus = twitterDb.getSearchRecordNumber(null, dbKey, dbData, null)
        if (status != OperationStatus.SUCCESS) {
          log.warning("Failed to fetch record.", "no", ""+i)

        } else {
          // display the record
          val tweet = binding.entryToObject(dbData)
          log.info("Fetched record.", "no", ""+i, "tweet", tweet)
        }
      }

      // close the database
      twitterDb.close

      log.info("Read all tweets.")

    } catch {
      case de :DatabaseException => log.warning("Database error.", de)
      case fnfe: FileNotFoundException => log.warning("No such file.", fnfe)
    }
  }
}

object TwitterSpiderApp
{
  def main (args :Array[String]) {
    // read command-line arguments
    if (args.length < 2) {
      log.warning("No username and password specified.")
      exit
    }
    val username = args(0)
    val password = args(1)

    // construct the user list to be queried
    val config = TwitterSpider.configBuilder.
                   enabled(true).runEvery(60).username(username).password(password).build

    // query twitter for the latest statuses
    val spider = new TwitterSpider(new URLFetcher)
    val tweets = spider.getFriendsTimeline(config)
    // tweets.foreach(log.info(_))

    // save each tweet keyed on the username
    try {
      // create the database interface object
      val dbConfig :DatabaseConfig = new DatabaseConfig
      dbConfig.setAllowCreate(true)
      dbConfig.setBtreeRecordNumbers(true)
      dbConfig.setType(DatabaseType.BTREE)
      val twitterDb :Database = new Database("/tmp/twitter.db", null, dbConfig)

      // write each tweet to the database
      val binding = new TwitterSpider.TweetTupleBinding

      log.info("Storing tweets.")

      // we're just testing for now, so we just write tweets out keyed on the author name which is
      // silly as one author could have multiple tweets and we should probably use the tweet id as
      // the key instead, but we're just exploring the bdb api right now anyway.
      tweets.foreach(t => {
        // build the key from the twitter user screen name
        val dbKey :DatabaseEntry = new DatabaseEntry(t.screenName.getBytes("UTF-8"))

        // use our custom TupleBinding implementation to serialize the record
        val dbData :DatabaseEntry = new DatabaseEntry
        binding.objectToEntry(t, dbData)

        // store the record
        twitterDb.put(null, dbKey, dbData)
      })

      // close the database
      twitterDb.close

      log.info("Stored tweets.")

    } catch {
      case de :DatabaseException => log.warning("Database error.", de)
      case fnfe: FileNotFoundException => log.warning("No such file.", fnfe)
    }
  }
}
