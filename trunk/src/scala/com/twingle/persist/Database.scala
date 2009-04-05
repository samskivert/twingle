//
// $Id$

package com.twingle.persist

/**
 * Provides access to the Twingle database.
 */
class Database
{
  /**
   * Loads the object with the specified id into an instance of the supplied
   * DatabaseObject derived class. Returns null if no object exists with the
   * supplied id.
   */
  def load[C >: DatabaseObject] (oclass :Class[C], id :Long) :DatabaseObject = throw new Error()
}