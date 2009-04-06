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
   * class. Returns null if no object exists with the supplied id.
   */
  def load[C <: DatabaseObject] (oclass :Class[C], id :UUID) :DatabaseObject
}
