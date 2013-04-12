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
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.resolver.LibraryResolver;
import com.google.dart.engine.internal.resolver.ResolutionVerifier;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.resolver.TypeProviderImpl;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.ast.ASTFactory.libraryIdentifier;
import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.AssertionFailedError;

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
    errorListener = new GatheringErrorListener();
    analysisContext = AnalysisContextFactory.contextWithCore();
    sourceFactory = analysisContext.getSourceFactory();
  }

  /**
   * Add a source file to the content provider. The file path should be absolute.
   * 
   * @param filePath the path of the file being added
   * @param contents the contents to be returned by the content provider for the specified file
   * @return the source object representing the added file
   */
  protected Source addSource(String filePath, String contents) {
    Source source = new FileBasedSource(sourceFactory.getContentCache(), createFile(filePath));
    sourceFactory.setContents(source, contents);
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source);
    analysisContext.applyChanges(changeSet);
    return source;
  }

  /**
   * Assert that no errors have been gathered.
   * 
   * @throws AssertionFailedError if any errors have been gathered
   */
  protected void assertNoErrors() {
    // TODO(brianwilkerson) This method no longer does anything because the error listener is never
    // used. We need to pass in a list of libraries and ask the context for the errors associated
    // with the compilation units in those libraries.
    errorListener.assertNoErrors();
  }

  /**
   * Create a library element that represents a library named {@code "test"} containing a single
   * empty compilation unit.
   * 
   * @return the library element that was created
   */
  protected LibraryElementImpl createTestLibrary() {
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
      compilationUnit.setSource(createSource(fileName));
      compilationUnit.setTypes(new ClassElement[] {type});
      sourcedCompilationUnits[i] = compilationUnit;
    }
    String fileName = libraryName + ".dart";
    CompilationUnitElementImpl compilationUnit = new CompilationUnitElementImpl(fileName);
    compilationUnit.setSource(createSource(fileName));

    LibraryElementImpl library = new LibraryElementImpl(context, libraryIdentifier(libraryName));
    library.setDefiningCompilationUnit(compilationUnit);
    library.setParts(sourcedCompilationUnits);
    return library;
  }

  protected AnalysisContext getAnalysisContext() {
    return analysisContext;
  }

  protected GatheringErrorListener getErrorListener() {
    return errorListener;
  }

  protected SourceFactory getSourceFactory() {
    return sourceFactory;
  }

  /**
   * Return a type provider that can be used to test the results of resolution.
   * 
   * @return a type provider
   */
  protected TypeProvider getTypeProvider() {
    Source coreSource = analysisContext.getSourceFactory().forUri(DartSdk.DART_CORE);
    LibraryElement coreElement = analysisContext.getLibraryElement(coreSource);
    return new TypeProviderImpl(coreElement);
  }

  /**
   * Given a library and all of its parts, resolve the contents of the library and the contents of
   * the parts. This assumes that the sources for the library and its parts have already been added
   * to the content provider using the method {@link #addSource(String, String)}.
   * 
   * @param librarySource the source for the compilation unit that defines the library
   * @param unitSources the sources for the compilation units that are part of the library
   * @return the element representing the resolved library
   * @throws AnalysisException if the analysis could not be performed
   */
  protected LibraryElement resolve(Source librarySource, Source... unitSources)
      throws AnalysisException {
    LibraryResolver resolver = new LibraryResolver(analysisContext, errorListener);
    return resolver.resolveLibrary(librarySource, true);
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
  private FileBasedSource createSource(String fileName) {
    FileBasedSource source = new FileBasedSource(
        sourceFactory.getContentCache(),
        createFile(fileName));
    sourceFactory.setContents(source, "");
    return source;
  }
}
