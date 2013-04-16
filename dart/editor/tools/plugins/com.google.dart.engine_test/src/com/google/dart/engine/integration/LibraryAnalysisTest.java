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
package com.google.dart.engine.integration;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.internal.resolver.ResolutionVerifier;
import com.google.dart.engine.internal.resolver.StaticTypeVerifier;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 * The abstract class {@code LibraryAnalysisTest} defines utility methods useful for integration
 * tests that analyze one or more libraries as a single test.
 */
public abstract class LibraryAnalysisTest extends TestCase {
  /**
   * A set containing all of the libraries that have been visited as part of verifying the results
   * of analysis.
   */
  private HashSet<LibraryElement> visitedLibraries = new HashSet<LibraryElement>();

  /**
   * A list to which all of the errors in the libraries will be added.
   */
  private ArrayList<AnalysisError> errorList = new ArrayList<AnalysisError>();

  /**
   * The object used to verify that everything that ought to have a static type associated with it
   * has a static type.
   */
  private StaticTypeVerifier staticTypeVerifier = new StaticTypeVerifier();

  /**
   * The object used to verify that everything that ought to have been resolved was resolved.
   */
  private ResolutionVerifier resolutionVerifier = new ResolutionVerifier();

  /**
   * The object used to verify that the element structures of the libraries were correctly formed.
   */
  private ElementStructureVerifier elementVerifier = new ElementStructureVerifier();

  /**
   * Assert that the results of analyzing the libraries (as computed by {#link
   * {@link #verify(LibraryElement)}) were as expected.
   */
  protected void assertValid() {
    assertErrors();
    elementVerifier.assertValid();
    staticTypeVerifier.assertResolved();
    resolutionVerifier.assertResolved();
  }

  /**
   * Add all of the errors in the given library and all referenced libraries to the given list of
   * errors.
   * 
   * @param library the library whose errors are to be added
   * @param visitedLibraries a set of all of the libraries whose errors have already been added,
   *          used to prevent infinite recursion when there are mutually dependent libraries (and
   *          duplication of errors)
   * @throws AnalysisException if the errors could not be determined
   */
  protected void verify(LibraryElement library) throws AnalysisException {
    if (library == null || visitedLibraries.contains(library)) {
      return;
    }
    visitedLibraries.add(library);
    library.accept(elementVerifier);
    verify(library.getDefiningCompilationUnit());
    for (CompilationUnitElement part : library.getParts()) {
      verify(part);
    }
    for (LibraryElement importedLibrary : library.getImportedLibraries()) {
      verify(importedLibrary);
    }
    for (LibraryElement exportedLibrary : library.getExportedLibraries()) {
      verify(exportedLibrary);
    }
  }

  /**
   * Assert that the errors that were reported match the expected behavior of the test.
   */
  private void assertErrors() {
    if (errorList.size() > 0) {
      PrintStringWriter writer = new PrintStringWriter();
      writer.print("Expected 0 errors, found ");
      writer.print(errorList.size());
      writer.print(":");
//      Collections.sort(errorList, AnalysisError.FILE_COMPARATOR);
      Collections.sort(errorList, AnalysisError.ERROR_CODE_COMPARATOR);
      for (AnalysisError error : errorList) {
        Source source = error.getSource();
        ErrorCode code = error.getErrorCode();
        int offset = error.getOffset();
        writer.println();
        writer.printf(
            "  %s %s (%d..%d) \"%s\"",
            source == null ? "null" : source.getShortName(),
            code.getClass().getSimpleName() + "." + code,
            offset,
            offset + error.getLength(),
            error.getMessage());
      }
      Assert.fail(writer.toString());
    }
  }

  /**
   * Add the errors reported for the given compilation unit to the given list of errors.
   * 
   * @param element the compilation unit whose errors are to be added
   * @throws AnalysisException if the errors could not be determined
   */
  private void verify(CompilationUnitElement element) throws AnalysisException {
    LibraryElement library = element.getLibrary();
    AnalysisContext context = library.getContext();
    CompilationUnit unit = context.resolveCompilationUnit(element.getSource(), library);
    AnalysisError[] errors = unit.getErrors();
    if (errors == null) {
      Assert.fail("The compilation unit \"" + element.getSource().getFullName()
          + "\" was not resolved");
    }
    for (AnalysisError error : errors) {
      errorList.add(error);
    }
    unit.accept(staticTypeVerifier);
    unit.accept(resolutionVerifier);
  }
}
