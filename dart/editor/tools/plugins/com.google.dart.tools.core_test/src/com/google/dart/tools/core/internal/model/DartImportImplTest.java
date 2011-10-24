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

import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.tools.core.test.util.MoneyProjectUtilities;

import junit.framework.TestCase;

import org.eclipse.core.resources.IResource;

import java.net.URI;

public class DartImportImplTest extends TestCase {

  public void test_DartImportImplTest_1() throws Exception {
    DartImportContainerImpl container = new DartImportContainerImpl(null);
    DartLibraryImpl lib = (DartLibraryImpl) MoneyProjectUtilities.getMoneyLibrary();
    DartImportImpl element = new DartImportImpl(container, lib.getLibrarySourceFile());
    assertTrue(element.getImportName().endsWith("money.dart"));
    IResource res = element.resource();
    assertEquals("money.dart", res.getLocation().lastSegment());
    res = element.getUnderlyingResource();
    assertEquals("money.dart", res.getLocation().lastSegment());
  }

  public void test_DartImportImplTest_core() throws Exception {
    DartImportContainerImpl container = new DartImportContainerImpl(null);
    LibrarySource libSrc = new UrlLibrarySource(new URI("dart:core"));
    DartImportImpl element = new DartImportImpl(container, libSrc);
    assertBundledLib(element, "corelib.lib");
  }

  public void test_DartImportImplTest_coreimpl() throws Exception {
    DartImportContainerImpl container = new DartImportContainerImpl(null);
    LibrarySource libSrc = new UrlLibrarySource(new URI("dart:coreimpl"));
    DartImportImpl element = new DartImportImpl(container, libSrc);
    assertBundledLib(element, "corelib_impl.lib");
  }

  public void test_DartImportImplTest_dom() throws Exception {
    DartImportContainerImpl container = new DartImportContainerImpl(null);
    LibrarySource libSrc = new UrlLibrarySource(new URI("dart:dom"));
    DartImportImpl element = new DartImportImpl(container, libSrc);
    assertBundledLib(element, "dart_dom.lib");
  }

  private void assertBundledLib(DartImportImpl element, final String expectedLibName) {
    assertTrue(
        "Expected getImportName() value to end with '" + expectedLibName + "': "
            + element.getImportName(), element.getImportName().endsWith(expectedLibName));
    assertNull(element.resource());
    assertNull(element.getUnderlyingResource());
  }
}
