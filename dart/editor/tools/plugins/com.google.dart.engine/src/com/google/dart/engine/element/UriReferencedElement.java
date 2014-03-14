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
package com.google.dart.engine.element;

/**
 * The interface {@code UriReferencedElement} defines the behavior of objects included into a
 * library using some URI.
 * 
 * @coverage dart.engine.element
 */
public interface UriReferencedElement extends Element {
  /**
   * Return the offset of the character immediately following the last character of this node's URI,
   * or {@code -1} for synthetic import.
   * 
   * @return the offset of the character just past the node's URI
   */
  public int getUriEnd();

  /**
   * Return the offset of the URI in the file, or {@code -1} if this element is synthetic.
   * 
   * @return the offset of the URI
   */
  public int getUriOffset();

  /**
   * Return the URI that is used to include this element into the enclosing library, or {@code null}
   * if this is the defining compilation unit of a library.
   * 
   * @return the URI that is used to include this element into the enclosing library
   */
  String getUri();
}
