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
 * A description of the change to the content of a file. If any of the optional fields are provided
 * then all of the optional fields must be provided.
 * 
 * @coverage dart.server
 */
public interface ContentChange {
  /**
   * The new content of the file, or {@code null} if the content of the file should be read from
   * disk.
   */
  public String getContent();

  /**
   * The (optional) length of the region that was added.
   */
  public int getNewLength();

  /**
   * The (optional) offset of the region that was modified.
   */
  public int getOffset();

  /**
   * The (optional) length of the region that was removed.
   */
  public int getOldLength();
}
