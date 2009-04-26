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
   * Stores the supplied object in the database.
   */
  def store (obj :DatabaseObject) :Unit

  /**
   * Searches the database and returns all objects that "match" the supplied query.
   */
  def find (query :String) :Iterator[DatabaseObject]

  /**
   * Searches the database and returns all objects that contain the supplied UUID in any member.
   */
  def find (id :UUID) :Iterator[DatabaseObject]

//   /**
//    * Creates a new database object with the supplied data. A UUID will be assigned to the newly
//    * created object.
//    */
//   def make (data :Map[String, String]) :DatabaseObject
}
