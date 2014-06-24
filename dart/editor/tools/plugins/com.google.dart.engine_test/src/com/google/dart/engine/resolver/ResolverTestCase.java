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
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.AnalysisOptionsImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.resolver.ResolutionVerifier;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;

import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.libraryIdentifier;
import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.AssertionFailedError;

public class ResolverTestCase extends EngineTestCase {
  /**
   * The analysis context used to parse the compilation units being resolved.
   */
  protected AnalysisContextImpl analysisContext;

  @Override
  public void setUp() {
    reset();
  }

  /**
   * Add a source file to the content provider. The file path should be absolute.
   * 
   * @param filePath the path of the file being added
   * @param contents the contents to be returned by the content provider for the specified file
   * @return the source object representing the added file
   */
  protected Source addNamedSource(String filePath, String contents) {
    Source source = cacheSource(filePath, contents);
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    analysisContext.applyChanges(changeSet);
    return source;
  }

  /**
   * Add a source file to the content provider.
   * 
   * @param contents the contents to be returned by the content provider for the specified file
   * @return the source object representing the added file
   */
  protected Source addSource(String contents) {
    return addNamedSource("/test.dart", contents);
  }

  /**
   * Assert that the number of errors reported against the given source matches the number of errors
   * that are given and that they have the expected error codes. The order in which the errors were
   * gathered is ignored.
   * 
   * @param source the source against which the errors should have been reported
   * @param expectedErrorCodes the error codes of the errors that should have been reported
   * @throws AnalysisException if the reported errors could not be computed
   * @throws AssertionFailedError if a different number of errors have been reported than were
   *           expected
   */
  protected void assertErrors(Source source, ErrorCode... expectedErrorCodes)
      throws AnalysisException {
    GatheringErrorListener errorListener = new GatheringErrorListener();
    for (AnalysisError error : analysisContext.computeErrors(source)) {
      errorListener.onError(error);
    }
    errorListener.assertErrorsWithCodes(expectedErrorCodes);
  }

  /**
   * Assert that no errors have been reported against the given source.
   * 
   * @param source the source against which no errors should have been reported
   * @throws AnalysisException if the reported errors could not be computed
   * @throws AssertionFailedError if any errors have been reported
   */
  protected void assertNoErrors(Source source) throws AnalysisException {
    assertErrors(source);
  }

  /**
   * Cache the source file content in the source factory but don't add the source to the analysis
   * context. The file path should be absolute.
   * 
   * @param filePath the path of the file being cached
   * @param contents the contents to be returned by the content provider for the specified file
   * @return the source object representing the cached file
   */
  protected Source cacheSource(String filePath, String contents) {
    Source source = new FileBasedSource(createFile(filePath));
    analysisContext.setContents(source, contents);
    return source;
  }

  /**
   * Create a library element that represents a library named {@code "test"} containing a single
   * empty compilation unit.
   * 
   * @return the library element that was created
   */
  protected LibraryElementImpl createDefaultTestLibrary() {
    return createTestLibrary(new AnalysisContextImpl(), "test");
  }

  /**
   * Create a library element that represents a library with the given name containing a single
   * empty compilation unit.
   * 
   * @param libraryName the name of the library to be created
   * @return the library element that was created
   */
  protected LibraryElementImpl createTestLibrary(AnalysisContext context, String libraryName,
      String... typeNames) {
    int count = typeNames.length;
    CompilationUnitElementImpl[] sourcedCompilationUnits = new CompilationUnitElementImpl[count];
    for (int i = 0; i < count; i++) {
      String typeName = typeNames[i];
      ClassElementImpl type = new ClassElementImpl(identifier(typeName));
      String fileName = typeName + ".dart";
      CompilationUnitElementImpl compilationUnit = new CompilationUnitElementImpl(fileName);
      compilationUnit.setSource(createNamedSource(fileName));
      compilationUnit.setTypes(new ClassElement[] {type});
      sourcedCompilationUnits[i] = compilationUnit;
    }
    String fileName = libraryName + ".dart";
    CompilationUnitElementImpl compilationUnit = new CompilationUnitElementImpl(fileName);
    compilationUnit.setSource(createNamedSource(fileName));

    LibraryElementImpl library = new LibraryElementImpl(context, libraryIdentifier(libraryName));
    library.setDefiningCompilationUnit(compilationUnit);
    library.setParts(sourcedCompilationUnits);
    return library;
  }

