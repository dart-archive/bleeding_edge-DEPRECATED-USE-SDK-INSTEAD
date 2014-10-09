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

package com.google.dart.tools.debug.core.util;

import org.eclipse.debug.core.model.IStackFrame;

import java.util.Map;

/**
 * An interface to allow you to query for a source location path.
 */
public interface IDartStackFrame extends IStackFrame {

  /**
   * Return either the actual path or the mapped path, depending on whether source maps are
   * currently being used. This method is used when Editor is run with the Analysis server.
   * 
   * @param executionContectId the Analysis Server execution context for this launch
   * @param executionUrlToFileCache the cache mapping URIs to file locations
   * @return either the actual path or the mapped path
   */
  public String getSourceLocationPath_NEW(String executionContectId,
      Map<String, String> executionUrlToFileCache);

  /**
   * Return either the actual path or the mapped path, depending on whether source maps are
   * currently being used.
   * 
   * @return either the actual path or the mapped path
   */
  public String getSourceLocationPath_OLD();

  /**
   * @return whether the frame represents a private method or function
   */
  public boolean isPrivate();

  /**
   * Return whether the frame is in code that is covered by a source map.
   * 
   * @return whether the frame is in code that is covered by a source map
   */
  public boolean isUsingSourceMaps();

}
