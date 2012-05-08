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

import java.io.File;

/**
 * A match representing a file external to the workspace.
 */
public class ExternalFileMatch extends FileResourceMatch {

  /**
   * Constructs a new Match object.
   * 
   * @param file the file that contains the match
   */
  public ExternalFileMatch(File file) {
    super(file, -1, -1);
  }

  /**
   * Constructs a new Match object.
   * 
   * @param file the file that contains the match
   * @param unit the unit offset and length are based on
   * @param offset the offset the match starts at
   * @param length the length of the match
   * @param lineElement the matched line
   */
  public ExternalFileMatch(File file, int matchOffset, int matchLength, LineElement lineElement) {
    super(file, matchOffset, matchLength, lineElement);
  }
}
