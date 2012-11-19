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
package com.google.dart.engine.internal.builder;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.TypeElement;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.provider.TestCompilationUnitProvider;
import com.google.dart.engine.provider.TestSourceContentProvider;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import java.io.File;

public class LibraryElementBuilderTest extends EngineTestCase {
  /**
   * The source factory used to create {@link Source sources}.
   */
  private SourceFactory sourceFactory;

  /**
   * The content provider used to provide the contents of the sources.
   */
  private TestSourceContentProvider contentProvider;

  public void fail_missingPartOfDirective() {
    Source partSource = sourceFactory.forFile(new File("/a.dart"));
    contentProvider.addSource(partSource, createSource(//
        "class A {}"));
    Source librarySource = sourceFactory.forFile(new File("/lib.dart"));
    contentProvider.addSource(librarySource, createSource(//
        "library lib;",
        "",
        "part 'a.dart';"));
    LibraryElement element = buildLibrary(
        librarySource,
        ResolverErrorCode.MISSING_PART_OF_DIRECTIVE);
    assertNotNull(element);
  }

  public void fail_partWithWrongLibraryName() {
    Source partSource = sourceFactory.forFile(new File("/a.dart"));
    contentProvider.addSource(partSource, createSource(//
        "part of other_lib;",
        "",
        "class A {}"));
    Source librarySource = sourceFactory.forFile(new File("/lib.dart"));
    contentProvider.addSource(librarySource, createSource(//
        "library lib;",
        "",
        "part 'a.dart';"));
    LibraryElement element = buildLibrary(
        librarySource,
        ResolverErrorCode.PART_WITH_WRONG_LIBRARY_NAME);
    assertNotNull(element);
  }

  @Override
  public void setUp() {
    sourceFactory = new SourceFactory(new FileUriResolver());
    contentProvider = new TestSourceContentProvider();
  }

  public void test_empty() {
    Source librarySource = sourceFactory.forFile(new File("/lib.dart"));
    contentProvider.addSource(librarySource, "library lib;");
    LibraryElement element = buildLibrary(librarySource);
    assertNotNull(element);
    assertEquals("lib", element.getName());
    assertNull(element.getEntryPoint());
    assertLength(0, element.getImportedLibraries());
    assertLength(0, element.getImports());
    assertNull(element.getLibrary());
    assertLength(0, element.getPrefixes());
    assertLength(0, element.getParts());

    CompilationUnitElement unit = element.getDefiningCompilationUnit();
    assertNotNull(unit);
    assertEquals("lib.dart", unit.getName());
    assertEquals(element, unit.getLibrary());
    assertLength(0, unit.getAccessors());
    assertLength(0, unit.getFields());
    assertLength(0, unit.getFunctions());
    assertLength(0, unit.getTypeAliases());
    assertLength(0, unit.getTypes());
  }

  public void test_invalidUri_export() {
    Source librarySource = sourceFactory.forFile(new File("/lib.dart"));
    contentProvider.addSource(librarySource, createSource(//
        "library lib;",
        "",
        "export '${'a'}.dart';"));
    LibraryElement element = buildLibrary(librarySource, ResolverErrorCode.INVALID_URI);
    assertNotNull(element);
  }

  public void test_invalidUri_import() {
    Source librarySource = sourceFactory.forFile(new File("/lib.dart"));
    contentProvider.addSource(librarySource, createSource(//
        "library lib;",
        "",
        "import '${'a'}.dart';"));
    LibraryElement element = buildLibrary(librarySource, ResolverErrorCode.INVALID_URI);
    assertNotNull(element);
  }

  public void test_invalidUri_part() {
    Source librarySource = sourceFactory.forFile(new File("/lib.dart"));
    contentProvider.addSource(librarySource, createSource(//
        "library lib;",
        "",
        "part '${'a'}.dart';"));
    LibraryElement element = buildLibrary(librarySource, ResolverErrorCode.INVALID_URI);
    assertNotNull(element);
  }

  public void test_missingLibraryDirectiveWithPart() {
    Source partSource = sourceFactory.forFile(new File("/a.dart"));
    contentProvider.addSource(partSource, createSource(//
        "part of lib;"));
    Source librarySource = sourceFactory.forFile(new File("/lib.dart"));
    contentProvider.addSource(librarySource, createSource(//
        "part 'a.dart';"));
    LibraryElement element = buildLibrary(
        librarySource,
        ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART);
    assertNotNull(element);
  }

  public void test_multipleFiles() {
    Source librarySource = sourceFactory.forFile(new File("/lib.dart"));
    Source firstSource = sourceFactory.forFile(new File("/first.dart"));
    Source secondSource = sourceFactory.forFile(new File("/second.dart"));
    contentProvider.addSource(librarySource, createSource(//
        "library lib;",
        "part 'first.dart';",
        "part 'second.dart';",
        "",
        "class A {}"));
    contentProvider.addSource(firstSource, createSource(//
        "part of lib;",
        "class B {}"));
    contentProvider.addSource(secondSource, createSource(//
        "part of lib;",
        "class C {}"));
    LibraryElement element = buildLibrary(librarySource);
    assertNotNull(element);
    CompilationUnitElement[] sourcedUnits = element.getParts();
    assertLength(2, sourcedUnits);

    assertTypes(element.getDefiningCompilationUnit(), "A");
    if (sourcedUnits[0].getName().equals("first.dart")) {
      assertTypes(sourcedUnits[0], "B");
      assertTypes(sourcedUnits[1], "C");
    } else {
      assertTypes(sourcedUnits[0], "C");
      assertTypes(sourcedUnits[1], "B");
    }
  }

  public void test_singleFile() {
    Source librarySource = sourceFactory.forFile(new File("/lib.dart"));
    contentProvider.addSource(librarySource, createSource(//
        "library lib;",
        "",
        "class A {}"));
    LibraryElement element = buildLibrary(librarySource);
    assertNotNull(element);

    assertTypes(element.getDefiningCompilationUnit(), "A");
  }

  private void assertTypes(CompilationUnitElement unit, String... typeNames) {
    assertNotNull(unit);
    TypeElement[] types = unit.getTypes();
    assertLength(typeNames.length, types);
    for (TypeElement type : types) {
      assertNotNull(type);
      String actualTypeName = type.getName();
      boolean wasExpected = false;
      for (String expectedTypeName : typeNames) {
        if (expectedTypeName.equals(actualTypeName)) {
          wasExpected = true;
        }
      }
      if (!wasExpected) {
        fail("Found unexpected type " + actualTypeName);
      }
    }
  }

  private LibraryElement buildLibrary(Source librarySource, ErrorCode... expectedErrorCodes) {
    GatheringErrorListener listener = new GatheringErrorListener();
    LibraryElementBuilder builder = new LibraryElementBuilder(new TestCompilationUnitProvider(
        contentProvider,
        listener), listener);
    LibraryElement element = builder.buildLibrary(librarySource);
    listener.assertErrors(expectedErrorCodes);
    return element;
  }
}
