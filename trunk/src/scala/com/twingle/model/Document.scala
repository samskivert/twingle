//
// $Id$

package com.twingle.model

import java.nio.ByteBuffer
import java.util.Date

import com.twingle.persist.DatabaseObject
import com.twingle.persist.OptAttr

/**
 * Contains data for a document stored in the database.
 */
class Document extends AnyRef with DatabaseObject
{
  /** The URL defining this document's location or "<unknown>". */
  def location () :String =
    attr[String, OptAttr[String]](classOf[OptAttr[String]], "location") | "<unknown>"

  /** The name of this document or "<unknown>". */
  def name () :String =
    attr[String, OptAttr[String]](classOf[OptAttr[String]], "name") | "<unknown>"

  /** The text of this document. */
  def text () :Option[String] =
    attr[String, OptAttr[String]](classOf[OptAttr[String]], "text").data

  /** This document's binary data. */
  def bits () :Option[ByteBuffer] =
    attr[ByteBuffer, OptAttr[ByteBuffer]](classOf[OptAttr[ByteBuffer]], "bits").data

  /** The date on which this document was created. */
  def created () :Option[Date] =
    attr[Date, OptAttr[Date]](classOf[OptAttr[Date]], "created").data

  /** The date on which this document was last modified. */
  def lastModified () :Option[Date] =
    attr[Date, OptAttr[Date]](classOf[OptAttr[Date]], "last_modified").data
}
