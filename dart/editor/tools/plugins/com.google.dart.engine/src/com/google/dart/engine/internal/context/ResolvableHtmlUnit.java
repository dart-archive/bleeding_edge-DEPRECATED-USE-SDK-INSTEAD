package com.google.dart.engine.internal.context;

import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.task.ResolveHtmlTask;

/**
 * Instances of the class {@code ResolvableHtmlUnit} represent an HTML unit that is not referenced
 * by any other objects and for which we have modification stamp information. It is used by the
 * {@link ResolveHtmlTask} to resolve an HTML source.
 */
public class ResolvableHtmlUnit {
  /**
   * The modification time of the source from which the AST was created.
   */
  private long modificationTime;

  /**
   * The AST that was created from the source.
   */
  private HtmlUnit unit;

  /**
   * Initialize a newly created holder to hold the given values.
   * 
   * @param modificationTime the modification time of the source from which the AST was created
   * @param unit the AST that was created from the source
   */
  public ResolvableHtmlUnit(long modificationTime, HtmlUnit unit) {
    this.modificationTime = modificationTime;
    this.unit = unit;
  }

  /**
   * Return the AST that was created from the source.
   * 
   * @return the AST that was created from the source
   */
  public HtmlUnit getCompilationUnit() {
    return unit;
  }

  /**
   * Return the modification time of the source from which the AST was created.
   * 
   * @return the modification time of the source from which the AST was created
   */
  public long getModificationTime() {
    return modificationTime;
  }
}
