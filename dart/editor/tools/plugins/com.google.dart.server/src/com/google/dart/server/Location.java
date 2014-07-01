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
 * The interface {@code Location} defines the behavior of objects that represent an information for
 * a location.
 * 
 * @coverage dart.server
 */
public interface Location {
  /**
   * An empty array of locations.
   */
  public final Location[] EMPTY_ARRAY = new Location[0];

  /**
   * Return the file of the location.
   * 
   * @return the file of the location
   */
  public String getFile();

  /**
   * Return the length of the location.
   * 
   * @return the length of the location
   */
  public int getLength();

  /**
   * Return the offset of the location.
   * 
   * @return the offset of the location
   */
  public int getOffset();

  /**
   * Return the one-based index of the column containing the first character of the range.
   * 
   * @return the one-based index of the column containing the first character of the range
   */
  public int getStartColumn();

  /**
   * Return the one-based index of the line containing the first character of the range.
   * 
   * @return the one-based index of the line containing the first character of the range
   */
  public int getStartLine();
}
