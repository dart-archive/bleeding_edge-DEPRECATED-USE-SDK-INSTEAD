/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.engine.internal.context;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.internal.resolver.LibraryResolver;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code ResolvableCompilationUnit} represent a compilation unit that is not
 * referenced by any other objects and for which we have modification stamp information. It is used
 * by the {@link LibraryResolver library resolver} to resolve a library.
 */
public class ResolvableCompilationUnit extends TimestampedData<CompilationUnit> {
  /**
   * The source of the compilation unit.
   */
  private Source source;

  /**
   * Initialize a newly created holder to hold the given values.
   * 
   * @param modificationTime the modification time of the source from which the AST was created
   * @param unit the AST that was created from the source
   */
  public ResolvableCompilationUnit(long modificationTime, CompilationUnit unit) {
    this(modificationTime, unit, null);
  }

  /**
   * Initialize a newly created holder to hold the given values.
   * 
   * @param modificationTime the modification time of the source from which the AST was created
   * @param unit the AST that was created from the source
   * @param source the source of the compilation unit
   */
  public ResolvableCompilationUnit(long modificationTime, CompilationUnit unit, Source source) {
    super(modificationTime, unit);
    this.source = source;
  }

  /**
   * Return the AST that was created from the source.
   * 
   * @return the AST that was created from the source
   */
  public CompilationUnit getCompilationUnit() {
    return getData();
  }

  /**
   * Return the source of the compilation unit.
   * 
   * @return the source of the compilation unit
   */
  public Source getSource() {
    return source;
  }
}
