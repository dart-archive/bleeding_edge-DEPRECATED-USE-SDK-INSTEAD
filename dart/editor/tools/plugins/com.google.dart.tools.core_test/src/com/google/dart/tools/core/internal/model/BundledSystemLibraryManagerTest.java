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
package com.google.dart.tools.core.internal.model;

import com.google.dart.compiler.SystemLibraryManager;

import junit.framework.TestCase;

import java.net.URI;

public class BundledSystemLibraryManagerTest extends TestCase {
  private SystemLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();

  public void testExpand1() throws Exception {
    URI shortUri = new URI("dart:core");
    URI fullUri = libraryManager.expandRelativeDartUri(shortUri);
    assertNotNull(fullUri);
    assertEquals("dart", fullUri.getScheme());
    assertEquals("core", fullUri.getHost());
    assertTrue(fullUri.getPath().endsWith("/corelib.lib"));
  }

  public void testExpand2() throws Exception {
    URI shortUri = new URI("dart:coreimpl");
    URI fullUri = libraryManager.expandRelativeDartUri(shortUri);
    assertNotNull(fullUri);
    assertEquals("dart", fullUri.getScheme());
    assertEquals("core", fullUri.getHost());
    assertTrue(fullUri.getPath().endsWith("/corelib_impl.lib"));
  }

  public void testExpand3() throws Exception {
    URI shortUri = new URI("dart:dom");
    URI fullUri = libraryManager.expandRelativeDartUri(shortUri);
    assertNotNull(fullUri);
    assertEquals("dart", fullUri.getScheme());
    assertEquals("dom", fullUri.getHost());
    assertTrue(fullUri.getPath().endsWith("/dart_dom.lib"));
  }

  public void testExpand4() throws Exception {
    URI shortUri = new URI("dart:coreimpl");
    URI fullUri1 = libraryManager.expandRelativeDartUri(shortUri);
    URI fullUri2 = libraryManager.expandRelativeDartUri(fullUri1);
    assertNotNull(fullUri2);
    assertEquals("dart", fullUri2.getScheme());
    assertEquals("core", fullUri2.getHost());
    assertTrue(fullUri2.getPath().endsWith("/corelib_impl.lib"));
  }

  public void testExpand5() throws Exception {
    URI shortUri = new URI("dart:doesnotexist.lib");
    try {
      URI fullUri = libraryManager.expandRelativeDartUri(shortUri);
      fail("Expected expansion of " + shortUri + " to fail, but returned " + fullUri);
    } catch (RuntimeException e) {
      String message = e.getMessage();
      assertTrue(message.startsWith("No system library"));
      assertTrue(message.contains(shortUri.toString()));
    }
  }

  public void testTranslate1() throws Exception {
    URI shortUri = new URI("dart:core");
    URI fullUri = libraryManager.expandRelativeDartUri(shortUri);
    URI translatedURI = libraryManager.translateDartUri(fullUri);
    assertNotNull(translatedURI);
    String scheme = translatedURI.getScheme();
    assertTrue(scheme.equals("file") || scheme.equals("jar"));
    assertTrue(translatedURI.getPath().endsWith("/corelib.lib"));
  }

  public void testTranslate2() throws Exception {
    URI shortUri = new URI("dart:coreimpl");
    URI fullUri = libraryManager.expandRelativeDartUri(shortUri);
    URI translatedURI = libraryManager.translateDartUri(fullUri);
    assertNotNull(translatedURI);
    String scheme = translatedURI.getScheme();
    assertTrue(scheme.equals("file") || scheme.equals("jar"));
    assertTrue(translatedURI.getPath().endsWith("/corelib_impl.lib"));
  }

  public void testTranslate3() throws Exception {
    URI fullUri = new URI("dart://doesnotexist/some/file.dart");
    try {
      URI translatedURI = libraryManager.translateDartUri(fullUri);
      fail("Expected translate " + fullUri + " to fail, but returned " + translatedURI);
    } catch (RuntimeException e) {
      String message = e.getMessage();
      assertTrue(message.startsWith("No system library"));
      assertTrue(message.contains(fullUri.toString()));
    }
  }
}
