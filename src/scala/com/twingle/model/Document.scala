//
// $Id$

package com.twingle.model

import java.nio.ByteBuffer
import java.util.Date

import com.twingle.persist.DatabaseObject

/**
 * Contains data for a document stored in the database.
 */
class Document extends AnyRef with DatabaseObject
{
  /** The URL defining this document's location or "<unknown>". */
  def location () :String = optA(stringM, "location") | "<unknown>"

  /** The name of this document or "<unknown>". */
  def name () :String = optA(stringM, "name") | "<unknown>"

  /** The text of this document. */
  def text () :Option[String] = optA(stringM, "text").data

  /** This document's binary data. */
  def bits () :Option[ByteBuffer] = optA(byteBufferM, "bits").data

  /** The date on which this document was created. */
  def created () :Option[Date] = optA(dateM, "created").data

  /** The date on which this document was last modified. */
  def lastModified () :Option[Date] = optA(dateM, "last_modified").data
}

object Document
{
//   def make (location :Option[String], name :Option[String], text :Option[String],
//             bits :Option[ByteBuffer], created :Option[Date], lastModified :Option[Date]) :Document
//   {
//   }
}
