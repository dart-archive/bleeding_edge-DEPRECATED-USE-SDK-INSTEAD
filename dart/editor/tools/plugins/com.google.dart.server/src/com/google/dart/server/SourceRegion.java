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
 * The interface {@code SourceRegion} defines the behavior of objects representing a range of
 * characters within a {@link Source}.
 * 
 * @coverage dart.server
 */
public interface SourceRegion {
  /**
   * Check if <code>x</code> is in [offset, offset + length] interval.
   */
  public boolean containsInclusive(int x);

  /**
   * Return the length of the region.
   * 
   * @return the length of the region
   */
  public int getLength();

  /**
   * Return the offset to the beginning of the region.
   * 
   * @return the offset to the beginning of the region
   */
  public int getOffset();
}
