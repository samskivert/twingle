//
// $Id$

package com.twingle.daemon

import java.io.PrintStream
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

/**
 * Simple hello world servlet.
 */
class HelloServlet (env :Env) extends HttpServlet
{
  override protected def doGet (req :HttpServletRequest, rsp :HttpServletResponse) {
    val out = new PrintStream(rsp.getOutputStream())
    out.println("Hello world!")
    out.close()
  }
}
