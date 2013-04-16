/*
 * Copyright (c) 2012, the Dart project authors.
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
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.SourceFactory;

import java.util.ArrayList;

public class SDKAnalysisTest extends LibraryAnalysisTest {
  public void test_sdkAnalysis() throws AnalysisException {
    DartSdk sdk = DirectoryBasedDartSdk.getDefaultSdk();
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    context.setSourceFactory(sourceFactory);
    long totalTime = 0L;
    ArrayList<LibraryElement> libraries = new ArrayList<LibraryElement>();
    for (String dartUri : sdk.getUris()) {
      long startTime = System.currentTimeMillis();
      libraries.add(context.computeLibraryElement(sourceFactory.forUri(dartUri)));
      long endTime = System.currentTimeMillis();
      totalTime += endTime - startTime;
    }
    //
    // Print out timing information.
    //
    System.out.print("Resolved Dart SDK in ");
    System.out.print(totalTime);
    System.out.println(" ms");
    System.out.println();
    //
    // Print out memory usage information.
    //
//    LibraryElement[] libraryEltArray = libraries.toArray(new LibraryElement[libraries.size()]);
//    MemoryUsage usage = MemoryUtilities.measureMemoryUsage(libraryEltArray);
//    PrintWriter writer = new PrintWriter(System.out);
//    usage.writeSummary(writer);
//    writer.flush();
    //
    // Validate that there were no errors.
    //
    for (LibraryElement library : libraries) {
      verify(library);
    }
    assertValid();
  }
}
