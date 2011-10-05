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
package com.google.dart.tools.core.internal.model.info;

import com.google.dart.tools.core.model.DartLibrary;

import junit.framework.TestCase;

public class HTMLFileInfoTest extends TestCase {
  public void test_HTMLFileInfo() {
    HTMLFileInfo result = new HTMLFileInfo();
    assertNotNull(result);
  }

  public void test_HTMLFileInfo_getReferencedLibraries() {
    HTMLFileInfo info = new HTMLFileInfo();
    DartLibrary[] libraries = info.getReferencedLibraries();
    assertNotNull(libraries);
    assertEquals(0, libraries.length);
  }

  public void test_HTMLFileInfo_setReferencedLibraries() {
    HTMLFileInfo info = new HTMLFileInfo();
    DartLibrary[] libraries = new DartLibrary[0];
    info.setReferencedLibraries(libraries);
    DartLibrary[] result = info.getReferencedLibraries();
    assertNotNull(result);
    assertEquals(libraries, result);
  }
}
