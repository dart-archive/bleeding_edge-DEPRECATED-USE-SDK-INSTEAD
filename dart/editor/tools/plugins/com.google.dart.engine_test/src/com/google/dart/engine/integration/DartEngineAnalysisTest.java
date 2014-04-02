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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.SourceFactory;

import java.io.File;

public class DartEngineAnalysisTest extends LibraryAnalysisTest {
  public void test_dartEngineAnalysis() throws AnalysisException {
    String svnRootName = System.getProperty("svnRoot");
    assertNotNull("Missing property value: set using -DsvnRoot=...", svnRootName);
    File svnRoot = new File(svnRootName);
    assertTrue("Invalid property value: svnRoot directory does not exist", svnRoot.exists());

    DartSdk sdk = DirectoryBasedDartSdk.getDefaultSdk();
    assertNotNull(
        "Missing or invalid property value: set using -Dcom.google.dart.sdk=...",
        svnRootName);
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    context.setSourceFactory(sourceFactory);

    FileBasedSource engineSource = new FileBasedSource(new File(
        svnRoot,
        "pkg/analyzer/lib/src/generated/engine.dart"));
    long startTime = System.currentTimeMillis();
    LibraryElement library = context.computeLibraryElement(engineSource);
    verify(library);
    long endTime = System.currentTimeMillis();
    //
    // Print out timing information.
    //
    System.out.print("Resolved Dart analysis engine in ");
    System.out.print(endTime - startTime);
    System.out.println(" ms");
    System.out.println();
    printStatistics();
    //
    // Print out memory usage information.
    //
//    MemoryUsage usage = MemoryUtilities.measureMemoryUsage(library);
//    PrintWriter writer = new PrintWriter(System.out);
//    usage.writeSummary(writer);
//    writer.flush();
    //
    // Validate that there were no errors.
    //
    assertValid(context);
  }
}
