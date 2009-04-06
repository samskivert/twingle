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
  class Builder extends DatabaseObject.Builder {
    def location (location :String) = { add("location", location); this }
    def name (name :String) = { add("name", name); this }
    def text (text :String) = { add("text", text); this }
    def bits (bits :ByteBuffer) = { add("bits", bits); this }
    def created (created :Date) = { add("created", created); this }
    def lastModified (lastModified :Date) = { add("lastModified", lastModified); this }
    def build :Document = build(new Document)
  }

  def builder = new Builder

  //   def make (location :Option[String], name :Option[String], text :Option[String],
//             bits :Option[ByteBuffer], created :Option[Date], lastModified :Option[Date]) :Document =
//     DatabaseObject.builder.add("location", location).add("name", name).add("text", text).
//       add("bits", bits).add("created", created).add("lastModified", lastModified).
//       build(new Document)
}

package tests {
  import org.scalatest.Suite

  class DocumentSuite extends Suite {
    def testBuilder () {
      val doc = Document.builder.
        location("http://www.samskivert.com/").
        name("MDB's Blog").
        text("Foo bar baz!").
        created(new Date()).
        lastModified(new Date()).
        build

      Console.println(doc.created)
      expect("Foo bar baz!") { doc.text }
    }
  }
}
