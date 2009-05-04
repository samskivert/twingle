//
// $Id$

package com.twingle.persist

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
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
    query("select field, value from " + ObjectTable + " where uuid = ?",
          stmt => { stmt.setString(1, id.toString) },
          rs => (Symbol(rs.getString(1)) -> rs.getString(2))) match {
      case Nil => None
      case attrs => {
        val obj = oclass.newInstance.asInstanceOf[C]
        obj.init(Map() ++ attrs + ('id -> id.toString))
        Some(obj)
      }
    }
  }

  // from Database
  def store (obj :DatabaseObject) {
    // TODO: make this all happen in the same transaction
    exec("delete from " + ObjectTable + " where uuid = ?",
         stmt => { stmt.setString(1, obj.id.toString) })
    for ((key, value) <- obj.attrs) {
      exec("insert into " + ObjectTable + " (uuid, field, value) values(?, ?, ?)",
           stmt => {
             stmt.setString(1, obj.id.toString)
             stmt.setString(2, key.name)
             stmt.setString(3, value)
           });
    }
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
      exec("create table " + ObjectTable + " " +
           "(uuid varchar(64) not null," +
           " field varchar(64) not null," +
           " value clob(128M) not null," +
           " primary key (uuid, field))")
    }
  }

  protected def getTableNames () :List[String] =
    query(_.getMetaData().getTables(null, null, null, null), _.getString("TABLE_NAME").toLowerCase)

  protected def exec (stmt :String) :Int = {
    transact(conn => {
      val sstmt = conn.createStatement
      try {
        sstmt.executeUpdate(stmt)
      } finally {
        sstmt.close
      }
    })
  }

  protected def exec (stmt :String, bind :PreparedStatement => Unit) :Int = {
    transact(conn => {
      val pstmt = conn.prepareStatement(stmt)
      try {
        bind(pstmt)
        pstmt.executeUpdate
      } finally {
        pstmt.close
      }
    })
  }

  protected def query[T] (query :Connection => ResultSet, f :ResultSet => T) :List[T] = {
    transact(conn => {
      val rs = query(conn)
      try {
        val lbuf = new ListBuffer[T]
        while (rs.next()) lbuf += f(rs)
        lbuf.toList
      } finally {
        rs.close
      }
    })
  }

  protected def query[T] (query :String, bind :PreparedStatement => Unit,
                          f :ResultSet => T) :List[T] = {
    transact(conn => {
      val stmt = conn.prepareStatement(query)
      try {
        bind(stmt)
        val rs = stmt.executeQuery
        try {
          val lbuf = new ListBuffer[T]
          while (rs.next()) lbuf += f(rs)
          lbuf.toList
        } finally {
          rs.close
        }
      } finally {
        stmt.close
      }
    })
  }

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
