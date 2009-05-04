//
// $Id$

package com.twingle.spider

import java.util.{Date, Properties}

import javax.mail.{Address, FetchProfile, Flags, Folder, Message,
                   MessagingException, Session, Store}
import javax.mail.Flags.Flag
import javax.mail.internet.{InternetAddress, MimeMultipart}

import com.twingle.Log.log
import com.twingle.daemon.{Env, Job}
import com.twingle.model.Document

class MailSpider extends Spider
{
  import MailSpider._

  def fetchMail (config :Config) :Seq[MailMessage] = {
    try {
      // get a base mail properties object
      val props :Properties = System.getProperties

      // get a session
      val session :Session = Session.getInstance(props, null)
      session.setDebug(config.debug)

      // get a store and connect to the server
      val store :Store = session.getStore(config.protocol)
      store.connect(config.host, config.port, config.username, config.password)

      // get the default folder from the store
      val folder :Folder = store.getDefaultFolder
      val mbox = "INBOX"
      val subFolder = folder.getFolder(mbox)

      // try to pull down all messages
      val folderMessages :Seq[MailMessage] = fetchFolderMessages(subFolder)

      // close down the folders and store
      subFolder.close(false)
      store.close

      // return any messages obtained
      folderMessages

    } catch {
      case e :Exception => {
        log.warning("Failed fetching mail", e)
        return List[MailMessage]()
      }
    }
  }

  /**
   * Fetches all messages in the supplied {@link Folder} and returns
   * them as a sequence of {@link MailMessage} records.
   */
  def fetchFolderMessages (folder :Folder) :Seq[MailMessage] = {
    // open the folder for reading messages
    folder.open(Folder.READ_ONLY)

    // get a count of total messages in the folder
    val totalCount :Int = folder.getMessageCount
    if (totalCount == 0) {
      log.info("No messages in folder")
      return List[MailMessage]()
    }

    // get a count of new messages in the folder
    val newCount :Int = folder.getNewMessageCount
    log.info("Message count", "folder", folder, "total", ""+totalCount, 
             "new", ""+newCount)

    // retrieve all messages from the folder
    val msgs = folder.getMessages

    // build fetch profile to pull down specifically sought data
    val fp :FetchProfile = new FetchProfile
    fp.add(FetchProfile.Item.ENVELOPE)
    fp.add(FetchProfile.Item.CONTENT_INFO)
//     fp.add(FetchProfile.Item.FLAGS)
//     fp.add("X-Mailer")
    folder.fetch(msgs, fp)

    // build our final list of mail messages from the message records
    msgs.map(m => constructMailMessage(m))
  }

  def constructMailMessage (m :Message) :MailMessage = {
    // pull out the simple bits
    val from = addressesToString(m.getFrom)
    val replyTo = addressesToString(m.getReplyTo)
    val to = addressesToString(m.getRecipients(Message.RecipientType.TO))
    val subject = m.getSubject
    val date = m.getSentDate
    val content = m.getContent
    MailMessage(from, to, replyTo, subject, date, content)
  }

  def addressesToString (addr :Seq[Address]) :String = {
    if (addr == null) {
      ""

    } else {
      addr.map(_.toString).mkString(",")
    }
  }
  
