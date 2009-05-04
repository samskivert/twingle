//
// $Id$

package com.twingle.persist

import java.sql.DriverManager
import java.sql.SQLException

import com.twingle.Log.log

/**
 * Provides a database implementation backed by the Derby Java SQL database.
 */
class DerbyDatabase extends JDBCDatabase
{
  // from Database
  override def shutdown () {
    super.shutdown()
    // this is how derby is shutdown, krazy!
    try {
      DriverManager.getConnection("jdbc:derby:;shutdown=true")
    } catch {
      case se :SQLException => 
        if (se.getErrorCode() == 50000 && se.getSQLState() == "XJ015") {
          // everything's AOK
        } else {
          log.warning("Failed to shutdown Derby", se)
        }
    }
  }

  protected def driverClass = "org.apache.derby.jdbc.EmbeddedDriver"
  protected def databaseURL (name :String) = "jdbc:derby:" + name + ";create=true"
}
