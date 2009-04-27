//
// $Id$

package com.twingle.fulltext

import java.util.UUID

import com.twingle.persist.DatabaseObject

/**
 * Interface to be implemented by specific full text indexing services that provide full text
 * search facilities of Twingle {@link DatabaseObject} records.
 */
trait FullTextService
{
  /** Performs a full text index of the supplied {@link DatabaseObject}. */
  def index (obj :DatabaseObject) :Unit

  /**
   * Performs a full text search for the supplied query terms and returns any matching
   * twingle document ids.
   */
  def search (query :String) :Iterator[UUID]
}
