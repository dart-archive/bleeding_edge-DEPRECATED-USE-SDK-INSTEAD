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
package com.google.dart.engine.context;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

/**
 * The interface {@code AnalysisContext} defines the behavior of objects that represent a context in
 * which analysis can be performed. The context includes such information as the version of the SDK
 * being analyzed against as well as the package-root used to resolve 'package:' URI's. This
 * information is included indirectly through the {@link SourceFactory source factory}.
 * <p>
 * Analysis engine allows for having more than one context. This can be used, for example, to
 * perform one analysis based on the state of files on disk and a separate analysis based on the
 * state of those files in open editors. It can also be used to perform an analysis based on a
 * proposed future state, such as after a refactoring.
 */
public interface AnalysisContext {
  /**
   * Return the element referenced by the given location.
   * 
   * @param location the reference describing the element to be returned
   * @return the element referenced by the given location
   */
  public Element getElement(ElementLocation location);

  /**
   * Return an array containing all of the errors associated with the given source.
   * 
   * @param source the source whose errors are to be returned
   * @return all of the errors associated with the given source
   * @throws AnalysisException if the errors could not be determined because the analysis could not
   *           be performed
   */
  public AnalysisError[] getErrors(Source source) throws AnalysisException;

  /**
   * Parse and build an element model for the library defined by the given source.
   * 
   * @param source the source defining the library whose element model is to be returned
   * @return the element model corresponding to the library defined by the given source
   */
  public LibraryElement getLibraryElement(Source source);

  /**
   * Return the source factory used to create the sources that can be analyzed in this context.
   * 
   * @return the source factory used to create the sources that can be analyzed in this context
   */
  public SourceFactory getSourceFactory();

  /**
   * Parse a single source to produce an AST structure.
   * 
   * @param source the source to be parsed
   * @param errorListener the listener to which errors should be reported
   * @return the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  public CompilationUnit parse(Source source, AnalysisErrorListener errorListener)
      throws AnalysisException;

  /**
   * Parse and resolve a single source within the given context to produce a fully resolved AST.
   * 
   * @param source the source to be parsed and resolved
   * @param library the library defining the context in which the source file is to be resolved
   * @param errorListener the listener to which errors should be reported
   * @return the result of resolving the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  public CompilationUnit resolve(Source source, LibraryElement library,
      AnalysisErrorListener errorListener) throws AnalysisException;

  /**
   * Scan a single source to produce a token stream.
   * 
   * @param source the source to be scanned
   * @param errorListener the listener to which errors should be reported
   * @return the head of the token stream representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  public Token scan(Source source, AnalysisErrorListener errorListener) throws AnalysisException;

  /**
   * Set the source factory used to create the sources that can be analyzed in this context to the
   * given source factory.
   * 
   * @param sourceFactory the source factory used to create the sources that can be analyzed in this
   *          context
   */
  public void setSourceFactory(SourceFactory sourceFactory);

  /**
   * Respond to the fact that the content of the given source has changed by removing any cached
   * information that might now be out-of-date.
   * 
   * @param source the source whose content has changed
   */
  public void sourceChanged(Source source);
}
