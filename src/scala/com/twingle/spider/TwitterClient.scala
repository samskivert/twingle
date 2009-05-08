package com.twingle.spider

import java.lang.NumberFormatException
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import scala.xml.{Node, NodeSeq, XML}

import com.twingle.Log.log

/**
 * Aims to provide a thorough Scala-oriented client interface to the Twitter API.
 *
 * http://apiwiki.twitter.com/Twitter-API-Documentation
 *
 * TODO:
 * - OAuth
 * - gzip
 */
class TwitterClient (val urlFetcher :URLFetcher)
{
  import com.twingle.spider.TwitterClient._

  // Search methods

//   def search // json, atom
//   def getTrends // json
//   def getCurrentTrends // json
//   def getDailyTrends // json
//   def getWeeklyTrends // json

  // Timeline methods

  /**
   * Fetch the most recent {@link Tweet} records from the public Twitter timeline.
   */
  def getPublicTimeline :Seq[Tweet] = 
    parseTweets(urlFetcher.getUrl(twitterUrl("public_timeline.xml")))

 /**
  * Fetch tweets made by a specific user's friends. Requires authentication as the user.
  *
  * @param username the user's Twitter username.
  * @param password the user's Twitter password.
  * @param sinceId optional tweet id after which (more-recent-than) tweets are sought.
  * @param maxId optional tweet id before which (older-than) tweets are sought.
  * @param count optional number of tweets to retrieve, maximum 200.
  * @param page optional page of results to retrieve. 
  */
  def getFriendsTimeline (username :String, password :String, 
                          sinceId :Option[Int],
                          maxId   :Option[Int],
                          count   :Option[Int], 
                          page    :Option[Int]) :Seq[Tweet] = {
    val qpb = new QueryParamBuilder
    qpb.add("since_id", sinceId)
    qpb.add("max_id", maxId)
    qpb.add("count", count)
    qpb.add("page", page)
    val url = twitterUrl("friends_timeline.xml") + '?' + qpb.build

    parseTweets(urlFetcher.getAuthedUrl(url, TWITTER_DOMAIN, username, password))
  }

  /** 
   * Fetch the tweet timeline for a user whose tweets are protected.
   *
   * @param username the user's Twitter username.
   * @param password the user's Twitter password.
   * @param sinceId optional tweet id after which (more-recent-than) tweets are sought.
   * @param maxId optional tweet id before which (older-than) tweets are sought.
   * @param count optional number of tweets to retrieve, maximum 200.
   * @param page optional page of results to retrieve. 
   */
  def getUserTimeline (username :String, password :String, 
                       sinceId :Option[Int], 
                       maxId   :Option[Int],
                       count   :Option[Int], 
                       page    :Option[Int]) :Seq[Tweet] = 
    muxUserTimeline(Some(username), Some(password), None, None, sinceId, maxId, count, page)

  /** 
   * Fetch a public user's tweet timeline, wherein the user is specified by their screen
   * name.
   *
   * @param screenName the user's Twitter username.
   * @param sinceId optional tweet id after which (more-recent-than) tweets are sought.
   * @param maxId optional tweet id before which (older-than) tweets are sought.
   * @param count optional number of tweets to retrieve, maximum 200.
   * @param page optional page of results to retrieve. 
   */
  def getUserTimelineByScreenName (screenName :String, 
                                   sinceId :Option[Int], 
                                   maxId   :Option[Int], 
                                   count   :Option[Int], 
                                   page    :Option[Int]) :Seq[Tweet] =
    muxUserTimeline(None, None, Some(screenName), None, sinceId, maxId, count, page)

  /** 
   * Fetch a public user's tweet timeline, wherein the user is specified by their Twitter
   * unique user id.
   *
   * @param userId the user's Twitter unique user id.
   * @param sinceId optional tweet id after which (more-recent-than) tweets are sought.
   * @param maxId optional tweet id before which (older-than) tweets are sought.
   * @param count optional number of tweets to retrieve, maximum 200.
   * @param page optional page of results to retrieve. 
   */
  /** Fetch a public user's tweet timeline by user id. */
  def getUserTimelineById (userId :Int,
                           sinceId :Option[Int],
                           maxId   :Option[Int], 
                           count   :Option[Int],
                           page    :Option[Int]) :Seq[Tweet] = 
    muxUserTimeline(None, None, None, Some(userId), sinceId, maxId, count, page)

