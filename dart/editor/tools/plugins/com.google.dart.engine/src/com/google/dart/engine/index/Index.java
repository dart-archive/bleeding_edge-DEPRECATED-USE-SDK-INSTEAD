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
package com.google.dart.engine.index;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

/**
 * The interface {@link Index} defines the behavior of objects that maintain an index storing
 * {@link Relationship relationships} between {@link Element elements}. All of the operations
 * defined on the index are asynchronous, and results, when there are any, are provided through a
 * callback.
 * <p>
 * Despite being asynchronous, the results of the operations are guaranteed to be consistent with
 * the expectation that operations are performed in the order in which they are requested.
 * Modification operations are executed before any read operation. There is no guarantee about the
 * order in which the callbacks for read operations will be invoked.
 * 
 * @coverage dart.engine.index
 */
public interface Index {
  /**
   * Asynchronously invoke the given callback with an array containing all of the locations of the
   * elements that have the given relationship with the given element. For example, if the element
   * represents a method and the relationship is the is-referenced-by relationship, then the
   * locations that will be passed into the callback will be all of the places where the method is
   * invoked.
   * 
   * @param element the element that has the relationship with the locations to be returned
   * @param relationship the relationship between the given element and the locations to be returned
   * @param callback the callback that will be invoked when the locations are found
   */
  void getRelationships(Element element, Relationship relationship, RelationshipCallback callback);

  /**
   * Answer index statistics.
   */
  String getStatistics();

  /**
   * Asynchronously process the given {@link HtmlUnit} in order to record the relationships.
   * 
   * @param context the {@link AnalysisContext} in which {@link HtmlUnit} was resolved
   * @param unit the {@link HtmlUnit} being indexed
   */
  void indexHtmlUnit(AnalysisContext context, HtmlUnit unit);

  /**
   * Asynchronously process the given {@link CompilationUnit} in order to record the relationships.
   * 
   * @param context the {@link AnalysisContext} in which {@link CompilationUnit} was resolved
   * @param unit the {@link CompilationUnit} being indexed
   */
  void indexUnit(AnalysisContext context, CompilationUnit unit);

  /**
   * Asynchronously remove from the index all of the information associated with the given context.
   * <p>
   * This method should be invoked when a context is disposed.
   * 
   * @param context the {@link AnalysisContext} to remove
   */
  void removeContext(AnalysisContext context);

  /**
   * Asynchronously remove from the index all of the information associated with elements or
   * locations in the given source. This includes relationships between an element in the given
   * source and any other locations, relationships between any other elements and a location within
   * the given source.
   * <p>
   * This method should be invoked when a source is no longer part of the code base.
   * 
   * @param context the {@link AnalysisContext} in which {@link Source} being removed
   * @param source the {@link Source} being removed
   */
  void removeSource(AnalysisContext context, Source source);

  /**
   * Asynchronously remove from the index all of the information associated with elements or
   * locations in the given sources. This includes relationships between an element in the given
   * sources and any other locations, relationships between any other elements and a location within
   * the given sources.
   * <p>
   * This method should be invoked when multiple sources are no longer part of the code base.
   * 
   * @param the {@link AnalysisContext} in which {@link Source}s being removed
   * @param container the {@link SourceContainer} holding the sources being removed
   */
  void removeSources(AnalysisContext context, SourceContainer container);

  /**
   * Should be called in separate {@link Thread} to process request in this {@link Index}. Does not
   * return until the {@link #stop()} method is called.
   */
  void run();

  /**
   * Should be called to stop process running {@link #run()}, so stop processing requests.
   */
  void stop();
}
