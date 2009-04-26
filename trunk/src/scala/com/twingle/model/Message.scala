//
// $Id$

package com.twingle.model

import java.util.UUID
import java.util.Date

/**
 * Contains data for a Message stored in the database.
 */
class Message extends Document
{
  /** The subject of this message or "<none>". */
  def subject :String = optA(stringM, "subject") | "<none>"

  /** The author of this message. */
  def author :UUID = reqA(uuidM, "author").data

  /** The recipients of this message. */
  def recipients :List[UUID] = listA(uuidM, "recipients").data

  /** The id of the conversation of which this message is a part. */
  def conversation :Option[UUID] = optA(uuidM, "conversation").data
}

/**
 * Message utility methods.
 */
object Message
{
  def builder = new Document.Builder {
    def subject (subject :String) = { add("subject", subject); this }
    def author (author :UUID) = { add("author", author); this }
    def recipients (recipients :List[UUID]) = { add("recipients", recipients); this }
    def conversation (conversation :UUID) = { add("conversation", conversation); this }
    def build :Message = build(new Message)
  }
}

package tests {
  import org.scalatest.Suite

  class MessageSuite extends Suite {
    def testBuilder () {
      val now = new Date
      val mdb = Person.builder.
        name("Michael Bayne").
        twitter("samskivert").
        build
      val shaper = Person.builder.
        name("Walter Korman").
        twitter("shaper").
        build
      val doc = Message.builder.
        location("twitter://1231232").
        name("Tweet tweet").
        text("@shaper Lulz!").
        created(now).
        lastModified(now).
        author(mdb.id).
        recipients(List(shaper.id)).
        build

      expect("twitter://1231232") { doc.location }
      expect("Tweet tweet") { doc.name }
      expect(mdb.id) { doc.author }
      expect(List(shaper.id)) { doc.recipients }
    }
  }
}
