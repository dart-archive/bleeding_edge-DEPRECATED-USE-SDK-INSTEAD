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
package com.google.dart.engine.internal.cache;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;

/**
 * The interface {@code DartEntry} defines the behavior of objects that maintain the information
 * cached by an analysis context about an individual Dart file.
 * 
 * @coverage dart.engine
 */
public interface DartEntry extends SourceEntry {
  /**
   * The data descriptor representing the errors reported during Angular resolution.
   */
  public static final DataDescriptor<AnalysisError[]> ANGULAR_ERRORS = new DataDescriptor<AnalysisError[]>(
      "DartEntry.ANGULAR_ERRORS");

  /**
   * The data descriptor representing the list of libraries that contain this compilation unit.
   */
  public static final DataDescriptor<Source[]> CONTAINING_LIBRARIES = new DataDescriptor<Source[]>(
      "DartEntry.CONTAINING_LIBRARIES");

  /**
   * The data descriptor representing the library element for the library. This data is only
   * available for Dart files that are the defining compilation unit of a library.
   */
  public static final DataDescriptor<LibraryElement> ELEMENT = new DataDescriptor<LibraryElement>(
      "DartEntry.ELEMENT");

  /**
   * The data descriptor representing the list of exported libraries. This data is only available
   * for Dart files that are the defining compilation unit of a library.
   */
  public static final DataDescriptor<Source[]> EXPORTED_LIBRARIES = new DataDescriptor<Source[]>(
      "DartEntry.EXPORTED_LIBRARIES");

  /**
   * The data descriptor representing the hints resulting from auditing the source.
   */
  public static final DataDescriptor<AnalysisError[]> HINTS = new DataDescriptor<AnalysisError[]>(
      "DartEntry.HINTS");

  /**
   * The data descriptor representing the list of imported libraries. This data is only available
   * for Dart files that are the defining compilation unit of a library.
   */
  public static final DataDescriptor<Source[]> IMPORTED_LIBRARIES = new DataDescriptor<Source[]>(
      "DartEntry.IMPORTED_LIBRARIES");

  /**
   * The data descriptor representing the list of included parts. This data is only available for
   * Dart files that are the defining compilation unit of a library.
   */
  public static final DataDescriptor<Source[]> INCLUDED_PARTS = new DataDescriptor<Source[]>(
      "DartEntry.INCLUDED_PARTS");

  /**
   * The data descriptor representing the client flag. This data is only available for Dart files
   * that are the defining compilation unit of a library.
   */
  public static final DataDescriptor<Boolean> IS_CLIENT = new DataDescriptor<Boolean>(
      "DartEntry.IS_CLIENT");

  /**
   * The data descriptor representing the launchable flag. This data is only available for Dart
   * files that are the defining compilation unit of a library.
   */
  public static final DataDescriptor<Boolean> IS_LAUNCHABLE = new DataDescriptor<Boolean>(
      "DartEntry.IS_LAUNCHABLE");

  /**
   * The data descriptor representing the errors resulting from parsing the source.
   */
  public static final DataDescriptor<AnalysisError[]> PARSE_ERRORS = new DataDescriptor<AnalysisError[]>(
      "DartEntry.PARSE_ERRORS");

  /**
   * The data descriptor representing the parsed AST structure.
   */
  public static final DataDescriptor<CompilationUnit> PARSED_UNIT = new DataDescriptor<CompilationUnit>(
      "DartEntry.PARSED_UNIT");

  /**
   * The data descriptor representing the public namespace of the library. This data is only
   * available for Dart files that are the defining compilation unit of a library.
   */
  public static final DataDescriptor<Namespace> PUBLIC_NAMESPACE = new DataDescriptor<Namespace>(
      "DartEntry.PUBLIC_NAMESPACE");

  /**
   * The data descriptor representing the errors resulting from resolving the source.
   */
  public static final DataDescriptor<AnalysisError[]> RESOLUTION_ERRORS = new DataDescriptor<AnalysisError[]>(
      "DartEntry.RESOLUTION_ERRORS");

  /**
   * The data descriptor representing the resolved AST structure.
   */
  public static final DataDescriptor<CompilationUnit> RESOLVED_UNIT = new DataDescriptor<CompilationUnit>(
      "DartEntry.RESOLVED_UNIT");

  /**
   * The data descriptor representing the token stream.
   */
  public static final DataDescriptor<AnalysisError[]> SCAN_ERRORS = new DataDescriptor<AnalysisError[]>(
      "DartEntry.SCAN_ERRORS");

  /**
   * The data descriptor representing the source kind.
   */
  public static final DataDescriptor<SourceKind> SOURCE_KIND = new DataDescriptor<SourceKind>(
      "DartEntry.SOURCE_KIND");

  /**
   * The data descriptor representing the token stream.
   */
  public static final DataDescriptor<Token> TOKEN_STREAM = new DataDescriptor<Token>(
      "DartEntry.TOKEN_STREAM");

  /**
   * The data descriptor representing the errors resulting from verifying the source.
   */
  public static final DataDescriptor<AnalysisError[]> VERIFICATION_ERRORS = new DataDescriptor<AnalysisError[]>(
      "DartEntry.VERIFICATION_ERRORS");

  /**
   * Return all of the errors associated with the compilation unit that are currently cached.
   * 
   * @return all of the errors associated with the compilation unit
   */
  public AnalysisError[] getAllErrors();

  /**
   * Return a valid parsed compilation unit, either an unresolved AST structure or the result of
   * resolving the AST structure in the context of some library, or {@code null} if there is no
   * parsed compilation unit available.
   * 
   * @return a valid parsed compilation unit
   */
  public CompilationUnit getAnyParsedCompilationUnit();

  /**
   * Return the result of resolving the compilation unit as part of any library, or {@code null} if
   * there is no cached resolved compilation unit.
   * 
   * @return any resolved compilation unit
   */
  public CompilationUnit getAnyResolvedCompilationUnit();

  /**
   * Return the state of the data represented by the given descriptor in the context of the given
   * library.
   * 
   * @param descriptor the descriptor representing the data whose state is to be returned
   * @param librarySource the source of the defining compilation unit of the library that is the
   *          context for the data
   * @return the value of the data represented by the given descriptor and library
   */
  public CacheState getStateInLibrary(DataDescriptor<?> descriptor, Source librarySource);

  /**
   * Return the value of the data represented by the given descriptor in the context of the given
   * library, or {@code null} if the data represented by the descriptor is not in the cache.
   * 
   * @param descriptor the descriptor representing which data is to be returned
   * @param librarySource the source of the defining compilation unit of the library that is the
   *          context for the data
   * @return the value of the data represented by the given descriptor and library
   */
  public <E> E getValueInLibrary(DataDescriptor<E> descriptor, Source librarySource);

  @Override
  public DartEntryImpl getWritableCopy();

  /**
   * Return {@code true} if the data represented by the given descriptor is marked as being invalid.
   * If the descriptor represents library-specific data then this method will return {@code true} if
   * the data associated with any library it marked as invalid.
   * 
   * @param descriptor the descriptor representing which data is being tested
   * @return {@code true} if the data is marked as being invalid
   */
  public boolean hasInvalidData(DataDescriptor<?> descriptor);

  /**
   * Return {@code true} if this entry has an AST structure that can be resolved (even if it needs
   * to be copied).
   * 
   * @return {@code true} if the method {@link DartEntryImpl#getResolvableCompilationUnit()} will
   *         return a non-{@code null} result
   */
  public boolean hasResolvableCompilationUnit();

  /**
   * Return {@code true} if this data is safe to use in refactoring.
   */
  public boolean isRefactoringSafe();
}
