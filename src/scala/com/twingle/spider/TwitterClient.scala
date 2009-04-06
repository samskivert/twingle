package com.twingle.spider

import scala.xml.{Node, XML}

import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.cookie.CookiePolicy
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.{HttpClient, HttpMethod, HttpMethodBase, UsernamePasswordCredentials}

import com.twingle.Log.log

class TwitterClient {
  def friendsTimeline (username :String, password :String) :Node = {
    val url = "http://twitter.com/statuses/friends_timeline.xml"
    val (method, result, responseBody) = httpGet(url, username, password)
    if (result != 200) {
      log.warning("Error fetching friends timeline", "result", ""+result,
                  "responseBody", responseBody)
      return null
    }

    XML.loadString(responseBody)
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

}
