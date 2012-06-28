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
package com.google.dart.tools.core.index;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;

import java.io.File;

/**
 * The interface <code>Index</code> defines the behavior of objects that maintain an index storing
 * information about the {@link Element elements} in a collection of {@link Resource resources}.
 * There are two kinds of information that can be stored: {@link Relationship relationships} between
 * elements and {@link Attribute attributes} associated with elements. All of the operations defined
 * on the index are asynchronous, and results, when there are any, are provided through a callback.
 * <p>
 * Despite being asynchronous, the results of the operations are guaranteed to be consistent with
 * the expectation that operations are performed in the order in which they are requested. For
 * example, assume the existence of a resource <b>R</b> that defines an element <b>E</b>, and that
 * there is an attribute <b>A</b> defined for which the following operation <blockquote><code>
 * getAttribute(E, A, C);
 * </code></blockquote> would result in the callback <b>C</b> being invoked with a non-
 * <code>null</code> value. If the resource is removed and the value of the attribute is
 * subsequently requested using the following operations (and assuming there are no other operations
 * performed between the two) <blockquote><code>
 * removeResource(R);<br>
 * getAttribute(E, A, C);
 * </code></blockquote> the final operation would result in the callback <b>C</b> being invoked with
 * a value of <code>null</code> because the element, and any information associated with it, will
 * have been removed by the previous operation.
 * <p>
 * However, there is no other guarantee about the order in which callbacks will be invoked.
 * Specifically, given two elements <b>E1</b> and <b>E2</b>, and an attribute <b>A</b>, if the
 * following operations are performed: <blockquote><code>
 * getAttribute(E1, A, C1);<br>
 * getAttribute(E2, A, C2);
 * </code></blockquote> there is no guarantee about the order in which the callbacks <b>C1</b> and
 * <b>C2</b> will be invoked.
 */
public interface Index {
  /**
   * Asynchronously invoke the given callback with the value of the given attribute that is
   * associated with the given element, or <code>null</code> if there is no value for the attribute.
   * 
   * @param element the element with which the attribute is associated
   * @param attribute the attribute whose value is to be returned
   * @param callback the callback that will be invoked when the attribute value is available
   */
  public void getAttribute(Element element, Attribute attribute, AttributeCallback callback);

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
  public void getRelationships(Element element, Relationship relationship,
      RelationshipCallback callback);

  /**
   * Asynchronously process the given resource in order to record the data and relationships found
   * within the resource.
   * 
   * @param resource the resource containing the elements defined in the compilation unit
   * @param libraryFile the library file defining the library containing the compilation unit to be
   *          indexed or <code>null</code> if the library is not on disk
   * @param compilationUnit the compilation unit being indexed
   * @param unit the compilation unit to be indexed
   */
  public void indexResource(Resource resource, File libraryFile, CompilationUnit compilationUnit,
      DartUnit unit);

  /**
   * Asynchronously remove from the index all of the information associated with elements or
   * locations in the given resource. This includes relationships between an element in the given
   * resource and any other locations, relationships between any other elements and a location
   * within the given resource, and any values of any attributes defined on elements in the given
   * resource.
   * <p>
   * This method should be invoked when a resource is no longer part of the code base.
   * 
   * @param resource the resource being removed
   */
  public void removeResource(Resource resource);
}
