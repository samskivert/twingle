//
// $Id$

package com.twingle.daemon

import org.mortbay.jetty.Server
import org.mortbay.jetty.handler.{DefaultHandler, ResourceHandler, ContextHandler}
import org.mortbay.jetty.handler.{HandlerCollection, ContextHandlerCollection}
import org.mortbay.jetty.nio.SelectChannelConnector
import org.mortbay.jetty.servlet.{Context, ServletHolder}

/**
 * Handles the serving of HTTP requests in the Twingle daemon.
 */
class HttpServer (env :Env, port :Int)
{
  def start () {
    _server.start()
  }

  private[this] val _server = new Server

  /* HttpServer */ {
    // create and initialize our jetty server instance
    val conn = new SelectChannelConnector
    conn.setPort(port)
    _server.setConnectors(Array(conn))
    val contexts = new ContextHandlerCollection
    val context = new Context(contexts, "/", Context.NO_SESSIONS)
    val handlers = new HandlerCollection
    handlers.addHandler(contexts)
    _server.setHandler(handlers)

    // wire up our various servlets
    context.addServlet(new ServletHolder(new HelloServlet(env)), "/hello")
  }
}
