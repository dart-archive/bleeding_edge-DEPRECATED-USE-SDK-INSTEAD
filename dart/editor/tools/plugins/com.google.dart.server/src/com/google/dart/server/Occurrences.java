/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.server;

/**
 * The interface {@code Occurrences} defines the behavior of objects that represent a set of
 * occurrences in a file.
 * 
 * @coverage dart.server
 */
public interface Occurrences {
  /**
   * An empty array of occurrences.
   */
  Occurrences[] EMPTY_ARRAY = new Occurrences[0];

  /**
   * Returns {@code true} if the given offset is contained by one of this occurrences ranges.
   */
  public boolean contains(int offset);

  /**
   * A description of the element represented by this node.
   * 
   * @return the {@link Element}
   */
  public Element getElement();

  /**
   * Return the length of the referenced element.
   * 
   * @return the length of the referenced element
   */
  public int getLength();

  /**
   * Return the offsets of the name of the referenced element within the file.
   * 
   * @return the offsets of the name of the referenced element within the file
   */
  public int[] getOffsets();

}
