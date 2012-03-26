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
package com.google.dart.tools.core.internal.model;

import junit.framework.TestCase;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

public abstract class SystemLibraryManagerProviderTest extends TestCase {

  public void test_SystemLibraryManagerProvider_expandDoesNotExist() throws Exception {
    URI shortUri = new URI("dart:doesnotexist.lib");
    URI fullUri = getLibraryManager().expandRelativeDartUri(shortUri);
    assertNull(fullUri);
  }

  public void test_SystemLibraryManagerProvider_getAllLibrarySpecs() throws Exception {
    EditorLibraryManager libraryManager = getLibraryManager();
    Collection<String> specs = libraryManager.getAllLibrarySpecs();
//    System.out.println(getClass().getName());
//    System.out.println("  " + specs.size() + " system libraries");
//    for (String eachSpec : new TreeSet<String>(specs)) {
//      URI uri;
//      try {
//        uri = libraryManager.resolveDartUri(new URI(eachSpec));
//      } catch (Exception e) {
//        System.out.println("Failed to resolve " + eachSpec);
//        e.printStackTrace();
//        continue;
//      }
//      StringBuilder builder = new StringBuilder();
//      builder.append("  ");
//      builder.append(eachSpec);
//      while (builder.length() < 20) {
//        builder.append(' ');
//      }
//      builder.append(" --> ");
//      builder.append(uri);
//      System.out.println(builder.toString());
//    }
    assertNotNull(specs);
    assertTrue(specs.contains("dart:core"));
    assertTrue(specs.contains("dart:coreimpl"));
    assertTrue(specs.contains("dart:dom"));
    assertTrue(specs.contains("dart:html"));
    assertTrue(specs.contains("dart:uri"));
  }

  public void test_SystemLibraryManagerProvider_translateDoesNotExist() throws Exception {
    URI fullUri = new URI("dart://doesnotexist/some/file.dart");
    try {
      URI translatedURI = getLibraryManager().translateDartUri(fullUri);
      fail("Expected translate " + fullUri + " to fail, but returned " + translatedURI);
    } catch (RuntimeException e) {
      String message = e.getMessage();
      assertTrue(message.startsWith("No system library"));
      assertTrue(message.contains(fullUri.toString()));
    }
  }

  protected abstract EditorLibraryManager getLibraryManager();

  protected void testLibrary(String shortLibName, String libFileName) throws URISyntaxException,
      AssertionError {
    final URI shortUri = new URI("dart:" + shortLibName);

    final URI fullUri1 = getLibraryManager().expandRelativeDartUri(shortUri);
    assertNotNull(fullUri1);
    assertEquals("dart", fullUri1.getScheme());
    assertEquals(shortLibName, fullUri1.getHost());
    assertTrue(fullUri1.getPath(), fullUri1.getPath().endsWith("/" + libFileName));
    URI fullUri2 = getLibraryManager().expandRelativeDartUri(fullUri1);
    assertEquals(fullUri1, fullUri2);
    URI shortUri2 = getLibraryManager().getShortUri(fullUri1);
    assertEquals(shortUri, shortUri2);

    URI translatedUri = getLibraryManager().translateDartUri(fullUri1);
    assertNotNull(translatedUri);
    String scheme = translatedUri.getScheme();
    assertTrue(scheme.equals("file"));
    assertTrue(translatedUri.getPath().endsWith("/" + libFileName));
    URI shortUri3 = getLibraryManager().getShortUri(translatedUri);
    assertEquals(shortUri, shortUri3);
  }
}
