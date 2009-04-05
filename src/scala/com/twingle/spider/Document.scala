package com.twingle.spider

import java.util.Date

class Document
{
  var location :String = null
  var name :String = null
  var text :String = null
  var bits :String = null
  var created :Date = null
  var lastModified :Date = null

  override def toString () = {
    val buf :StringBuffer = new StringBuffer
    buf.append("[location=").append(location)
    buf.append(", name=").append(name)
    buf.append(", text=").append(text)
    buf.append(", bitCount=").append(if (bits != null) bits.size else 0)
    buf.append(", created=").append(created)
    buf.append(", lastModified=").append(lastModified)
    buf.toString
  }
}
