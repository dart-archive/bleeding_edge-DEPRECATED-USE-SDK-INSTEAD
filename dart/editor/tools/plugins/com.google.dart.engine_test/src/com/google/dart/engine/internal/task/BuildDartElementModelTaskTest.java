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
package com.google.dart.engine.internal.task;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.UriBasedDirective;
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.ResolvableCompilationUnit;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.resolver.ResolvableLibrary;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.CharSequenceReader;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.utilities.io.FileUtilities2;

import java.util.ArrayList;
import java.util.List;

public class BuildDartElementModelTaskTest extends EngineTestCase {
  public void test_accept() throws AnalysisException {
    AnalysisContextImpl context = AnalysisContextFactory.contextWithCore();
    BuildDartElementModelTask task = new BuildDartElementModelTask(
        context,
        null,
        new ArrayList<ResolvableLibrary>());
    assertTrue(task.accept(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitBuildDartElementModelTask(BuildDartElementModelTask task)
          throws AnalysisException {
        return true;
      }
    }));
  }

  public void test_getErrors() {
    AnalysisContextImpl context = AnalysisContextFactory.contextWithCore();
    BuildDartElementModelTask task = new BuildDartElementModelTask(
        context,
        null,
        new ArrayList<ResolvableLibrary>());
    assertLength(0, task.getErrorListener().getErrors());
  }

  public void test_getLibrariesInCycle() {
    AnalysisContextImpl context = AnalysisContextFactory.contextWithCore();
    ArrayList<ResolvableLibrary> librariesInCycle = new ArrayList<ResolvableLibrary>();
    BuildDartElementModelTask task = new BuildDartElementModelTask(context, null, librariesInCycle);
    assertSame(librariesInCycle, task.getLibrariesInCycle());
  }

  public void test_perform_multiple() throws AnalysisException {
    AnalysisContextImpl context = AnalysisContextFactory.contextWithCore();

    ResolvableLibrary lib3 = createLibrary(
        new ResolvableCompilationUnit[] {createUnit(
            context,
            "/lib3.dart",
            createSource("library lib3;", "import 'lib1.dart';", "class C { A a; }"))},
        null);
    ResolvableLibrary lib2 = createLibrary(
        new ResolvableCompilationUnit[] {createUnit(
            context,
            "/lib2.dart",
            createSource("library lib2;", "import 'lib3.dart';", "class B { C c; }"))},
        new ResolvableLibrary[] {createCoreLibrary(context), lib3});
    ResolvableLibrary lib1 = createLibrary(
        new ResolvableCompilationUnit[] {createUnit(
            context,
            "/lib1.dart",
            createSource("library lib1;", "import 'lib2.dart';", "class A { B b; }"))},
        new ResolvableLibrary[] {createCoreLibrary(context), lib2});
    lib3.setImportedLibraries(new ResolvableLibrary[] {createCoreLibrary(context), lib1});

    ArrayList<ResolvableLibrary> librariesInCycle = new ArrayList<ResolvableLibrary>();
    librariesInCycle.add(lib1);
    librariesInCycle.add(lib2);
    librariesInCycle.add(lib3);

    BuildDartElementModelTask task = new BuildDartElementModelTask(context, null, librariesInCycle);
    task.perform(new TestTaskVisitor<Void>() {
      @Override
      public Void visitBuildDartElementModelTask(BuildDartElementModelTask task)
          throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertLength(0, task.getErrorListener().getErrors());
        List<ResolvableLibrary> librariesInCycle = task.getLibrariesInCycle();
        assertSizeOfList(3, librariesInCycle);
        for (int i = 0; i < 3; i++) {
          ResolvableLibrary library = librariesInCycle.get(i);
          LibraryElementImpl libraryElement = library.getLibraryElement();
          assertNotNull(libraryElement);
          CompilationUnitElement unitElement = libraryElement.getDefiningCompilationUnit();
          assertNotNull(unitElement);
          ClassElement[] types = unitElement.getTypes();
          assertLength(1, types);
        }
        return null;
      }
    });
  }

  public void test_perform_single_noParts() throws AnalysisException {
    AnalysisContextImpl context = AnalysisContextFactory.contextWithCore();

    ResolvableLibrary lib1 = createLibrary(
        new ResolvableCompilationUnit[] {createUnit(
            context,
            "/lib1.dart",
            createSource("library lib1;", "class A {}", "class B extends A {}"))},
        new ResolvableLibrary[] {createCoreLibrary(context)});

    ArrayList<ResolvableLibrary> librariesInCycle = new ArrayList<ResolvableLibrary>();
    librariesInCycle.add(lib1);

    BuildDartElementModelTask task = new BuildDartElementModelTask(context, null, librariesInCycle);
    task.perform(new TestTaskVisitor<Void>() {
      @Override
      public Void visitBuildDartElementModelTask(BuildDartElementModelTask task)
          throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertLength(0, task.getErrorListener().getErrors());
        List<ResolvableLibrary> librariesInCycle = task.getLibrariesInCycle();
        assertSizeOfList(1, librariesInCycle);
        ResolvableLibrary library = librariesInCycle.get(0);
        LibraryElementImpl libraryElement = library.getLibraryElement();
        assertNotNull(libraryElement);
        CompilationUnitElement unitElement = libraryElement.getDefiningCompilationUnit();
        assertNotNull(unitElement);
        ClassElement[] types = unitElement.getTypes();
        assertLength(2, types);
        InterfaceType supertype = types[1].getSupertype();
        assertNotNull(supertype);
        assertSame(types[0], supertype.getElement());
        return null;
      }
    });
  }

  public void test_perform_single_parts() throws AnalysisException {
    AnalysisContextImpl context = AnalysisContextFactory.contextWithCore();

    ResolvableLibrary lib1 = createLibrary(
        new ResolvableCompilationUnit[] {
            createUnit(
                context,
                "/lib1.dart",
                createSource(
                    "library lib1;",
                    "part 'part1-1.dart';",
                    "part 'part1-2.dart';",
                    "class A {}",
                    "class B extends A {}")),
            createUnit(
                context,
                "/part1-1.dart",
                createSource("part of lib1;", "class C extends B {}")),
            createUnit(
                context,
                "/part1-2.dart",
                createSource("part of lib1;", "class D implements A {}"))},
        new ResolvableLibrary[] {createCoreLibrary(context)});

    ArrayList<ResolvableLibrary> librariesInCycle = new ArrayList<ResolvableLibrary>();
    librariesInCycle.add(lib1);

    BuildDartElementModelTask task = new BuildDartElementModelTask(context, null, librariesInCycle);
    task.perform(new TestTaskVisitor<Void>() {
      @Override
      public Void visitBuildDartElementModelTask(BuildDartElementModelTask task)
          throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertLength(0, task.getErrorListener().getErrors());
        List<ResolvableLibrary> librariesInCycle = task.getLibrariesInCycle();
        assertSizeOfList(1, librariesInCycle);
        ResolvableLibrary library = librariesInCycle.get(0);
        LibraryElementImpl libraryElement = library.getLibraryElement();
        assertNotNull(libraryElement);
        CompilationUnitElement definingUnit = libraryElement.getDefiningCompilationUnit();
        assertNotNull(definingUnit);
        ClassElement[] definingTypes = definingUnit.getTypes();
        assertLength(2, definingTypes);

        CompilationUnitElement[] parts = libraryElement.getParts();
        assertNotNull(parts);
        assertLength(2, parts);
        ClassElement[] types = parts[0].getTypes();
        assertLength(1, types);
        InterfaceType supertype = types[0].getSupertype();
        assertNotNull(supertype);
        assertSame(definingTypes[1], supertype.getElement());

        types = parts[1].getTypes();
        assertLength(1, types);
        InterfaceType implementedType = types[0].getInterfaces()[0];
        assertNotNull(implementedType);
        assertSame(definingTypes[0], implementedType.getElement());
        return null;
      }
    });
  }

  /**
   * Create a resolvable library representing the core library.
   * 
   * @param context the context used to build the library
   * @return the resolvable library representing the core library
   * @throws AnalysisException if the core library has not been resolved
   */
  private ResolvableLibrary createCoreLibrary(AnalysisContextImpl context) throws AnalysisException {
    Source coreSource = context.getSourceFactory().forUri(DartSdk.DART_CORE);
    ResolvableLibrary coreLibrary = new ResolvableLibrary(coreSource);
    coreLibrary.setLibraryElement((LibraryElementImpl) context.computeLibraryElement(coreSource));
    return coreLibrary;
  }

  /**
   * Create a resolvable library with the given compilation units and imports.
   * 
   * @param units the compilation units in the library, with the defining compilation unit first
   * @param imports the libraries imported by the library (including the core library)
   * @return the resolvable library that was created
   */
  private ResolvableLibrary createLibrary(ResolvableCompilationUnit[] units,
      ResolvableLibrary[] imports) {
    ResolvableLibrary library = new ResolvableLibrary(units[0].getSource());
    library.setImportedLibraries(imports);
    library.setResolvableCompilationUnits(units);
    return library;
  }

  /**
   * Return a resolvable compilation unit representing the file with the given name and contents.
   * 
   * @param fileName the name of the file being represented
   * @param contents the contents of the file being represented
   * @return a resolvable compilation unit representing the file
   */
  private ResolvableCompilationUnit createUnit(AnalysisContextImpl context, String fileName,
      String contents) {
    Source source = new FileBasedSource(FileUtilities2.createFile(fileName));
    context.setContents(source, contents);
    return new ResolvableCompilationUnit(source.getModificationStamp(), parse(
        context,
        source,
        contents), source);
  }

  /**
   * Return the result of parsing the given source.
   * 
   * @param source the source being parsed
   * @param contents the contents of the source
   * @return the result of parsing the given source
   */
  private CompilationUnit parse(AnalysisContextImpl context, Source source, String contents) {
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scanner scanner = new Scanner(source, new CharSequenceReader(contents), errorListener);
    Parser parser = new Parser(source, errorListener);
    CompilationUnit unit = parser.parseCompilationUnit(scanner.tokenize());
    for (Directive directive : unit.getDirectives()) {
      if (directive instanceof UriBasedDirective) {
        UriBasedDirective uriDirective = (UriBasedDirective) directive;
        ParseDartTask.resolveDirective(context, source, uriDirective, errorListener);
      }
    }
    return unit;
  }
}
