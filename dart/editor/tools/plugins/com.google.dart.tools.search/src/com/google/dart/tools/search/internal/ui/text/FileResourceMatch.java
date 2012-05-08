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
package com.google.dart.tools.search.internal.ui.text;

import com.google.dart.tools.search.ui.text.Match;

/**
 * A textual match in a file resource.
 */
public class FileResourceMatch extends Match {

  private final LineElement lineEntry;

  /**
   * Create a match.
   * 
   * @param element the matched element
   * @param offset the offset of the match
   * @param length the length of the match
   */
  public FileResourceMatch(Object element, int offset, int length) {
    this(element, offset, length, null);
  }

  /**
   * Create a match.
   * 
   * @param element the matched element
   * @param offset the offset of the match
   * @param length the length of the match
   * @param lineEntry the matched line
   */
  public FileResourceMatch(Object element, int offset, int length, LineElement lineEntry) {
    super(element, offset, length);
    this.lineEntry = lineEntry;
  }

  /**
   * Get the associated line.
   * 
   * @return the line or <code>null</code> if none is associated.
   */
  public LineElement getLineElement() {
    return lineEntry;
  }

}
