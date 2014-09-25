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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;

public class LibraryResolverTest extends ResolverTestCase {

  private LibraryResolver resolver;

  @Override
  public void setUp() {
    super.setUp();
    resolver = new LibraryResolver(analysisContext);
  }

  public void test_imports_dart_html() throws Exception {
    Source source = addSource(createSource(//
        "library libA;",
        "import 'dart:html';",
        "class A {}"));
    LibraryElement library = resolver.resolveLibrary(source, true);
    LibraryElement[] importedLibraries = library.getImportedLibraries();
    assertNamedElements(importedLibraries, "dart.core", "dart.dom.html");
  }

  public void test_imports_none() throws Exception {
    Source source = addSource(createSource(//
        "library libA;",
        "class A {}"));
    LibraryElement library = resolver.resolveLibrary(source, true);
    LibraryElement[] importedLibraries = library.getImportedLibraries();
    assertNamedElements(importedLibraries, "dart.core");
  }

  public void test_imports_relative() throws Exception {
    addNamedSource("/libB.dart", "library libB;");
    Source source = addSource(createSource(//
        "library libA;",
        "import 'libB.dart';",
        "class A {}"));
    LibraryElement library = resolver.resolveLibrary(source, true);
    LibraryElement[] importedLibraries = library.getImportedLibraries();
    assertNamedElements(importedLibraries, "dart.core", "libB");
  }
}
