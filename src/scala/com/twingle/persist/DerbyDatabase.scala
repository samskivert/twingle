//
// $Id$

package com.twingle.persist

import java.sql.DriverManager

/**
 * Provides a database implementation backed by the Derby Java SQL database.
 */
class DerbyDatabase extends JDBCDatabase
{
  // from Database
  override def shutdown () {
    super.shutdown()
    // this is how derby is shutdown, krazy!
    DriverManager.getConnection("jdbc:derby:;shutdown=true")
  }

  protected def driverClass = "org.apache.derby.jdbc.EmbeddedDriver"
  protected def databaseURL (name :String) = "jdbc:derby:" + name + ";create=true"
}
