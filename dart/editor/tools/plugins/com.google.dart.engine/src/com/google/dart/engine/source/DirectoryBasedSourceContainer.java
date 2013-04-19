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
package com.google.dart.engine.source;

import java.io.File;

/**
 * Instances of the class {@link DirectoryBasedSourceContainer} represent a source container that
 * contains all sources within a given directory.
 * 
 * @coverage dart.engine.source
 */
public class DirectoryBasedSourceContainer implements SourceContainer {
  /**
   * Append the system file separator to the given path unless the path already ends with a
   * separator.
   * 
   * @param path the path to which the file separator is to be added
   * @return a path that ends with the system file separator
   */
  private static String appendFileSeparator(String path) {
    if (path == null || path.length() <= 0 || path.charAt(path.length() - 1) == File.separatorChar) {
      return path;
    }
    return path + File.separator;
  }

  /**
   * The container's path (not {@code null}).
   */
  private final String path;

  /**
   * Construct a container representing the specified directory and containing any sources whose
   * {@link Source#getFullName()} starts with the directory's path. This is a convenience method,
   * fully equivalent to {@link DirectoryBasedSourceContainer#DirectoryBasedSourceContainer(String)}
   * .
   * 
   * @param directory the directory (not {@code null})
   */
  public DirectoryBasedSourceContainer(File directory) {
    this(directory.getPath());
  }

  /**
   * Construct a container representing the specified path and containing any sources whose
   * {@link Source#getFullName()} starts with the specified path.
   * 
   * @param path the path (not {@code null} and not empty)
   */
  public DirectoryBasedSourceContainer(String path) {
    this.path = appendFileSeparator(path);
  }

  @Override
  public boolean contains(Source source) {
    return source.getFullName().startsWith(path);
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof DirectoryBasedSourceContainer)
        && ((DirectoryBasedSourceContainer) obj).getPath().equals(getPath());
  }

  /**
   * Answer the receiver's path, used to determine if a source is contained in the receiver.
   * 
   * @return the path (not {@code null}, not empty)
   */
  public String getPath() {
    return path;
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }

  @Override
  public String toString() {
    return "SourceContainer[" + path + "]";
  }
}
