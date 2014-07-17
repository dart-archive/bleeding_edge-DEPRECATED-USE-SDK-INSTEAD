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
 * The interface {@link SourceFileEdit} is a description of a set of changes to a single file.
 * 
 * @coverage dart.server
 */
public interface SourceFileEdit {
  /**
   * An empty array of source file edits.
   */
  public final SourceFileEdit[] EMPTY_ARRAY = new SourceFileEdit[0];

  /**
   * A list of the edits used to effect the change.
   * 
   * @return a list of the edits used to effect the change.
   */
  public SourceEdit[] getEdits();

  /**
   * The file containing the code to be modified.
   * 
   * @return the file containing the code to be modified
   */
  public String getFile();

}
