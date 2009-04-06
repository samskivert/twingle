package com.twingle.spider

import java.text.SimpleDateFormat
import java.util.Date

import scala.xml.{Node, XML}

import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.cookie.CookiePolicy
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.{HttpClient, HttpMethod, HttpMethodBase,
                                      UsernamePasswordCredentials}

import com.twingle.Log.log

class TwitterClient {
  /**
   * Simple status record to contain a tweet.
   */
  class Status {
    var createdAt :Date = null
    var tweetId :Int = 0
    var text :String = null
    var userId :Int = 0
    var userName :String = null
    var screenName :String = null

    override def toString () = {
      val buf :StringBuffer = new StringBuffer
      buf.append("[createdAt=").append(createdAt)
      buf.append(", tweetId=").append(tweetId)
      buf.append(", text=").append(text)
      buf.append(", userId=").append(userId)
      buf.append(", userName=").append(userName)
      buf.append(", screenName=").append(screenName)
      buf.toString
    }
  }

  def friendsTimeline (username :String, password :String) :Seq[Status] = {
    val url = "http://twitter.com/statuses/friends_timeline.xml"
    val (method, result, responseBody) = httpGet(url, username, password)
    if (result != 200) {
      log.warning("Error fetching friends timeline", "result", ""+result,
                  "responseBody", responseBody)
      return null
    }

    val doc = XML.loadString(responseBody)
    if (doc == null) {
      return null
    }
    
    val statuses = (doc \\ "status")
    statuses.map(parseStatusElement(_))
  }

  protected def httpGet (url :String, username :String, password :String) = {
    val httpClient = new HttpClient

    val authScope = new AuthScope("twitter.com", 80, AuthScope.ANY_REALM)
    val creds = new UsernamePasswordCredentials(username, password)
    httpClient.getState.setCredentials(authScope, creds)
    httpClient.getParams.setAuthenticationPreemptive(true)

    val method = new GetMethod(url)
    method.getParams.setCookiePolicy(CookiePolicy.IGNORE_COOKIES)
    val result = httpClient.executeMethod(method)
    val responseBody = method.getResponseBodyAsString
    method.releaseConnection

    (method, result, responseBody)
  }

  protected def parseStatusElement (e :Node) :Status = {
    val status = new Status

    status.createdAt =
      TwitterClient.dateFormatter.parse((e \ "created_at").text)
    status.tweetId = (e \ "id").text.toInt
    status.text = (e \ "text").text

    val userElem = (e \ "user")
    status.userId = (userElem \ "id").text.toInt
    status.userName = (userElem \ "name").text
    status.screenName = (userElem \ "screen_name").text

    status
  }
}

object TwitterClient {
  val dateFormatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy")
}
