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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.context.ResolvableCompilationUnit;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.Source;

import java.util.ArrayList;
import java.util.List;

public class LibraryResolver2Test extends ResolverTestCase {

  private LibraryResolver2 resolver;
  private Source coreLibrarySource;

  @Override
  public void setUp() {
    super.setUp();
    resolver = new LibraryResolver2(analysisContext);
    coreLibrarySource = analysisContext.getSourceFactory().forUri(DartSdk.DART_CORE);
  }

  public void test_imports_relative() throws Exception {
    Source sourceA = addSource(createSource(//
        "library libA;",
        "import 'libB.dart';",
        "class A {}"));
    Source sourceB = addNamedSource("/libB.dart", createSource(//
        "library libB;",
        "import 'test.dart",
        "class B {}"));
    List<ResolvableLibrary> cycle = new ArrayList<ResolvableLibrary>();
    ResolvableLibrary coreLib = createResolvableLibrary(coreLibrarySource);
    coreLib.setLibraryElement((LibraryElementImpl) analysisContext.computeLibraryElement(coreLibrarySource));
    ResolvableLibrary libA = createResolvableLibrary(sourceA);
    ResolvableLibrary libB = createResolvableLibrary(sourceB);
    libA.setImportedLibraries(new ResolvableLibrary[] {coreLib, libB});
    libB.setImportedLibraries(new ResolvableLibrary[] {coreLib, libA});
    cycle.add(libA);
    cycle.add(libB);
    LibraryElement library = resolver.resolveLibrary(sourceA, cycle);
    LibraryElement[] importedLibraries = library.getImportedLibraries();
    assertNamedElements(importedLibraries, "dart.core", "libB");
  }

  private ResolvableLibrary createResolvableLibrary(Source source) throws Exception {
    CompilationUnit unit = analysisContext.parseCompilationUnit(source);
    ResolvableLibrary resolvableLibrary = new ResolvableLibrary(source);
    resolvableLibrary.setResolvableCompilationUnits(new ResolvableCompilationUnit[] {new ResolvableCompilationUnit(
        analysisContext.getModificationStamp(source),
        unit,
        source)});
    return resolvableLibrary;
  }
}
