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
    transact(conn => {
      val stmt = conn.createStatement
      try {
        op(stmt)
      } finally {
        stmt.close
      }
    })
  }

  /** Executes the supplied query then executes the supplied action on the result set. */
  protected def query[T] (q :Connection => ResultSet, action :ResultSet => T) :T = {
    transact(conn => {
      val rs = q(conn)
      try {
        action(rs)
      } finally {
        rs.close
      }
    })
  }

  /** Executes the supplied query, maps each row using f and returns a list of the results. */
  protected def queryMap[T] (q :Connection => ResultSet, f :ResultSet => T) :List[T] = {
    query(q, rs => {
      val lbuf = new ListBuffer[T]
      while (rs.next()) lbuf += f(rs)
      lbuf.toList
    })
  }

  /** Executes the supplied block in a transction.
   *  Commits if the block completes, rolls back if an exception is thrown. */
  protected def transact[T] (op :Connection => T) :T = {
    try {
      val result = op(_conn)
      _conn.commit()
      result
    } catch {
      case ex :Exception => {
        _conn.rollback()
        throw ex
      }
    }
  }

  protected def driverClass :String
  protected def databaseURL (name :String) :String
}
