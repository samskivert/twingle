//
// $Id$

package com.twingle.daemon

import java.io.PrintStream
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

/**
 * Quick hack to grab a query and spit out results.
 */
class HackServlet (env :Env) extends HttpServlet
{
  override protected def doGet (req :HttpServletRequest, rsp :HttpServletResponse) {
    val query = req.getPathInfo
    val out = new PrintStream(rsp.getOutputStream())
    if (query != null && query.length > 0) {
      out.println("Query: '" + query + "'")
      out.println("Results:")
      env.db.find(query.substring(1)).foreach(obj => out.println(obj))
    } else {
      out.println("Hack! Tack a query onto the path.")
    }
    out.close()
  }
}
