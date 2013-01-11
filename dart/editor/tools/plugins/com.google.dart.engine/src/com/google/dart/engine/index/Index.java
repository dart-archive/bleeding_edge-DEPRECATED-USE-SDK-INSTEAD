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
import com.google.dart.engine.element.Element;
import com.google.dart.engine.source.Source;

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
   * Asynchronously process the given {@link CompilationUnit} in order to record the relationships.
   * 
   * @param unit the compilation unit being indexed
   */
  void indexUnit(CompilationUnit unit);

  /**
   * Asynchronously remove from the index all of the information associated with elements or
   * locations in the given source. This includes relationships between an element in the given
   * source and any other locations, relationships between any other elements and a location within
   * the given source.
   * <p>
   * This method should be invoked when a source is no longer part of the code base.
   * 
   * @param source the {@link Source} being removed
   */
  void removeSource(Source source);

  /**
   * Should be called in separate {@link Thread} to process request in this {@link Index}.
   */
  void run();
}
