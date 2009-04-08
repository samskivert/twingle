//
// $Id$

package com.twingle.spider

import java.util.{Date, Properties}

import javax.mail.{Address, FetchProfile, Flags, Folder, Message,
                   MessagingException, Session, Store}
import javax.mail.Flags.Flag
import javax.mail.internet.InternetAddress

import com.twingle.Log.log

class MailException (msg: String) extends Exception(msg)

case class MailSpiderConfig (val protocol :String,
                             val host :String,
                             val port :Int,
                             val username :String,
                             val password :String,
                             val debug :Boolean) extends SpiderConfig {
  def toString (buf :StringBuffer) = {
    buf.append("protocol=").append(protocol)
    buf.append(", host=").append(host)
    buf.append(", port=").append(port)
    buf.append(", username=").append(username)
    buf.append(", password=").append(password)
    buf.append(", debug=").append(debug)
  }
}

case class MailMessage (val title :String) {
  override def toString :String = {
    val buf :StringBuffer = new StringBuffer("[")
    buf.append("title=").append(title)
    return buf.append("]").toString
  }
}

case class MailSpiderResult (val messages :Seq[MailMessage])
     extends SpiderResult {
  def toString (buf :StringBuffer) = {
    buf.append("messages=").append(messages)
  }
}

class MailSpider (urlFetcher :URLFetcher) extends Spider(urlFetcher) {
  def crawl (configs :Seq[SpiderConfig]) :Seq[SpiderResult] =
    configs.map(_ match { case c :MailSpiderConfig => fetchMail(c) })

  def fetchMail (config :MailSpiderConfig) :MailSpiderResult = {
    try {
      var messages :List[MailMessage] = Nil

      // get a base mail properties object
      val props :Properties = System.getProperties

      // get a session
      val session :Session = Session.getInstance(props, null)
      session.setDebug(config.debug)

      // get a store and connect to the server
      val store :Store = session.getStore(config.protocol)
      store.connect(config.host, config.port, config.username, config.password)

      var folder :Folder = store.getDefaultFolder
      if (folder == null) {
        log.warning("No default folder", "config", config)
        return null
      }

      val mbox :String = "INBOX"
      folder = folder.getFolder(mbox)
      if (folder == null) {
        log.warning("Couldn't look up main inbox folder.")
        return null
      }

      // try to open read/write and if that fails try read-only
      try {
        folder.open(Folder.READ_WRITE)

      } catch {
        case e :MessagingException => folder.open(Folder.READ_ONLY)
      }

      val totalMessages :Int = folder.getMessageCount
      if (totalMessages == 0) {
        log.warning("Empty folder.")
        folder.close(false)
        store.close
        return null
      }

      val newMessages :Int = folder.getNewMessageCount
      log.info("Total messages = " + totalMessages)
      log.info("New messages = " + newMessages)
      log.info("-------------------------------")

      // Attributes & Flags for all messages ..
      val msgs = folder.getMessages

      // Use a suitable FetchProfile
      val fp :FetchProfile = new FetchProfile
      fp.add(FetchProfile.Item.ENVELOPE)
      fp.add(FetchProfile.Item.FLAGS)
      fp.add("X-Mailer")
      folder.fetch(msgs, fp)

      for (i <- 0 until msgs.length) {
        log.info("--------------------------")
        log.info("MESSAGE #" + (i + 1) + ":")

        val m = msgs(i)
        dumpEnvelope(m)
        // dumpPart(m)

        // tack the message onto the main message list
        messages = MailMessage(m.getSubject) :: messages
      }

      // wrap up
      folder.close(false)
      store.close

      // construct the final result record with all messages
      MailSpiderResult(messages)

    } catch {
      case e :Exception => {
        log.warning("Failed fetching mail", e)
        return null
      }
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

object MailSpiderApp {
  def main (args :Array[String]) {
    // read command-line arguments
    if (args.length < 4) {
      log.warning("Protocol, host, user and password required.")
      exit
    }
    val protocol = args(0)
    val host = args(1)
    val user = args(2)
    val password = args(3)

    val port = -1
    val debug = true
    val configs = List(MailSpiderConfig(protocol, host, port, user, password,
                                        debug))

    val crawler = new MailSpider(new URLFetcher)
    val results = crawler.crawl(configs)
    results.foreach(log.info(_))
  }
}