  protected Expression findTopLevelConstantExpression(CompilationUnit compilationUnit, String name) {
    for (CompilationUnitMember member : compilationUnit.getDeclarations()) {
      if (member instanceof TopLevelVariableDeclaration) {
        for (VariableDeclaration variable : ((TopLevelVariableDeclaration) member).getVariables().getVariables()) {
          if (variable.getName().getName().equals(name)) {
            return variable.getInitializer();
          }
        }
      }
    }
    return null; // Not found
  }

  protected AnalysisContext getAnalysisContext() {
    return analysisContext;
  }

  /**
   * Return a type provider that can be used to test the results of resolution.
   * 
   * @return a type provider
   * @throws AnalysisException if dart:core cannot be resolved
   */
  protected TypeProvider getTypeProvider() throws AnalysisException {
    return analysisContext.getTypeProvider();
  }

  /**
   * In the rare cases we want to group several tests into single "test_" method, so need a way to
   * reset test instance to reuse it.
   */
  protected void reset() {
    analysisContext = AnalysisContextFactory.contextWithCore();
  }

  /**
   * In the rare cases we want to group several tests into single "test_" method, so need a way to
   * reset test instance to reuse it.
   * 
   * @param options the analysis options for the context
   */
  protected void resetWithOptions(AnalysisOptions options) {
    analysisContext = AnalysisContextFactory.contextWithCoreAndOptions(options);
  }

  /**
   * Given a library and all of its parts, resolve the contents of the library and the contents of
   * the parts. This assumes that the sources for the library and its parts have already been added
   * to the content provider using the method {@link #addNamedSource(String, String)}.
   * 
   * @param librarySource the source for the compilation unit that defines the library
   * @return the element representing the resolved library
   * @throws AnalysisException if the analysis could not be performed
   */
  protected LibraryElement resolve(Source librarySource) throws AnalysisException {
    return analysisContext.computeLibraryElement(librarySource);
  }

  /**
   * Return the resolved compilation unit corresponding to the given source in the given library.
   * 
   * @param source the source of the compilation unit to be returned
   * @param library the library in which the compilation unit is to be resolved
   * @return the resolved compilation unit
   * @throws Exception if the compilation unit could not be resolved
   */
  protected CompilationUnit resolveCompilationUnit(Source source, LibraryElement library)
      throws Exception {
    return analysisContext.resolveCompilationUnit(source, library);
  }

  protected CompilationUnit resolveSource(String sourceText) throws AnalysisException {
    return resolveSource("/test.dart", sourceText);
  }

  protected CompilationUnit resolveSource(String fileName, String sourceText)
      throws AnalysisException {
    Source source = addNamedSource(fileName, sourceText);
    LibraryElement library = getAnalysisContext().computeLibraryElement(source);
    return getAnalysisContext().resolveCompilationUnit(source, library);
  }

  protected Source resolveSources(String[] sourceTexts) throws AnalysisException {
    for (int i = 0; i < sourceTexts.length; i++) {
      CompilationUnit unit = resolveSource("/lib" + (i + 1) + ".dart", sourceTexts[i]);
      // reference the source if this is the last source
      if (i + 1 == sourceTexts.length) {
        return unit.getElement().getSource();
      }
    }
    return null;
  }

  protected void resolveWithAndWithoutExperimental(String[] strSources,
      ErrorCode[] codesWithoutExperimental, ErrorCode[] codesWithExperimental) throws Exception {

    // Setup analysis context as non-experimental
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    options.setEnableDeferredLoading(false);
    resetWithOptions(options);

    // Analysis and assertions
    Source source = resolveSources(strSources);
    assertErrors(source, codesWithoutExperimental);
    verify(source);

    // Setup analysis context as experimental
    reset();

    // Analysis and assertions
    source = resolveSources(strSources);
    assertErrors(source, codesWithExperimental);
    verify(source);
  }

  @Override
  protected void tearDown() throws Exception {
    analysisContext = null;
    super.tearDown();
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
  protected void verify(Source... sources) throws Exception {
    ResolutionVerifier verifier = new ResolutionVerifier();
    for (Source source : sources) {
      analysisContext.parseCompilationUnit(source).accept(verifier);
    }
    verifier.assertResolved();
  }

  /**
   * Create a source object representing a file with the given name and give it an empty content.
   * 
   * @param fileName the name of the file for which a source is to be created
   * @return the source that was created
   */
  private FileBasedSource createNamedSource(String fileName) {
    FileBasedSource source = new FileBasedSource(createFile(fileName));
    analysisContext.setContents(source, "");
    return source;
  }
}
