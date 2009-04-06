//
// $Id$

package com.twingle.persist

import java.util.UUID

import scala.collection.mutable.Map

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
  def find (query :String) :List[DatabaseObject] = {
    Nil // TODO
  }

//   // from Database
//   def make (data :Map[String, String]) :DatabaseObject = {
//   }

  protected val _db = Map[UUID, DatabaseObject]()
}
