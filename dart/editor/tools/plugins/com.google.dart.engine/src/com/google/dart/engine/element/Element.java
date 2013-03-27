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
package com.google.dart.engine.element;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;

import java.util.Comparator;

/**
 * The interface {@code Element} defines the behavior common to all of the elements in the element
 * model. Generally speaking, the element model is a semantic model of the program that represents
 * things that are declared with a name and hence can be referenced elsewhere in the code.
 * <p>
 * There are two exceptions to the general case. First, there are elements in the element model that
 * are created for the convenience of various kinds of analysis but that do not have any
 * corresponding declaration within the source code. Such elements are marked as being
 * <i>synthetic</i>. Examples of synthetic elements include
 * <ul>
 * <li>default constructors in classes that do not define any explicit constructors,
 * <li>getters and setters that are induced by explicit field declarations,
 * <li>fields that are induced by explicit declarations of getters and setters, and
 * <li>functions representing the initialization expression for a variable.
 * </ul>
 * <p>
 * Second, there are elements in the element model that do not have a name. These correspond to
 * unnamed functions and exist in order to more accurately represent the semantic structure of the
 * program.
 * 
 * @coverage dart.engine.element
 */
public interface Element {
  /**
   * A comparator that can be used to sort elements by their name offset. Elements with a smaller
   * offset will be sorted to be before elements with a larger name offset.
   */
  public static final Comparator<Element> SORT_BY_OFFSET = new Comparator<Element>() {
    @Override
    public int compare(Element firstElement, Element secondElement) {
      return firstElement.getNameOffset() - secondElement.getNameOffset();
    }
  };

  /**
   * Use the given visitor to visit this element.
   * 
   * @param visitor the visitor that will visit this element
   * @return the value returned by the visitor as a result of visiting this element
   */
  public <R> R accept(ElementVisitor<R> visitor);

  /**
   * Return the element of the given class that most immediately encloses this element, or
   * {@code null} if there is no enclosing element of the given class.
   * 
   * @param elementClass the class of the element to be returned
   * @return the element that encloses this element
   */
  public <E extends Element> E getAncestor(Class<E> elementClass);

  /**
   * Return the analysis context in which this element is defined.
   * 
   * @return the analysis context in which this element is defined
   */
  public AnalysisContext getContext();

  /**
   * Return the element that either physically or logically encloses this element. This will be
   * {@code null} if this element is a library because libraries are the top-level elements in the
   * model.
   * 
   * @return the element that encloses this element
   */
  public Element getEnclosingElement();

  /**
   * Return the kind of element that this is.
   * 
   * @return the kind of this element
   */
  public ElementKind getKind();

  /**
   * Return the library that contains this element. This will be the element itself if it is a
   * library element. This will be {@code null} if this element is an HTML file because HTML files
   * are not contained in libraries.
   * 
   * @return the library that contains this element
   */
  public LibraryElement getLibrary();

  /**
   * Return an object representing the location of this element in the element model. The object can
   * be used to locate this element at a later time.
   * 
   * @return the location of this element in the element model
   */
  public ElementLocation getLocation();

  /**
   * Return an array containing all of the metadata associated with this element.
   * 
   * @return the metadata associated with this element
   */
  public Annotation[] getMetadata();

  /**
   * Return the name of this element, or {@code null} if this element does not have a name.
   * 
   * @return the name of this element
   */
  public String getName();

  /**
   * Return the offset of the name of this element in the file that contains the declaration of this
   * element, or {@code -1} if this element is synthetic, does not have a name, or otherwise does
   * not have an offset.
   * 
   * @return the offset of the name of this element
   */
  public int getNameOffset();

  /**
   * Return the source that contains this element, or {@code null} if this element is not contained
   * in a source.
   * 
   * @return the source that contains this element
   */
  public Source getSource();

  /**
   * Return {@code true} if this element, assuming that it is within scope, is accessible to code in
   * the given library. This is defined by the Dart Language Specification in section 3.2:
   * <blockquote> A declaration <i>m</i> is accessible to library <i>L</i> if <i>m</i> is declared
   * in <i>L</i> or if <i>m</i> is public. </blockquote>
   * 
   * @param library the library in which a possible reference to this element would occur
   * @return {@code true} if this element is accessible to code in the given library
   */
  public boolean isAccessibleIn(LibraryElement library);

  /**
   * Return {@code true} if this element is synthetic. A synthetic element is an element that is not
   * represented in the source code explicitly, but is implied by the source code, such as the
   * default constructor for a class that does not explicitly define any constructors.
   * 
   * @return {@code true} if this element is synthetic
   */
  public boolean isSynthetic();

  /**
   * Use the given visitor to visit all of the children of this element. There is no guarantee of
   * the order in which the children will be visited.
   * 
   * @param visitor the visitor that will be used to visit the children of this element
   */
  public void visitChildren(ElementVisitor<?> visitor);
}
