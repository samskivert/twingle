//
// $Id$

package com.twingle

import com.samskivert.util.Logger

/**
 * Provides a logger used by the entire project.
 */
object Log
{
  /** Statically import this into your class and use it like so:
   * <pre>
   * log.warning("Oh my god, the Canadians are coming!", "radar", radarReading())
   * </pre>
   */
  val log = Logger.getLogger("com.twingle")
}
