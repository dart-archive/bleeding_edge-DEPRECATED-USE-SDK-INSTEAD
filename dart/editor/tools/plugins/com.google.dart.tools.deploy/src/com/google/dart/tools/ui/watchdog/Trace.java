package com.google.dart.tools.ui.watchdog;

import java.util.Arrays;

/**
 * A single trace and what point in time it occurred during the event processing.
 */
final class Trace {

  /** The time in milliseconds when the trace was taken */
  final long timestampMillis;

  final long durationMicros;

  final Event event;

  /** The stack frames of the trace */
  final StackTraceElement[] frames;

  Trace(Event event, long timestampMillis, long durationMicros, StackTraceElement[] frames) {
    this.event = event;
    this.timestampMillis = timestampMillis;
    this.durationMicros = durationMicros;
    this.frames = frames;
  }

  @Override
  public boolean equals(Object b) {
    if (b == this) {
      return true;
    }
    if (b == null) {
      return false;
    }
    if (!(b instanceof Trace)) {
      return false;
    }
    Trace otherTrace = (Trace) b;

    return timestampMillis == otherTrace.timestampMillis
        && durationMicros == otherTrace.durationMicros
        && Arrays.deepEquals(frames, otherTrace.frames);
  }

  public long getDurationMicros() {
    return durationMicros;
  }

  public Event getEvent() {
    return event;
  }

  public StackTraceElement[] getStackTrace() {
    return frames;
  }

  public long getTimestampMillis() {
    return timestampMillis;
  }

  @Override
  public int hashCode() {
    return 31 * Arrays.hashCode(new Object[] {timestampMillis, durationMicros})
        + Arrays.deepHashCode(frames);
  }
}
