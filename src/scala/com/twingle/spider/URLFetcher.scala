package com.twingle.spider

import com.twingle.Log.log

import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.cookie.CookiePolicy
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.{HttpClient, HttpMethod, HttpMethodBase,
                                      UsernamePasswordCredentials}

class URLFetcher {
  /** Simple response record to report on http request results. */ 
  class Response (val method :HttpMethod, val resultCode :Int, val body :String)

  /** Request a url via HTTP GET with http user authentication. */
  def getAuthedUrl (url :String, host :String, username :String, password :String) :Response = {
    log.debug("Fetching via GET with auth", "url", url)

    // start by resetting the client http state
    _client.getState.clear()

    // prepare our credentials and authentication
    val port = 80
    val authScope = new AuthScope(host, port, AuthScope.ANY_REALM)
    val creds = new UsernamePasswordCredentials(username, password)
    _client.getState.setCredentials(authScope, creds)
    _client.getParams.setAuthenticationPreemptive(true)

    // prepare the method with which we make the request
    val method = new GetMethod(url)
    method.getParams.setCookiePolicy(CookiePolicy.IGNORE_COOKIES)

    // submit the http request
    val resultCode = _client.executeMethod(method)
    val body = method.getResponseBodyAsString
    method.releaseConnection

    // build our response record
    new Response(method, resultCode, body)
  }

  /** Request a url via HTTP GET. */
  def getUrl (url :String) :Response = {
    log.debug("Fetching via GET", "url", url)

    // start by resetting the client http state
    _client.getState.clear()

    // prepare the method with which we make the request
    val method = new GetMethod(url)
    val resultCode = _client.executeMethod(method)
    val body = method.getResponseBodyAsString
    method.releaseConnection

    // build our response record
    new Response(method, resultCode, body)
  }
  
  private[this] val _client = new HttpClient
}