  def dumpEnvelope (m :Message) {
    pr("This is the message envelope");
    pr("---------------------------");

    // FROM 
    var a :Seq[Address] = m.getFrom
    if (a != null) {
      for (j <- 0 until a.length) {
	pr("FROM: " + a(j).toString)
      }
    }

    // REPLY TO
    a = m.getReplyTo
    if (a != null) {
      for (j <- 0 until a.length) {
	pr("REPLY TO: " + a(j).toString)
      }
    }

    // TO
    a = m.getRecipients(Message.RecipientType.TO)
    if (a != null) {
      for (j <- 0 until a.length) {
	pr("TO: " + a(j).toString)
	val ia :InternetAddress = a(j).asInstanceOf[InternetAddress]
	if (ia.isGroup) {
	  val aa :Seq[InternetAddress] = ia.getGroup(false)
	  for (k <- 0 until aa.length) {
	    pr("  GROUP: " + aa(k).toString)
          }
	}
      }
    }

    // SUBJECT
    pr("SUBJECT: " + m.getSubject)

    // DATE
    val d :Date = m.getSentDate
    pr("SendDate: " + (if (d != null) d.toString else "UNKNOWN"))

    // FLAGS
    val flags :Flags = m.getFlags
    val sb :StringBuffer = new StringBuffer
    val sf :Seq[Flags.Flag] = flags.getSystemFlags // get the system flags

    var first :Boolean = true;
    for (i <- 0 until sf.length) {
      var s :String = null
      val f :Flags.Flag = sf(i)

      if (f == Flags.Flag.ANSWERED)
	s = "\\Answered"
      else if (f == Flags.Flag.DELETED)
	s = "\\Deleted"
      else if (f == Flags.Flag.DRAFT)
	s = "\\Draft"
      else if (f == Flags.Flag.FLAGGED)
	s = "\\Flagged"
      else if (f == Flags.Flag.RECENT)
	s = "\\Recent"
      else if (f == Flags.Flag.SEEN)
	s = "\\Seen"

      if (s != null) {
        if (first)
	  first = false
        else
	  sb.append(' ')
        sb.append(s)
      }
    }

    val uf :Seq[String] = flags.getUserFlags // get the user flag strings
    for (i <- 0 until uf.length) {
      if (first)
	first = false
      else
	sb.append(' ')
      sb.append(uf(i))
    }
    pr("FLAGS: " + sb.toString)

    // X-MAILER
    val hdrs :Seq[String] = m.getHeader("X-Mailer")
    if (hdrs != null)
      pr("X-Mailer: " + hdrs(0))
    else
      pr("X-Mailer NOT available")
  }

  def pr (s :String) {
    log.info(s)
  }
}

object MailSpider
{
  class Config extends Spider.Config {
    /** The email account's username. */
    def username () :String = reqA(stringM, 'username).data

    /** The email account's password. TODO: can we encyrpt this? */
    def password () :String = reqA(stringM, 'password).data

    /** The protocol to use when talking to the mail server (imap or imaps). */
    def protocol () :String = reqA(stringM, 'protocol).data

    /** The host at which to contact the mail server. */
    def host () :String = reqA(stringM, 'host).data

    /** The port on which to contact the mail server. */
    def port () :Int = reqA(intM, 'port).data

    /** Whether or not to enable debug mode. */
    def debug () :Boolean = reqA(booleanM, 'debug).data

    def createJob () = new Job() {
      def run (env :Env) {
        val spider = new MailSpider
        spider.fetchMail(Config.this).foreach(t => env.db.store(toDocument(t)))
      }

      def toDocument (m :MailMessage) = Document.builder.name(m.subject).created(m.date).build
    }
  }    

  // TODO: store flags e.g. answered, draft, seen, etc.; other headers?
  case class MailMessage (val from :String,
                          val to :String,
                          val replyTo :String,
                          val subject :String,
                          val date :Date,
                          val contents :Object)

  def configBuilder () = new Spider.ConfigBuilder {
    def username (username :String) = { add('username, username); this }
    def password (password :String) = { add('password, password); this }
    def protocol (protocol :String) = { add('protocol, protocol); this }
    def host (host :String) = { add('host, host); this }
    def port (port :Int) = { add('port, port); this }
    def debug (debug :Boolean) = { add('debug, debug); this }
    def build :MailSpider.Config = build(new MailSpider.Config)
  }
}

object MailSpiderApp {
  def main (args :Array[String]) {
    // read command-line arguments
    if (args.length < 4) {
      log.warning("Protocol, host, user and password required.")
      exit
    }

    val port = -1
    val debug = true
    val config = MailSpider.configBuilder.protocol(args(0)).host(args(1)).port(port).
                   username(args(2)).password(args(3)).debug(debug).build

    val spider = new MailSpider
    val messages = spider.fetchMail(config)
    messages.foreach(log.info(_))
  }
}
