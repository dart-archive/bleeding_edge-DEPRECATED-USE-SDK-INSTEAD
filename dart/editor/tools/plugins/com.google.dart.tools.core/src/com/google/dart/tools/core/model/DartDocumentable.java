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

package com.google.dart.tools.core.model;

/**
 * A DartElement that is documentable.
 */
public interface DartDocumentable extends DartElement {

  /**
   * Returns the dartdoc range if this element is from source or if this element is a binary element
   * with an attached source, null otherwise.
   * <p>
   * If this element is from source, the dartdoc range is extracted from the corresponding source.
   * </p>
   * <p>
   * If this element is from a binary, the dartdoc is extracted from the attached source if present.
   * </p>
   * <p>
   * If this element's openable is not consistent, then null is returned.
   * </p>
   * 
   * @exception DartModelException if this element does not exist or if an exception occurs while
   *              accessing its corresponding resource.
   * @return a source range corresponding to the dartdoc source or <code>null</code> if no source is
   *         available, this element has no dartdoc comment or this element's openable is not
   *         consistent
   * @see IOpenable#isConsistent()
   */
  public SourceRange getDartDocRange() throws DartModelException;

}