  /**
   * Internal routine to pull a user's tweet timeline using one of various available means
   * to specify the user: one of username/password, screen name, or Twitter unique user
   * id.  For sanity's sake we expose this functionality through more clearly named
   * specifier-specific methods, all of which route back to here to actually retrieve from
   * Twitter.
   */
  protected def muxUserTimeline (username   :Option[String], 
                                 password   :Option[String], 
                                 screenName :Option[String], 
                                 userId     :Option[Int],
                                 sinceId    :Option[Int],
                                 maxId      :Option[Int],
                                 count      :Option[Int], 
                                 page       :Option[Int]) :Seq[Tweet] = {
    val qpb = new QueryParamBuilder
    qpb.add("user_id", userId)
    qpb.add("screen_name", screenName)
    qpb.add("since_id", sinceId)
    qpb.add("max_id", maxId)
    qpb.add("count", count)
    qpb.add("page", page)
    val url = twitterUrl("user_timeline.xml") + '?' + qpb.build

    parseTweets(getMaybeAuthedUrl(username, password, url))
  }

  /**
   * Fetch mentions of a particular user, e.g., recent tweets containing "@username".  
   * Requires authentication.
   */
  def getMentions (username :String, password :String, 
                   sinceId :Option[Int], 
                   maxId   :Option[Int], 
                   count   :Option[Int], 
                   page    :Option[Int]) :Seq[Tweet] = {
    val qpb = new QueryParamBuilder
    qpb.add("since_id", sinceId)
    qpb.add("max_id", maxId)
    qpb.add("count", count)
    qpb.add("page", page)
    val url = twitterUrl("mentions.xml") + '?' + qpb.build

    parseTweets(urlFetcher.getAuthedUrl(url, TWITTER_DOMAIN, username, password))
  }

  // Status methods

  /** 
   * Returns details on a specific tweet.  Authentication only required if the tweeting
   * user's feed is protected.
   */
  def getShowStatus (username :Option[String], password :Option[String], 
                     tweetId :Int) :Tweet = {
    val url = twitterUrl("show/" + tweetId + ".xml")
    parseTweets(getMaybeAuthedUrl(username, password, url)).first
  }

//   def updateStatus
//   def destroyStatus

  // User methods

//   def getUser 
//   def getFriends
//   def getFollowers

  // Direct Message methods

//   def getDirectMessages
//   def getSentDirectMessages
//   def newDirectMessage
//   def destroyDirectMessage

  // Friendship methods

//   def createFriendship
//   def destroyFriendship
//   def friendshipExists

  // Social Graph methods

//   def getFriendIds
//   def getFollowerIds

  // Account methods

//   def verifyCredentials
//   def getRateLimitStatus
//   def getRateLimitStatus
//   def endSession
//   def updateDeliveryDevice
//   def updateProfileColors
//   def updateProfileImage
//   def updateProfileBackgroundImage
//   def updateProfile

  // Favorite methods

//   def getFavorites
//   def createFavorite
//   def destroyFavorite

  // Notification methods

//   def followUserNotification
//   def leaveUserNotification

  // Block methods

//   def createBlock
//   def destroyBlock

  /** Returns the full url to the specified Twitter API service. */
  protected def twitterUrl (service :String) = "http://twitter.com/statuses/" + service

  /** 
   * Submits an HTTP GET request for the supplied Twitter URL, optionally using 
   * standard HTTP authentication for the supplied user which is necessary to retrieve
   * some data, e.g. private user feeds or a specific user's friend's tweets.
   */
  protected def getMaybeAuthedUrl (
    username :Option[String], password :Option[String], url :String) :URLFetcher.Response =
      if (username.isDefined) {
        urlFetcher.getAuthedUrl(url, TWITTER_DOMAIN, username.get, password.get) 

      } else {
        urlFetcher.getUrl(url)
      }

  /**
   * Standard antics to take the response from a Twitter API service call as XML content
   * and parse it into a sequence of {@link Tweet} records.
   */
  protected def parseTweets (rsp :URLFetcher.Response) :Seq[Tweet] = 
    (XML.loadString(rsp.body) \\ "status").map(Tweet.fromXML(_))
}

object TwitterClient
{
  /** Utility routines for parsing xml elements. */
  trait XMLHandler {
    def xmlBoolean (e :NodeSeq, name :String) = xmlText(e, name).toBoolean

    def xmlDate (e :NodeSeq, name :String, fmt :DateFormat) = fmt.parse(xmlText(e, name))

    def xmlInt (e :NodeSeq, name :String) = xmlText(e, name).toInt

    def xmlOptionInt (e :NodeSeq, name :String) :Option[Int] = 
      try {
        Some(xmlInt(e, name))
      } catch {
        case nfe :NumberFormatException => None
      }

    def xmlText (e :NodeSeq, name :String) = (e \ name).text
  }

  /** Describes a single status update from a {@link User}. */
  case class Tweet (val id :Int,
                    val createdAt :Date,
                    val text :String,
                    val source :String,
                    val truncated :Boolean,
                    val inReplyToTweetId :Option[Int],
                    val inReplyToUserId :Option[Int],
                    val favorited :Boolean,
                    val user :User)

