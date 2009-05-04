//
// $Id$

package com.twingle.persist

import java.sql.DriverManager
import java.util.UUID

/**
 * Provides a database implementation backed by a SQL database accessed via JDBC.
 */
abstract class JDBCDatabase extends AnyRef with Database
{
  protected val DatabaseName = "twingledb"
  protected val ObjectTable = "objects"

  // resolve the driver which makes it visible to JDBC
  Class.forName(driverClass)
  // create a database connection
  val _conn = DriverManager.getConnection(databaseURL(DatabaseName))
  // we'll manage commits manually
  _conn.setAutoCommit(false)

  // if our objects table does not exist, create it
  createObjectTable()

  // from Database
  def load[C <: DatabaseObject] (oclass :Class[C], id :UUID) :Option[C] = {
    None // TODO
  }

  // from Database
  def store (obj :DatabaseObject) {
    // TODO
  }

  // from Database
  def find (query :String) :Iterator[DatabaseObject] = {
    Iterator.empty // TODO
  }

  // from Database
  def find (id :UUID) :Iterator[DatabaseObject] = {
    Iterator.empty // TODO
  }

  // from Database
  def shutdown () {
    // close the connection we opened in our constructor
    _conn.close()
  }

  protected def createObjectTable () {
    if (!getTableNames.contains(ObjectTable)) {
      println("Creating '" + ObjectTable + "' table...")
      val stmt = _conn.createStatement
      try {
        stmt.executeUpdate("create table " + ObjectTable + " " +
                           "(uuid varchar(64) not null," +
                           " field varchar(64) not null," +
                           " value clob(128M) not null," +
                           " primary key (uuid, field))")
        _conn.commit()
      } finally {
        stmt.close
      }
    }
  }

  protected def getTableNames () = {
    val gtrs = _conn.getMetaData().getTables(null, null, ObjectTable, null)
    try {
      def foldNames () :List[String] =
        if (gtrs.next()) gtrs.getString("TABLE_NAME") :: foldNames() else Nil
      foldNames()
    } finally {
      gtrs.close()
    }
  }

  protected def driverClass :String
  protected def databaseURL (name :String) :String
}
