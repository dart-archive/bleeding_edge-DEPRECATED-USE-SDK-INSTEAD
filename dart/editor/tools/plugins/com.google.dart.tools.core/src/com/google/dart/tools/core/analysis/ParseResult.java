package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ast.DartUnit;

/**
 * Result of a parse returned via {@link ParseCallback}
 */
public class ParseResult {

  public static final DartCompilationError[] NO_ERRORS = new DartCompilationError[] {};

  private final DartUnit dartUnit;
  private final DartCompilationError[] errors;

  public ParseResult(DartUnit dartUnit, DartCompilationError[] errors) {
    this.dartUnit = dartUnit;
    this.errors = errors != null ? errors : NO_ERRORS;
  }

  /**
   * Answer the parsed (maybe resolved) Dart unit
   * 
   * @return the unit (not <code>null</code>)
   */
  public DartUnit getDartUnit() {
    return dartUnit;
  }

  /**
   * Answer errors that occurred during the parse process
   * 
   * @return the errors (not <code>null</code>, contains no <code>null</code>)
   */
  public DartCompilationError[] getParseErrors() {
    return errors;
  }
}
