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
package com.google.dart.engine.internal.sdk;

import com.google.dart.engine.EngineTestCase;

public class SDKLibrariesReaderTest extends EngineTestCase {
  public void test_readFrom_empty() {
    LibraryMap libraryMap = new SdkLibrariesReader().readFrom("");
    assertNotNull(libraryMap);
    assertEquals(0, libraryMap.size());
  }

  public void test_readFrom_nonEmpty() {
    LibraryMap libraryMap = new SdkLibrariesReader().readFrom(createSource(//
        "final Map<String, LibraryInfo> LIBRARIES = const <LibraryInfo> {",
        "  'first' : const LibraryInfo(",
        "    'first/first.dart',",
        "    category: 'First',",
        "    documented: true,",
        "    platforms: VM_PLATFORM),",
        "",
        "  'second' : const LibraryInfo(",
        "    'second/second.dart',",
        "    category: 'Second',",
        "    documented: false,",
        "    implementation: true,",
        "    platforms: 0),",
        "};"));
    assertNotNull(libraryMap);
    assertEquals(2, libraryMap.size());

    SdkLibrary first = libraryMap.getLibrary("dart:first");
    assertNotNull(first);
    assertEquals("First", first.getCategory());
    assertEquals("first/first.dart", first.getPath());
    assertEquals("dart:first", first.getShortName());
    assertEquals(false, first.isDart2JsLibrary());
    assertEquals(true, first.isDocumented());
    assertEquals(false, first.isImplementation());
    assertEquals(true, first.isVmLibrary());

    SdkLibrary second = libraryMap.getLibrary("dart:second");
    assertNotNull(second);
    assertEquals("Second", second.getCategory());
    assertEquals("second/second.dart", second.getPath());
    assertEquals("dart:second", second.getShortName());
    assertEquals(false, second.isDart2JsLibrary());
    assertEquals(false, second.isDocumented());
    assertEquals(true, second.isImplementation());
    assertEquals(false, second.isVmLibrary());
  }
}
