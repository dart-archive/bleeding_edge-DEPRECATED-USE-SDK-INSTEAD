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
package com.google.dart.engine.resolver;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.TypeElement;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.builder.LibraryElementBuilder;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.TypeElementImpl;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import static com.google.dart.engine.ast.ASTFactory.identifier;

import junit.framework.AssertionFailedError;

import java.io.File;
import java.util.Map;

public class ResolverTestCase extends EngineTestCase {
  /**
   * The source factory used to create {@link Source sources}.
   */
  private SourceFactory sourceFactory;

  /**
   * The error listener used during resolution.
   */
  private GatheringErrorListener errorListener;

  /**
   * The analysis context used to parse the compilation units being resolved.
   */
  private AnalysisContextImpl analysisContext;

  /**
   * Assert that the number of errors that have been gathered matches the number of errors that are
   * given and that they have the expected error codes. The order in which the errors were gathered
   * is ignored.
   * 
   * @param expectedErrorCodes the error codes of the errors that should have been gathered
   * @throws AssertionFailedError if a different number of errors have been gathered than were
   *           expected
   */
  public void assertErrors(ErrorCode... expectedErrorCodes) {
    errorListener.assertErrors(expectedErrorCodes);
  }

  @Override
  public void setUp() {
    sourceFactory = new SourceFactory(new FileUriResolver());
    errorListener = new GatheringErrorListener();
    analysisContext = new AnalysisContextImpl();
    analysisContext.setSourceFactory(sourceFactory);
  }

  /**
   * Add a source file to the content provider. The file path should be absolute.
   * 
   * @param filePath the path of the file being added
   * @param contents the contents to be returned by the content provider for the specified file
   * @return the source object representing the added file
   */
  protected Source addSource(String filePath, String contents) {
    Source source = sourceFactory.forFile(new File(filePath));
    sourceFactory.setContents(source, contents);
    return source;
  }

  /**
   * Assert that no errors have been gathered.
   * 
   * @throws AssertionFailedError if any errors have been gathered
   */
  protected void assertNoErrors() {
    errorListener.assertNoErrors();
  }

  /**
   * Create a library element that represents a library named {@code "test"} containing a single
   * empty compilation unit.
   * 
   * @return the library element that was created
   */
  protected LibraryElementImpl createTestLibrary() {
    return createTestLibrary("test");
  }

  /**
   * Create a library element that represents a library with the given name containing a single
   * empty compilation unit.
   * 
   * @param libraryName the name of the library to be created
   * @return the library element that was created
   */
  protected LibraryElementImpl createTestLibrary(String libraryName, String... typeNames) {
    int count = typeNames.length;
    CompilationUnitElementImpl[] sourcedCompilationUnits = new CompilationUnitElementImpl[count];
    for (int i = 0; i < count; i++) {
      String typeName = typeNames[i];
      TypeElementImpl type = new TypeElementImpl(identifier(typeName));
      String fileName = typeName + ".dart";
      CompilationUnitElementImpl compilationUnit = new CompilationUnitElementImpl(fileName);
      compilationUnit.setSource(sourceFactory.forFile(new File(fileName)));
      compilationUnit.setTypes(new TypeElement[] {type});
      sourcedCompilationUnits[i] = compilationUnit;
    }
    String fileName = libraryName + ".dart";
    CompilationUnitElementImpl compilationUnit = new CompilationUnitElementImpl(fileName);
    compilationUnit.setSource(sourceFactory.forFile(new File(fileName)));

    LibraryElementImpl library = new LibraryElementImpl(identifier(libraryName));
    library.setDefiningCompilationUnit(compilationUnit);
    library.setParts(sourcedCompilationUnits);
    return library;
  }

  /**
   * Given a library and all of its parts, resolve the contents of the library and the contents of
   * the parts. This assumes that the sources for the library and its parts have already been added
   * to the content provider using the method {@link #addSource(String, String)}.
   * 
   * @param librarySource the source for the compilation unit that defines the library
   * @param unitSources the sources for the compilation units that are part of the library
   * @return the error listener used while scanning, parsing and resolving the compilation units
   * @throws AnalysisException if the analysis could not be performed
   */
  protected Map<ASTNode, Element> resolve(Source librarySource, Source... unitSources)
      throws AnalysisException {
    LibraryElementBuilder builder = new LibraryElementBuilder(analysisContext, errorListener);
    LibraryElement definingLibrary = builder.buildLibrary(librarySource);
    Resolver resolver = new Resolver(
        definingLibrary,
        errorListener,
        builder.getDeclaredElementMap());
    resolver.resolve(librarySource, analysisContext.parse(librarySource, errorListener));
    for (Source unitSource : unitSources) {
      resolver.resolve(unitSource, analysisContext.parse(unitSource, errorListener));
    }
    return resolver.getResolvedElementMap();
  }

  /**
   * Verify that all of the identifiers in the compilation units associated with the given sources
   * have been resolved.
   * 
   * @param resolvedElementMap a table mapping the AST nodes that have been resolved to the element
   *          to which they were resolved
   * @param sources the sources identifying the compilation units to be verified
   * @throws Exception if the contents of the compilation unit cannot be accessed
   */
  protected void verify(Map<ASTNode, Element> resolvedElementMap, Source... sources)
      throws Exception {
    ResolutionVerifier verifier = new ResolutionVerifier(resolvedElementMap);
    for (Source source : sources) {
      analysisContext.parse(source, errorListener).accept(verifier);
    }
    verifier.assertResolved();
  }
}
