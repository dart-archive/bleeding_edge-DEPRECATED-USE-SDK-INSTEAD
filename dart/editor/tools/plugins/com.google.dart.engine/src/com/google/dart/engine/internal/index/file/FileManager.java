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
package com.google.dart.engine.internal.index.file;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A manager for files content.
 * 
 * @coverage dart.engine.index
 */
public interface FileManager {
  /**
   * Removes all files.
   */
  void clear();

  /**
   * Deletes the file with the given name.
   */
  void delete(String name);

  /**
   * Returns an {@link InputStream} to read the content of the file with the given name.
   */
  InputStream openInputStream(String name) throws Exception;

  /**
   * Returns an {@link OutputStream} to write the content of the file with the given name.
   */
  OutputStream openOutputStream(String name) throws Exception;
}
