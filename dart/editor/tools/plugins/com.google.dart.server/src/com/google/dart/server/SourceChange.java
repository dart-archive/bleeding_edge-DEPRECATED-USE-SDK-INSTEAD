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
 * The interface {@link SourceChange} is a description of a single change to one or more files.
 * 
 * @coverage dart.server
 */
public interface SourceChange {
  /**
   * An empty array of source changes.
   */
  public final SourceChange[] EMPTY_ARRAY = new SourceChange[0];

  /**
   * A list of the edits used to effect the change, grouped by file.
   * 
   * @return a list of the edits used to effect the change, grouped by file
   */
  public SourceFileEdit[] getEdits();

  /**
   * A textual description of the change to be applied.
   * 
   * @return a textual description of the change to be applied
   */
  public String getMessage();

}
