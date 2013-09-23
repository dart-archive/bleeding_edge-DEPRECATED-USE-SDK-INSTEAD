package com.google.dart.tools.ui.watchdog;

/**
 * Information captured about a set of events.
 */
public class LongEventInfo {
  /**
   * The start time of the first event, in milliseconds since 00:00 of 1 January 1970 Z.
   * 
   * @see System#currentTimeMillis
   */
  public final long start;

  /**
   * The total duration of all events, in milliseconds
   */
  public final long duration;

  /**
   * Constructs an event snapshot object from a contiguous range of events.
   * 
   * @param start the start timestamp in milliseconds since 00:00 of 1 Jan 1970
   * @param duration the duration of the captured events, in milliseconds
   */
  public LongEventInfo(long start, long duration) {
    this.start = start;
    this.duration = duration;
  }
}
