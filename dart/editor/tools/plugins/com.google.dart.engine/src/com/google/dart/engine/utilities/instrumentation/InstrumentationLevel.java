package com.google.dart.engine.utilities.instrumentation;

/**
 * The instrumentation recording level representing (1) recording {@link #EVERYTHING} recording of
 * all instrumentation data, (2) recording only {@link #METRICS} information, or (3) recording
 * turned {@link #OFF} in which case nothing is recorded.
 */
public enum InstrumentationLevel {

  /** Recording all instrumented information */
  EVERYTHING,

  /** Recording only metrics */
  METRICS,

  /** Nothing recorded */
  OFF
}
