package com.google.dart.engine.internal.context;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.internal.resolver.LibraryResolver;

/**
 * Instances of the class {@code ResolvableCompilationUnit} represent a compilation unit that is not
 * referenced by any other objects and for which we have modification stamp information. It is used
 * by the {@link LibraryResolver library resolver} to resolve a library.
 */
public class ResolvableCompilationUnit {
  /**
   * The modification time of the source from which the AST was created.
   */
  private long modificationStamp;

  /**
   * The AST that was created from the source.
   */
  private CompilationUnit unit;

  /**
   * Initialize a newly created holder to hold the given values.
   * 
   * @param modificationStamp the modification time of the source from which the AST was created
   * @param unit the AST that was created from the source
   */
  public ResolvableCompilationUnit(long modificationStamp, CompilationUnit unit) {
    this.modificationStamp = modificationStamp;
    this.unit = unit;
  }

  /**
   * Return the AST that was created from the source.
   * 
   * @return the AST that was created from the source
   */
  public CompilationUnit getCompilationUnit() {
    return unit;
  }

  /**
   * Return the modification time of the source from which the AST was created.
   * 
   * @return the modification time of the source from which the AST was created
   */
  public long getModificationStamp() {
    return modificationStamp;
  }
}
