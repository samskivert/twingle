//
// $Id$

package com.twingle.persist

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.util.UUID

import scala.collection.mutable.ListBuffer

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
      exec(stmt => {
        stmt.executeUpdate("create table " + ObjectTable + " " +
                           "(uuid varchar(64) not null," +
                           " field varchar(64) not null," +
                           " value clob(128M) not null," +
                           " primary key (uuid, field))")
      })
    }
  }

  protected def getTableNames () :List[String] =
    queryMap(_.getMetaData().getTables(null, null, null, null),
             _.getString("TABLE_NAME").toLowerCase)

  /** Creates a statement, executes a block, commits the connection and closes the statement. */
  protected def exec (op :Statement => Unit) {
    val stmt = _conn.createStatement
    try {
      op(stmt)
      _conn.commit()
    } finally {
      stmt.close
    }
  }

  /** Executes the supplied query then executes the supplied action on the result set. */
  protected def query[T] (query :Connection => ResultSet, action :ResultSet => T) :T = {
    val rs = query(_conn)
    try {
      action(rs)
    } finally {
      rs.close
    }
  }

  /** Executes the supplied query, maps each row using f and returns a list of the results. */
  protected def queryMap[T] (query :Connection => ResultSet, f :ResultSet => T) :List[T] = {
    val rs = query(_conn)
    try {
      val lbuf = new ListBuffer[T]
      while (rs.next()) lbuf += f(rs)
      lbuf.toList
    } finally {
      rs.close
    }
  }

  protected def driverClass :String
  protected def databaseURL (name :String) :String
}
