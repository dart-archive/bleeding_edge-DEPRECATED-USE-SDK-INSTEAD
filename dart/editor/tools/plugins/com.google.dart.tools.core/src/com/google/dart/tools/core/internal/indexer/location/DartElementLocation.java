/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.indexer.location;

import com.google.dart.indexer.locations.Location;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.SourceRange;

public interface DartElementLocation extends Location {
  /**
   * Return the element associated with this location.
   * 
   * @return the element associated with this location
   */
  public DartElement getDartElement();

  /**
   * Return the kind of reference represented by this location.
   * 
   * @return the kind of reference represented by this location
   */
  public ReferenceKind getReferenceKind();

  /**
   * Return the source range associated with this location.
   * 
   * @return the source range associated with this location
   */
  public SourceRange getSourceRange();
}
