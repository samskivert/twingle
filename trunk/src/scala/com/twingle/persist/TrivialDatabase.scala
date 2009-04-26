//
// $Id$

package com.twingle.persist

import java.util.UUID

/**
 * A trivial, in-memory implementation of {@link Database}.
 */
class TrivialDatabase extends AnyRef with Database
{
  // from Database
  def load[C <: DatabaseObject] (oclass :Class[C], id :UUID) :Option[C] = _db.get(id) match {
    case None => None
    case Some(obj) => Some(oclass.cast(obj))
  }

  // from Database
  def store (obj :DatabaseObject) {
    _db.put(obj.id, obj)
  }

  // from Database
  def find (query :String) :Iterator[DatabaseObject] = {
    _db.values.filter(obj => matches(obj, query))
  }

  // from Database
  def find (id :UUID) :Iterator[DatabaseObject] = {
    _db.values.filter(obj => matches(obj, id))
  }

  protected def matches (obj :DatabaseObject, query :String) = {
    val meta = DatabaseObject.meta(obj)
    val tquery = new TrivialDatabase.Query(query)
    meta.stringAttrs.exists(m => m.invoke(obj) match {
      case text :String => tquery.matches(text)
      case None => false
      case Some(text) => tquery.matches(text.asInstanceOf[String])
      case tlist :List[_] => tlist.exists(text => tquery.matches(text.asInstanceOf[String]))
      case _ => false
    })
  }

  protected def matches (obj :DatabaseObject, qid :UUID) = {
    val meta = DatabaseObject.meta(obj)
    meta.uuidAttrs.exists(m => m.invoke(obj) match {
      case id :UUID => id == qid
      case None => false
      case Some(id) => id.asInstanceOf[UUID] == qid
      case tlist :List[_] => tlist.exists(id => id.asInstanceOf[UUID] == qid)
      case _ => false
    })
  }

  protected val _db = scala.collection.mutable.Map[UUID, DatabaseObject]()
}

object TrivialDatabase
{
  class Query (query :String) {
    def matches (text :String) = _tokens.exists(tok => (text.indexOf(tok) != -1))
    private[this] val _tokens = query.split("\\s")
  }
}
