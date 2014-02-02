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
import com.google.dart.engine.error.TodoCode;
import com.google.dart.engine.internal.context.PerformanceStatistics;
import com.google.dart.engine.internal.resolver.ResolutionVerifier;
import com.google.dart.engine.internal.resolver.StaticTypeVerifier;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
   * A flag indicating whether libraries that are part of the SDK should be validated.
   */
  private boolean validateSdk = false;

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
   * The object used to verify that the containing libraries of the sources were correctly computed.
   */
  private ContainingLibrariesVerifier librariesVerifier = new ContainingLibrariesVerifier();

  /**
   * Assert that the results of analyzing the libraries (as computed by {#link
   * {@link #verify(LibraryElement)}) were as expected.
   * 
   * @param context the analysis context used to get additional information
   */
  protected void assertValid(AnalysisContext context) {
    assertErrors();
    elementVerifier.assertValid();
    staticTypeVerifier.assertResolved();
    resolutionVerifier.assertResolved();
    librariesVerifier.assertValid(context);
  }

  protected void printStatistics() {
    System.out.print("  scan:    ");
    printTime(PerformanceStatistics.scan.getResult());
    System.out.println();

    System.out.print("  parse:   ");
    printTime(PerformanceStatistics.parse.getResult());
    System.out.println();

    System.out.print("  resolve: ");
    printTime(PerformanceStatistics.resolve.getResult());
    System.out.println();

    System.out.print("  errors:  ");
    printTime(PerformanceStatistics.errors.getResult());
    System.out.println();

    System.out.print("  hints:   ");
    printTime(PerformanceStatistics.hints.getResult());
    System.out.println();
  }

  /**
   * Mark the given library as one that should be ignored when validating the libraries that were
   * analyzed.
   * 
   * @param library the library to be ignored
   */
  protected void validateSdk() {
    validateSdk = true;
  }

  /**
   * Add all of the errors in the given library and all referenced libraries to the list of errors.
   * 
   * @param library the library whose errors are to be added
   * @throws AnalysisException if the errors could not be determined
   */
  protected void verify(LibraryElement library) throws AnalysisException {
    if (library == null || visitedLibraries.contains(library)
        || (!validateSdk && library.getSource().isInSystemLibrary())) {
      return;
    }
    visitedLibraries.add(library);
    library.accept(elementVerifier);
    CompilationUnitElement definingUnit = library.getDefiningCompilationUnit();
    verify(definingUnit);
    librariesVerifier.addLibrary(definingUnit);
    for (CompilationUnitElement part : library.getParts()) {
      verify(part);
      librariesVerifier.addPart(part, definingUnit);
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
    int size = errorList.size();
    if (size > 0) {
      @SuppressWarnings("resource")
      PrintStringWriter writer = new PrintStringWriter();
      writer.print("Expected 0 errors, found ");
      writer.print(errorList.size());
      writer.print(":");
      int todoCount = countErrorsOfType(TodoCode.TODO);
      size -= todoCount;
      if (size > 128) {
        HashMap<String, Integer> counts = new HashMap<String, Integer>();
        int maxCount = 0;
        for (AnalysisError error : errorList) {
          ErrorCode code = error.getErrorCode();
          String codeName = code.getClass().getSimpleName() + "." + code;
          Integer oldCount = counts.get(codeName);
          int newCount = (oldCount == null) ? 1 : oldCount.intValue() + 1;
          counts.put(codeName, Integer.valueOf(newCount));
          maxCount = Math.max(maxCount, newCount);
        }
        int countWidth = Integer.toString(maxCount).length();
        String format = "%0" + countWidth + "d" + " %s";
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
          writer.println();
          writer.printf(format, entry.getValue(), entry.getKey());
        }
      } else {
//        Collections.sort(errorList, AnalysisError.FILE_COMPARATOR);
        Collections.sort(errorList, AnalysisError.ERROR_CODE_COMPARATOR);
        for (AnalysisError error : errorList) {
          Source source = error.getSource();
          ErrorCode code = error.getErrorCode();
          if (code != TodoCode.TODO) {
            int offset = error.getOffset();
            writer.println();
            writer.printf(
                "%s %s (%d..%d) \"%s\"%s",
                source == null ? "null" : source.getShortName(),
                code.getClass().getSimpleName() + "." + code,
                offset,
                offset + error.getLength(),
                error.getMessage(),
                source == null ? "" : " (" + source.getFullName() + ")");
          }
        }
        if (todoCount > 0) {
          writer.println();
          writer.printf("%d %s", todoCount, TodoCode.TODO);
        }
      }
      Assert.fail(writer.toString());
    }
  }

  /**
   * Return the number of errors with the given error code that were recorded.
   * 
   * @param errorCode the error code being searched for
   * @return the number of errors with the given error code
   */
  private int countErrorsOfType(ErrorCode errorCode) {
    int count = 0;
    for (AnalysisError error : errorList) {
      if (error.getErrorCode() == errorCode) {
        count++;
      }
    }
    return count;
  }

  /**
   * Print the given value as a number of milliseconds.
   * 
   * @param time the number of milliseconds to be printed
   */
  private void printTime(long time) {
    if (time == 0) {
      System.out.print("0 ms");
    } else {
      System.out.print(time);
      System.out.print(" ms");
      if (time > 60000) {
        long seconds = time / 1000;
        long minutes = seconds / 60;
        seconds -= minutes * 60;
        System.out.print(" (");
        System.out.print(minutes);
        System.out.print(":");
        if (seconds < 10) {
          System.out.print("0");
        }
        System.out.print(seconds);
        System.out.print(")");
      }
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
    AnalysisError[] errors = context.computeErrors(element.getSource());
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
