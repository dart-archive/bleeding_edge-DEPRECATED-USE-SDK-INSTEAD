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
package com.google.dart.tools.core.utilities.compiler;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * A source location - a source path and line number tuple.
 */
public class SourceLocation {
  public static final int INVALID_LINE = -1;

  public static final SourceLocation UNKNOWN_LOCATION = new SourceLocation(Path.ROOT, -1);

  private int line;
  private IPath path;

  /**
   * Create a new SourceLocation.
   * 
   * @param path
   * @param line
   */
  public SourceLocation(IPath path, int line) {
    this.path = path;
    this.line = line;
  }

  /**
   * @return the user presentable name of this source mapping
   */
  public String getFileName() {
    return path.lastSegment();
  }

  /**
   * @return the source line
   */
  public int getLine() {
    return line;
  }

  /**
   * @return return the source file path
   */
  public IPath getPath() {
    return path;
  }

  /**
   * @return whether this source mapping is valid
   */
  public boolean isValid() {
    return line != INVALID_LINE;
  }

  @Override
  public String toString() {
    return "[" + getFileName() + ":" + getLine() + "]";
  }

}
