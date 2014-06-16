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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

/**
 * Container of information computed by the index - relationships between elements.
 * 
 * @coverage dart.engine.index
 */
public interface IndexStore {
  /**
   * Notifies the index store that we are going to index the unit with the given element.
   * <p>
   * If the unit is a part of a library, then all its locations are removed. If it is a defining
   * compilation unit of a library, then index store also checks if some previously indexed parts of
   * the library are not parts of the library anymore, and clears their information.
   * 
   * @param the {@link AnalysisContext} in which unit being indexed
   * @param unitElement the element of the unit being indexed
   * @return {@code true} the given {@link AnalysisContext} is active, or {@code false} if it was
   *         removed before, so no any unit may be indexed with it
   */
  boolean aboutToIndexDart(AnalysisContext context, CompilationUnitElement unitElement);

  /**
   * Notifies the index store that we are going to index the given {@link HtmlElement}.
   * 
   * @param the {@link AnalysisContext} in which unit being indexed
   * @param htmlElement the {@link HtmlElement} being indexed
   * @return {@code true} the given {@link AnalysisContext} is active, or {@code false} if it was
   *         removed before, so no any unit may be indexed with it
   */
  boolean aboutToIndexHtml(AnalysisContext context, HtmlElement htmlElement);

  /**
   * Removes all of the information.
   */
  void clear();

  /**
   * Notifies that index store that the current Dart or HTML unit indexing is done.
   * <p>
   * If this method is not invoked after corresponding "aboutToIndex*" invocation, all recorded
   * information may be lost.
   */
  void doneIndex();

  /**
   * Return the locations of the elements that have the given relationship with the given element.
   * For example, if the element represents a method and the relationship is the is-referenced-by
   * relationship, then the returned locations will be all of the places where the method is
   * invoked.
   * 
   * @param element the the element that has the relationship with the locations to be returned
   * @param relationship the {@link Relationship} between the given element and the locations to be
   *          returned
   * @return the locations that have the given relationship with the given element
   */
  Location[] getRelationships(Element element, Relationship relationship);

  /**
   * Answer index statistics.
   */
  String getStatistics();

  /**
   * Record that the given element and location have the given relationship. For example, if the
   * relationship is the is-referenced-by relationship, then the element would be the element being
   * referenced and the location would be the point at which it is referenced. Each element can have
   * the same relationship with multiple locations. In other words, if the following code were
   * executed
   * 
   * <pre>
   *   recordRelationship(element, isReferencedBy, location1);
   *   recordRelationship(element, isReferencedBy, location2);
   * </pre>
   * 
   * then both relationships would be maintained in the index and the result of executing
   * 
   * <pre>
   *   getRelationship(element, isReferencedBy);
   * </pre>
   * 
   * would be an array containing both <code>location1</code> and <code>location2</code>.
   * 
   * @param element the element that is related to the location
   * @param relationship the {@link Relationship} between the element and the location
   * @param location the {@link Location} where relationship happens
   */
  void recordRelationship(Element element, Relationship relationship, Location location);

  /**
   * Remove from the index all of the information associated with {@link AnalysisContext}.
   * <p>
   * This method should be invoked when a context is disposed.
   * 
   * @param the {@link AnalysisContext} being removed
   */
  void removeContext(AnalysisContext context);

  /**
   * Remove from the index all of the information associated with elements or locations in the given
   * source. This includes relationships between an element in the given source and any other
   * locations, relationships between any other elements and a location within the given source.
   * <p>
   * This method should be invoked when a source is no longer part of the code base.
   * 
   * @param the {@link AnalysisContext} in which {@link Source} being removed
   * @param source the source being removed
   */
  void removeSource(AnalysisContext context, Source source);

  /**
   * Remove from the index all of the information associated with elements or locations in the given
   * sources. This includes relationships between an element in the given sources and any other
   * locations, relationships between any other elements and a location within the given sources.
   * <p>
   * This method should be invoked when multiple sources are no longer part of the code base.
   * 
   * @param the {@link AnalysisContext} in which {@link Source}s being removed
   * @param container the {@link SourceContainer} holding the sources being removed
   */
  void removeSources(AnalysisContext context, SourceContainer container);
}
