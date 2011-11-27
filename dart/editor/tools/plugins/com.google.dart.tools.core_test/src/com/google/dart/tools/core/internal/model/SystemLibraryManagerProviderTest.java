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

import com.google.dart.compiler.SystemLibraryManager;

import junit.framework.TestCase;

import java.net.URI;
import java.util.Collection;

public class SystemLibraryManagerProviderTest extends TestCase {
  private SystemLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();

  public void test_SystemLibraryManagerProvider_expandCore() throws Exception {
    URI shortUri = new URI("dart:core");
    URI fullUri = libraryManager.expandRelativeDartUri(shortUri);
    assertNotNull(fullUri);
    assertEquals("dart", fullUri.getScheme());
    assertEquals("core", fullUri.getHost());
    assertTrue(fullUri.getPath().endsWith("/corelib.dart"));
    URI shortUri2 = SystemLibraryManagerProvider.getShortUri(fullUri);
    assertEquals(shortUri, shortUri2);
  }

  public void test_SystemLibraryManagerProvider_expandCoreImpl() throws Exception {
    URI shortUri = new URI("dart:core_impl");
    URI fullUri1 = libraryManager.expandRelativeDartUri(shortUri);
    assertNotNull(fullUri1);
    assertEquals("dart", fullUri1.getScheme());
    assertEquals("core", fullUri1.getHost());
    assertTrue(fullUri1.getPath().endsWith("/corelib_impl.dart"));
    URI fullUri2 = libraryManager.expandRelativeDartUri(fullUri1);
    assertEquals(fullUri1, fullUri2);
    URI shortUri2 = SystemLibraryManagerProvider.getShortUri(fullUri1);
    assertEquals(shortUri, shortUri2);
  }

  public void test_SystemLibraryManagerProvider_expandDoesNotExist() throws Exception {
    URI shortUri = new URI("dart:doesnotexist.lib");
    URI fullUri = libraryManager.expandRelativeDartUri(shortUri);
    assertNull(fullUri);
  }

  public void test_SystemLibraryManagerProvider_expandDom() throws Exception {
    URI shortUri = new URI("dart:dom");
    URI fullUri = libraryManager.expandRelativeDartUri(shortUri);
    assertNotNull(fullUri);
    assertEquals("dart", fullUri.getScheme());
    assertEquals("dom", fullUri.getHost());
    assertTrue(fullUri.getPath().endsWith("/dom.dart"));
    URI shortUri2 = SystemLibraryManagerProvider.getShortUri(fullUri);
    assertEquals(shortUri, shortUri2);
  }

  public void test_SystemLibraryManagerProvider_getAllLibrarySpecs() {
    Collection<String> specs = SystemLibraryManagerProvider.getAllLibrarySpecs();
    assertNotNull(specs);
    assertTrue(specs.contains("dart:core"));
    assertTrue(specs.contains("dart:core_impl"));
    assertTrue(specs.contains("dart:html"));
  }

  public void test_SystemLibraryManagerProvider_translateCore() throws Exception {
    URI shortUri = new URI("dart:core");
    URI fullUri = libraryManager.expandRelativeDartUri(shortUri);
    URI translatedURI = libraryManager.translateDartUri(fullUri);
    assertNotNull(translatedURI);
    String scheme = translatedURI.getScheme();
    assertTrue(scheme.equals("file") || scheme.equals("jar"));
    assertTrue(translatedURI.getPath().endsWith("/corelib.dart"));
    URI shortUri2 = SystemLibraryManagerProvider.getShortUri(translatedURI);
    assertEquals(shortUri, shortUri2);
  }

  public void test_SystemLibraryManagerProvider_translateCoreImpl() throws Exception {
    URI shortUri = new URI("dart:core_impl");
    URI fullUri = libraryManager.expandRelativeDartUri(shortUri);
    URI translatedURI = libraryManager.translateDartUri(fullUri);
    assertNotNull(translatedURI);
    String scheme = translatedURI.getScheme();
    assertTrue(scheme.equals("file") || scheme.equals("jar"));
    assertTrue(translatedURI.getPath().endsWith("/corelib_impl.dart"));
    URI shortUri2 = SystemLibraryManagerProvider.getShortUri(translatedURI);
    assertEquals(shortUri, shortUri2);
  }

  public void test_SystemLibraryManagerProvider_translateDoesNotExist() throws Exception {
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
