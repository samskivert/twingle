You are here: [Twingle](http://code.google.com/p/twingle/) - [Overview](Overview.md) - Data API

## Data API ##

The data API is the main way to access Your Data. In includes two major components: adding data (injection) and retrieving data (querying).

## Ontology Classes ##

The Twingle data API contains classes that represent entities in its [Ontology](Ontology.md) which are built from [Database](Database.md) primitives.

```
// syntactically, you can say class Foo extends Bar when Bar is a trait, but
// for clarity about the traitfulness of D.O. I write class Foo extends AnyRef with Bar
// which is what the former expands to.
class Person extends AnyRef with DatabaseObject {
  /** This person's primary name, or Nil if they have none. */
  def name : String = attr(Text, "name").getOrElse(Nil)

  /** Other names by which this person is known. */
  def aliases : Text = attr(Text, "aliases")

  /** Email addresses associated with this person. */
  def addresses : Text = attr(Text, "addresses")
}

class Document extends AnyRef with DatabaseObject {
  /** A URL defining this document's location or Nil. */
  def location () :String = attr(Text, "location").getOrElse(Nil)

  /** The name of this document or Nil. */
  def name () :String = attr(Text, "name").getOrElse(Nil)

  /** The text of this document or Nil. */
  def text () :String = attr(Text, "text").getOrElse(Nil)

  /** This document's binary data or Nil. */
  def bits () :ByteArray = attr(Bits, "bits").getOrElse(Nil)

  /** The date on which this document was created or Nil. */
  def created () :Date = attr(Date, "created").getOrElse(Nil)

  /** The date on which this document was last modified or Nil. */
  def lastModified () :Date = attr(Date, "last_modified").getOrElse(Nil)
}

class Message extends Document {
  // TBD
}

class Conversation extends AnyRef with DatabaseObject {
  // TBD
}
```

## Injection API ##

The injection API relies on the caller to have done the work of parsing the source data into objects from Twingle's ObjectModel. However, nothing ...

TBD

## Query API ##

TBD