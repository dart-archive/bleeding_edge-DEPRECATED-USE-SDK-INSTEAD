/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.lang.reflect.Method;

public class LibraryElementBuilderTest extends EngineTestCase {
  /**
   * The analysis context used to analyze sources.
   */
  private AnalysisContextImpl context;

  @Override
  public void setUp() {
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(
        DirectoryBasedDartSdk.getDefaultSdk()), new FileUriResolver());
    context = new AnalysisContextImpl();
    context.setSourceFactory(sourceFactory);
  }

  public void test_accessorsAcrossFiles() throws Exception {
    Source librarySource = addSource("/lib.dart", createSource(//
        "library lib;",
        "part 'first.dart';",
        "part 'second.dart';"));
    addSource("/first.dart", createSource(//
        "part of lib;",
        "int get V => 0;"));
    addSource("/second.dart", createSource(//
        "part of lib;",
        "void set V(int v) {}"));

    LibraryElement element = buildLibrary(librarySource);
    assertNotNull(element);
    CompilationUnitElement[] sourcedUnits = element.getParts();
    assertLength(2, sourcedUnits);

    PropertyAccessorElement[] firstAccessors = sourcedUnits[0].getAccessors();
    assertLength(1, firstAccessors);
    PropertyAccessorElement[] secondAccessors = sourcedUnits[1].getAccessors();
    assertLength(1, secondAccessors);
    assertSame(firstAccessors[0].getVariable(), secondAccessors[0].getVariable());
  }

  public void test_empty() throws Exception {
    Source librarySource = addSource("/lib.dart", "library lib;");

    LibraryElement element = buildLibrary(librarySource);
    assertNotNull(element);
    assertEquals("lib", element.getName());
    assertNull(element.getEntryPoint());
    assertLength(0, element.getImportedLibraries());
    assertLength(0, element.getImports());
    assertSame(element, element.getLibrary());
    assertLength(0, element.getPrefixes());
    assertLength(0, element.getParts());

    CompilationUnitElement unit = element.getDefiningCompilationUnit();
    assertNotNull(unit);
    assertEquals("lib.dart", unit.getName());
    assertEquals(element, unit.getLibrary());
    assertLength(0, unit.getAccessors());
    assertLength(0, unit.getFunctions());
    assertLength(0, unit.getFunctionTypeAliases());
    assertLength(0, unit.getTypes());
    assertLength(0, unit.getTopLevelVariables());
  }

  public void test_missingLibraryDirectiveWithPart() throws Exception {
    addSource("/a.dart", createSource(//
        "part of lib;"));
    Source librarySource = addSource("/lib.dart", createSource(//
        "part 'a.dart';"));

    LibraryElement element = buildLibrary(
        librarySource,
        ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART);
    assertNotNull(element);
  }

  public void test_missingPartOfDirective() throws Exception {
    addSource("/a.dart", "class A {}");
    Source librarySource = addSource("/lib.dart", createSource(//
        "library lib;",
        "",
        "part 'a.dart';"));

    LibraryElement element = buildLibrary(librarySource, CompileTimeErrorCode.PART_OF_NON_PART);
    assertNotNull(element);
  }

  public void test_multipleFiles() throws Exception {
    Source librarySource = addSource("/lib.dart", createSource(//
        "library lib;",
        "part 'first.dart';",
        "part 'second.dart';",
        "",
        "class A {}"));
    addSource("/first.dart", createSource(//
        "part of lib;",
        "class B {}"));
    addSource("/second.dart", createSource(//
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

  public void test_singleFile() throws Exception {
    Source librarySource = addSource("/lib.dart", createSource(//
        "library lib;",
        "",
        "class A {}"));

    LibraryElement element = buildLibrary(librarySource);
    assertNotNull(element);

    assertTypes(element.getDefiningCompilationUnit(), "A");
  }

  /**
   * Add a source file to the content provider. The file path should be absolute.
   * 
   * @param filePath the path of the file being added
   * @param contents the contents to be returned by the content provider for the specified file
   * @return the source object representing the added file
   */
  protected Source addSource(String filePath, String contents) {
    Source source = new FileBasedSource(createFile(filePath));
    context.setContents(source, contents);
    return source;
  }

  @Override
  protected void tearDown() throws Exception {
    context = null;
    super.tearDown();
  }

  /**
   * Ensure that there are elements representing all of the types in the given array of type names.
   * 
   * @param unit the compilation unit containing the types
   * @param typeNames the names of the types that should be found
   */
  private void assertTypes(CompilationUnitElement unit, String... typeNames) {
    assertNotNull(unit);
    ClassElement[] types = unit.getTypes();
    assertLength(typeNames.length, types);
    for (ClassElement type : types) {
      assertNotNull(type);
      String actualTypeName = type.getDisplayName();
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

  /**
   * Build the element model for the library whose defining compilation unit has the given source.
   * 
   * @param librarySource the source of the defining compilation unit for the library
   * @param expectedErrorCodes the errors that are expected to be found while building the element
   *          model
   * @return the element model that was built for the library
   * @throws Exception if the element model could not be built
   */
  private LibraryElement buildLibrary(Source librarySource, ErrorCode... expectedErrorCodes)
      throws Exception {
    LibraryResolver resolver = new LibraryResolver(context);
    LibraryElementBuilder builder = new LibraryElementBuilder(
        resolver.getAnalysisContext(),
        resolver.getErrorListener());
    Method method = resolver.getClass().getDeclaredMethod(
        "createLibrary",
        new Class[] {Source.class});
    method.setAccessible(true);
    Library library = (Library) method.invoke(resolver, librarySource);
    LibraryElement element = builder.buildLibrary(library);
    GatheringErrorListener listener = new GatheringErrorListener();
    listener.addAll(resolver.getErrorListener());
    listener.assertErrorsWithCodes(expectedErrorCodes);
    return element;
  }
}
