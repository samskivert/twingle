//
// $Id$

package com.twingle.persist

import java.util.UUID

/**
 * Provides access to the Twingle database.
 */
trait Database
{
  /**
   * Loads the object with the specified id into an instance of the supplied DatabaseObject derived
   * class.
   */
  def load[C <: DatabaseObject] (oclass :Class[C], id :UUID) :Option[C]

  /**
   * Stores the supplied object in the database. Returns the newly assigned UUID for the object.
   */
  def store (obj :DatabaseObject) :UUID

  /**
   * Searches the database and returns all objects that "match" the supplied query.
   */
  def find (query :String) :List[DatabaseObject]
}
