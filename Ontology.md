You are here: [Twingle](http://code.google.com/p/twingle/) - [Overview](Overview.md) - Ontology

## Ontology ##

Twingle shoehorns the world of your data into a simple, but flexible ontology (set of concepts and their relations). Yes your data is precious and unique, but computers need structure. We bow to the almighty algorithm.

## Person ##

Just what you might think: a person.

**Attributes**

  * name - your favorite name for this person
  * alias (✕N) - any number of other names Twingle encounters for this person as it is peacefully grinding through your data
  * address (✕N) - any number of email addresses that seem to belong to this person
  * data source specific attributes like:
  * facebook\_id
  * twitter\_id
  * IM usernames of all shapes and sizes

Twingle tries to be smart about identifying the people in your life. When it discovers people in your data, it tries to figure out whether it already knows about those people and should expand its understanding of them by adding to their data, or whether it should celebrate the discovery of a brand new intelligent entity. Sometimes it will make mistakes and you'll be able to show Twingle the error of its ways.

## Document ##

A document is a glorified bag of bytes. It might not even be a document (that's one great thing about being a computer scientist, you can totally misuse words), it might be an image or an audio file or a small virus infected program sent to you in an email attachment (though we won't keep the viral part around).

**Attributes**

  * location - a URL defining this document's home on the interwebs
  * name - the name of the document
  * text - the text of the document (not in the literary critical sense, just a bunch of UTF-8 characters)
  * bits - the binary data that makes up this document (a document would generally have only text or bits but Twingle doesn't really care, it can have none or both if it likes)
  * created - when the document was created (if we know)
  * last\_modified - when the document was last modified (if we know)

## Message ##

A message represents the output of a Person that most likely originated as an attempt at communication. It may also just be a cry for attention. Twingle does not judge. Message extends Document and adds an all important author as well as some other useful things.

**Attributes**

  * subject - the subject of this message; this is the same as Document.name but Twingle doesn't mind repeating itself for clarity
  * author - the Person that authored this message
  * recipient (✕N) - one or more People to whom this message was addressed (Twingle doesn't care about the tyranny of To: versus Cc:)
  * conversation - the id of the Conversation of which this Message is a part

## Conversation ##

A Conversation is a series of Messages that are related. Aside from manifestos and unrequited love letters, most communication involves a bit of back and forth between People. Twingle understands this basic aspect of human nature.

**Attributes**

  * subject - the subject of the first message in this conversation