/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.source.Source;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class {@code ContainingLibrariesVerifier} verify that the list of containing
 * libraries that is computed for each source is consistent with the list of parts included in each
 * library.
 */
public class ContainingLibrariesVerifier {
  /**
   * A table mapping sources to a list of the sources of the containing libraries of the keys.
   */
  private HashMap<Source, ArrayList<Source>> expectedValues = new HashMap<Source, ArrayList<Source>>();

  /**
   * Initialize a newly created containing libraries verifier.
   */
  public ContainingLibrariesVerifier() {
    super();
  }

  /**
   * Add the information for the defining compilation unit of a library.
   * 
   * @param library the library for which information is being added
   */
  public void addLibrary(CompilationUnitElement library) {
    Source librarySource = library.getSource();
    ArrayList<Source> containingLibraries = new ArrayList<Source>();
    containingLibraries.add(librarySource);
    expectedValues.put(librarySource, containingLibraries);
  }

  /**
   * Add the information for a part in the given library.
   * 
   * @param part the part for which information is being added
   * @param library the library containing the part
   */
  public void addPart(CompilationUnitElement part, CompilationUnitElement library) {
    Source partSource = part.getSource();
    Source librarySource = library.getSource();
    ArrayList<Source> containingLibraries = expectedValues.get(partSource);
    if (containingLibraries == null) {
      containingLibraries = new ArrayList<Source>();
      expectedValues.put(partSource, containingLibraries);
    }
    containingLibraries.add(librarySource);
  }

  /**
   * Assert that the information obtained from the given context about which libraries contain which
   * parts matches what we expect to find.
   * 
   * @param context the context whose state is being tested
   */
  public void assertValid(AnalysisContext context) {
    for (Map.Entry<Source, ArrayList<Source>> entry : expectedValues.entrySet()) {
      Source source = entry.getKey();
      ArrayList<Source> expectedLibraries = entry.getValue();
      Source[] actualLibraries = context.getLibrariesContaining(source);
      int actualCount = actualLibraries.length;
      Assert.assertEquals(expectedLibraries.size(), actualCount);
      for (int i = 0; i < actualCount; i++) {
        Assert.assertTrue(expectedLibraries.contains(actualLibraries[i]));
      }
    }
  }
}
