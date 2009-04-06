package com.twingle.spider

import com.twingle.Log.log

import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.cookie.CookiePolicy
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.{HttpClient, HttpMethod, HttpMethodBase,
                                      UsernamePasswordCredentials}

class URLFetcher {
  def getAuthedUrl (url :String, host :String, username :String, password :String) = {
    val port = 80
    val authScope = new AuthScope(host, port, AuthScope.ANY_REALM)
    val creds = new UsernamePasswordCredentials(username, password)
    _client.getState.setCredentials(authScope, creds)
    _client.getParams.setAuthenticationPreemptive(true)

    val method = new GetMethod(url)
    method.getParams.setCookiePolicy(CookiePolicy.IGNORE_COOKIES)
    val result = _client.executeMethod(method)
    val responseBody = method.getResponseBodyAsString
    method.releaseConnection

    (method, result, responseBody)
  }
  
  private[this] val _client = new HttpClient
}
