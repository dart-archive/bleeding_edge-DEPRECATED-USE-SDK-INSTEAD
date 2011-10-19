/*
 * Copyright 2011 Dart project authors.
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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.test.util.MoneyProjectUtilities;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.util.Hashtable;

/**
 * Long running tests to exercise code completion across lots of code looking for situations in
 * which the code completion functionality throws an unhandled exception. As a byproduct, metrics
 * are tracked and reported.
 */
public class CompletionEngineTest2 extends TestCase {

  private static final long CODE_COMPLETION_THRESHOLD = 500;
  private static final String[] TRAILING_STRINGS = new String[] {"", ";", "(", "{"};
// TODO (danrubel) investigate OutOfMemoryError given larger set of trailing strings
//      "", ";", "(", "{", ")", "}", "[", "]", ".", ",", "$", "="};

  private int totalCount;
  private int exceptionCount;
  private int siteCount;
  private int hasSuggestionsCount;
  private int totalLineCount;
  private long totalResolveLibraryTime;
  private long maxResolveLibraryTime;
  private long totalCodeCompleteTime;
  private long maxCodeCompleteTime;
  private int codeCompleteOverThreshold;
  private long totalOpenTime;
  private int openCount;
  private long maxOpenTime;

  /**
   * Stress test the completion engine, looking for situations that throw unhandled exceptions.
   */
  public void test_CompletionEngine_stressTest() throws Exception {

    testCompletion(MoneyProjectUtilities.getMoneyLibrary());

    // TODO (danrubel) Running this across base/base.dart causes this test to slow to a crawl
    // and finally throw an OutOfMemoryError with -Xms40m -Xmx512m -XX:MaxPermSize=256m
    //Path root = new Path("/path/to/dart/client");
    //testCompletion(root.append("base/base.dart"));
    //testCompletion(root.append("view/view.dart"));
    //testCompletion(root.append("observable/observable.dart"));
    //testCompletion(root.append("touch/touch.dart"));
    //testCompletion(root.append("util/utilslib.dart"));
    //testCompletion(root.append("samples/swarm/swarm.dart"));
    //testCompletion(root.append("samples/swarm/swarmlib.dart"));

    double percentSitesWithLetterOrDot = ((double) siteCount) / ((double) totalCount);
    double percentSitesWithSuggestions = ((double) hasSuggestionsCount) / ((double) siteCount);
    double aveResolveLibraryTime = ((double) totalResolveLibraryTime) / ((double) totalCount);
    double aveCodeCompleteTime = ((double) totalCodeCompleteTime) / ((double) totalCount);
    double aveOpenTime = ((double) totalOpenTime) / ((double) openCount);

    System.out.println(getClass().getSimpleName() + " Summary");
    System.out.println("Lines Processed        : " + totalLineCount);
    System.out.println("Possible/All Sites     : " + percentSitesWithLetterOrDot);
    System.out.println("Possible Sites with CC : " + percentSitesWithSuggestions);
    System.out.println("Exceptions             : " + exceptionCount);
    System.out.println("Ave Resolve Lib Time   : " + aveResolveLibraryTime);
    System.out.println("Max Resolve Lib Time   : " + maxResolveLibraryTime);
    System.out.println("Ave CC Time            : " + aveCodeCompleteTime);
    System.out.println("Max CC Time            : " + maxCodeCompleteTime);
    System.out.println("# CC over " + CODE_COMPLETION_THRESHOLD + "ms        : "
        + codeCompleteOverThreshold);
    System.out.println("Ave Open Lib Time      : " + aveOpenTime);
    System.out.println("Max Open Lib Time      : " + maxOpenTime);
  }

  /**
   * Perform code completion on all compilation units in the specified library.
   */
  private void testCompletion(DartLibrary lib) throws Exception {
    CompilationUnit[] allCompUnits = lib.getCompilationUnits();
    for (CompilationUnit unit : allCompUnits) {
      testCompletion(lib, unit);
    }
  }

