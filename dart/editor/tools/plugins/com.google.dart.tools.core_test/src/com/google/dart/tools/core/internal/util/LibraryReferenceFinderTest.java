/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.util;

import com.google.dart.tools.core.test.util.HTMLFactory;

import junit.framework.TestCase;

import java.util.List;

public class LibraryReferenceFinderTest extends TestCase {
  public void test_LibraryReferenceFinder() {
    LibraryReferenceFinder finder = new LibraryReferenceFinder();
    assertNotNull(finder);
  }

  public void test_LibraryReferenceFinder_malformed() throws Exception {
    // issue 6651
    String malformedHtml = "<html><body>"
        + "\n    <script type=\"application/dart\" src=\"bug.dart\"> /script>" + "\n</body></html>"
        + "\n/Network/Servers/hdserver1.hdinternal.net/Users/egrimes/dart/bug/web/bug.html";
    LibraryReferenceFinder finder = new LibraryReferenceFinder();
    finder.processHTML(malformedHtml);
    List<String> libraries = finder.getLibraryList();
    assertNotNull(libraries);
    assertEquals(1, libraries.size());
  }

  public void test_LibraryReferenceFinder_malformed2() throws Exception {
    // issue 6651
    String malformedHtml = "<html><body><script type=\"application/dart\" src=\"bug.dart\"";
    LibraryReferenceFinder finder = new LibraryReferenceFinder();
    finder.processHTML(malformedHtml);
    List<String> libraries = finder.getLibraryList();
    assertNotNull(libraries);
    assertEquals(0, libraries.size());
  }

  public void test_LibraryReferenceFinder_noScripts() {
    LibraryReferenceFinder finder = new LibraryReferenceFinder();
    finder.processHTML(HTMLFactory.noScripts());
    List<String> libraries = finder.getLibraryList();
    assertNotNull(libraries);
    assertEquals(0, libraries.size());
  }

  public void test_LibraryReferenceFinder_twoScripts() {
    LibraryReferenceFinder finder = new LibraryReferenceFinder();
    finder.processHTML(HTMLFactory.allScriptTypes());
    List<String> libraries = finder.getLibraryList();
    assertNotNull(libraries);
    assertEquals(2, libraries.size());
    assertTrue("Missing special.dart", libraries.contains("special.dart"));
    assertTrue("Missing main.dart", libraries.contains("main.dart"));
  }
}