  object Tweet extends XMLHandler {
    /** Construct a {@link Tweet} record by parsing the supplied XML element. */
    def fromXML (e :Node) :Tweet = {
      val createdAt = xmlDate(e, "created_at", _dateFormat)
      val tweetId = xmlInt(e, "id")
      val text = xmlText(e, "text")
      val source = xmlText(e, "source")
      val truncated = xmlBoolean(e, "truncated")
      val inReplyToTweetId = xmlOptionInt(e , "in_reply_to_status_id")
      val inReplyToUserId =  xmlOptionInt(e, "in_reply_to_user_id")
      val favorited =  xmlBoolean(e, "favorited")
      val user = User.fromXML(e \ "user")

      Tweet(tweetId, createdAt, text, source, truncated, inReplyToTweetId, 
            inReplyToUserId, favorited, user)
    }

    private[this] val _dateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy")
  }

  /** Describes a Twitter user info. */
  case class User (val id :Int,
                   val name :String,
                   val screenName :String,
                   val description :String,
                   val location :String,
                   val profileImageUrl :String,
                   val url :String,
                   val isProtected :Boolean,
                   val followersCount :Int)

  object User extends XMLHandler {
    /** Construct a {@link User} record by parsing the supplied XML element. */
    def fromXML (e :NodeSeq) :User = {
      val userId = xmlInt(e, "id")
      val name = xmlText(e, "name")
      val screenName = xmlText(e, "screen_name")
      val description = xmlText(e, "description")
      val location = xmlText(e, "location")
      val profileImageUrl = xmlText(e, "profile_image_url")
      val url = xmlText(e, "url")
      val isProtected = xmlBoolean(e, "protected")
      val followersCount = xmlInt(e, "followers_count")

      User(userId, name, screenName, description, location, 
           profileImageUrl, url, isProtected, followersCount)
    }
  }

  private[this] val TWITTER_DOMAIN :String = "twitter.com"
}

package tests {
  import java.io.{File, FileInputStream, FileNotFoundException, FileReader}
  import java.util.Properties

  import org.apache.commons.io.IOUtils

  import org.scalatest.Suite

  class TwitterClientSuite extends Suite {
    import com.twingle.spider.TwitterClient._

    /** 
     * Provides user-specified configuration settings needed to run
     * the tests, as loaded from a local properties file.
     */
    class TestConfig (val propsPath :String) {
      def privateUsername = _props.getProperty("twitter.private_username")
      def privatePassword = _props.getProperty("twitter.private_password")
      def username = _props.getProperty("twitter.username")
      def userId = _props.getProperty("twitter.user_id").toInt

      val _props :Properties = new Properties
      try {
        _props.load(new FileInputStream(propsPath))
      } catch {
        case fnf :FileNotFoundException =>
          log.warning("No test properties found", "path", propsPath)
      }
    }

    def testPublicTimeline () {
      val tweets = _client.getPublicTimeline

      assert(tweets.length > 10)
      tweets.foreach(validTweet(_))
    }

    def testFriendsTimeline () {
      val tweets = _client.getFriendsTimeline(
        _config.privateUsername, _config.privatePassword, None, None, None, None)

      assert(tweets.length > 0)
      tweets.foreach(validTweet(_))
    }

    def testUserTimeline () {
      val tweets = _client.getUserTimeline(
        _config.privateUsername, _config.privatePassword, None, None, None, None)

      assert(tweets.length > 0)
      tweets.foreach(validTweet(_))
    }

    def testUserTimelineByScreenName () {
      val tweets = _client.getUserTimelineByScreenName(
        _config.username, None, None, None, None)

      assert(tweets.length > 0)
      tweets.foreach(validTweet(_))
    }

    def testUserTimelineById () {
      val tweets = _client.getUserTimelineById(_config.userId, None, None, None, None)

      assert(tweets.length > 0)
      tweets.foreach(validTweet(_))
    }

    def testMentions () {
      val tweets = _client.getMentions(
        _config.privateUsername, _config.privatePassword, None, None, None, None)

      assert(tweets.length > 0)
      tweets.foreach(validTweet(_))
    }

    def testShowStatus () {
      val tweet = _client.getShowStatus(None, None, 123)

      validTweet(tweet)
    }

    protected def validTweet (t :Tweet) {
      assert(t.id > 0)
      assert(t.createdAt.getTime > 0)
      assert(t.text.length > 0)
      assert(t.source.length > 0)
      validUser(t.user)
    }

    protected def validUser (u :User) {
      assert(u.id > 0)
      assert(u.name.length > 0)
      assert(u.screenName.length > 0)
      assert(u.profileImageUrl.startsWith("http://s3.amazonaws.com/twitter_production") ||
             u.profileImageUrl == DEFAULT_PROFILE_IMAGE_URL)
      assert(u.followersCount >= 0)
    }

    protected[this] val DEFAULT_PROFILE_IMAGE_URL = 
      "http://static.twitter.com/images/default_profile_normal.png"

    protected[this] val _config = new TestConfig("tests.properties")
    protected[this] val _client = new TwitterClient(new URLFetcher)
  }
}
