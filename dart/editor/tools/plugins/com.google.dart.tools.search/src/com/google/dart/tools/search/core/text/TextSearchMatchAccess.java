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
package com.google.dart.tools.search.core.text;

import com.google.dart.tools.search.internal.ui.text.FileResource;
import com.google.dart.tools.search.internal.ui.text.FileResourceMatch;
import com.google.dart.tools.search.internal.ui.text.LineElement;

/**
 * A {@link TextSearchMatchAccess} gives access to a pattern match found by the
 * {@link TextSearchEngine}.
 * <p>
 * Please note that <code>{@link TextSearchMatchAccess}</code> objects <b>do not </b> have value
 * semantic. The state of the object might change over time especially since objects are reused for
 * different call backs. Clients shall not keep a reference to a {@link TextSearchMatchAccess}
 * element.
 * </p>
 * <p>
 * This class should only be implemented by implementors of a {@link TextSearchEngine}.
 * </p>
 */
public abstract class TextSearchMatchAccess {

  /**
   * Creates a match object based on the given line element info.
   * 
   * @param lineElement the line element
   * @return a match object
   */
  public abstract FileResourceMatch createMatch(LineElement lineElement);

  /**
   * Returns the file the match was found in.
   * 
   * @return the file the match was found.
   */
  public abstract FileResource<?> getFile();

  /**
   * Returns the file's content at the given offsets.
   * 
   * @param offset the offset of the requested content
   * @param length the of the requested content
   * @return the substring of the file's content
   * @throws IndexOutOfBoundsException an {@link IndexOutOfBoundsException} is thrown when the
   *           <code>offset</code> or the <code>length</code> are negative or when
   *           <code>offset + length</code> is not less than the file content's length.
   */
  public abstract String getFileContent(int offset, int length);

  /**
   * Returns a character of the file's content at the given offset
   * 
   * @param offset the offset
   * @return the character at the given offset
   * @throws IndexOutOfBoundsException an {@link IndexOutOfBoundsException} is thrown when the
   *           <code>offset</code> is negative or not less than the file content's length.
   */
  public abstract char getFileContentChar(int offset);

  /**
   * Returns the length of this file's content.
   * 
   * @return the length of this file's content.
   */
  public abstract int getFileContentLength();

  /**
   * Returns the length of this search match.
   * 
   * @return the length of this search match
   */
  public abstract int getMatchLength();

  /**
   * Returns the offset of this search match.
   * 
   * @return the offset of this search match
   */
  public abstract int getMatchOffset();

}