  /**
   * Perform code completion on all positions in the specified compilation unit. For each position,
   * truncate the line at that position and perform multiple code completions with different
   * trailing characters on that line.
   */
  private void testCompletion(DartLibrary lib, CompilationUnit unit) throws Exception {
    String srcCode = unit.getSource();
    int index = 0;
    int endOfLine = 0;
    //int lineNum = 0;
    //System.out.print(new Path(unit.getElementName()).lastSegment());
    while (endOfLine < srcCode.length()) {
      //if (++lineNum % 80 == 0) {
      //  System.out.println();
      //}
      //System.out.print('.');
      while (endOfLine < srcCode.length()) {
        char ch = srcCode.charAt(endOfLine);
        if (ch == '\r' || ch == '\n') {
          break;
        }
        endOfLine++;
      }
      while (index <= endOfLine) {
        for (String trailing : TRAILING_STRINGS) {
          String modifiedSrcCode = srcCode.substring(0, index) + trailing
              + srcCode.substring(endOfLine);
          testCompletion(lib, unit, modifiedSrcCode, index);
        }
        index++;
      }
      endOfLine++;
      totalLineCount++;
    }
    //System.out.println();
  }

  /**
   * Generate a series of code completion suggestions for the specified source at the specified
   * location within the source.
   */
  private void testCompletion(DartLibrary lib, CompilationUnit unit, String srcCode, int index)
      throws Exception {
    CompletionEngine engine;
    CompletionEnvironment environment = null;
    MockCompletionRequestor requestor = new MockCompletionRequestor();
    Hashtable<String, String> options = null;
    DartProject project = null;
    WorkingCopyOwner owner = null;
    IProgressMonitor monitor = new NullProgressMonitor();
    engine = new CompletionEngine(environment, requestor, options, project, owner, monitor);

    boolean checkForSuggestions = false;
    if (index > 0) {
      char ch = srcCode.charAt(index - 1);
      if (ch == '.' || Character.isLetter(ch)) {
        checkForSuggestions = true;
      }
    }
    unit.getBuffer().setContents(srcCode);

    long delta;
    try {
      long start = System.currentTimeMillis();
      engine.complete(unit, index, 0);
      delta = System.currentTimeMillis() - start;
    } catch (Exception ex) {
      throw new RuntimeException("Exception during code completion at <<!>> in source:\n"
          + (srcCode.substring(0, index) + "<<!>>" + srcCode.substring(index)), ex);
    }

    totalCount++;
    if (checkForSuggestions) {
      siteCount++;
    }
    exceptionCount += requestor.getExceptionCount();
    boolean hasSuggestions = requestor.validate();
    if (checkForSuggestions && hasSuggestions) {
      hasSuggestionsCount++;
    }
    totalResolveLibraryTime += requestor.getResolveLibraryTime();
    maxResolveLibraryTime = Math.max(maxResolveLibraryTime, requestor.getResolveLibraryTime());
    totalCodeCompleteTime += delta;
    maxCodeCompleteTime = Math.max(maxCodeCompleteTime, delta);
    if (delta > CODE_COMPLETION_THRESHOLD) {
      codeCompleteOverThreshold++;
      //System.out.print('x');
    }
  }

  /**
   * Open the library at the specified path, and perform code completion on each compilation unit in
   * that library.
   */
//  private void testCompletion(IPath location) throws Exception, SAXException, DartModelException,
//      Exception {
//    DartModelManager manager = DartModelManager.getInstance();
//    long start = System.currentTimeMillis();
//    DartLibrary lib = manager.openLibrary(location.toFile(), new NullProgressMonitor());
//    openCount++;
//    long delta = System.currentTimeMillis() - start;
//    totalOpenTime += delta;
//    maxOpenTime = Math.max(maxOpenTime, delta);
//    if (lib == null) {
//      fail("Failed to open library " + location);
//    }
//    testCompletion(lib);
//  }
}
