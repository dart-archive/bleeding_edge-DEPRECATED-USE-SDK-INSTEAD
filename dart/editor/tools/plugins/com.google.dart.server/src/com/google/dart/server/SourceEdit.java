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
 * The interface {@link SourceEdit} is a description of a single change to a single file.
 * 
 * @coverage dart.server
 */
public interface SourceEdit {
  /**
   * An empty array of source edits.
   */
  public final SourceEdit[] EMPTY_ARRAY = new SourceEdit[0];

  /**
   * The length of the region to be modified.
   * 
   * @return the length of the region to be modified
   */
  public int getLength();

  /**
   * The offset of the region to be modified.
   * 
   * @return the offset of the region to be modified
   */
  public int getOffset();

  /**
   * The code that is to replace the specified region in the original code.
   * 
   * @return the code that is to replace the specified region in the original code
   */
  public String getReplacement();

}
