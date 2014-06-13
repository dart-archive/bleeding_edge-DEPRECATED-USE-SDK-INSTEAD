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
package com.google.dart.engine.internal.index.structure.btree;

import java.nio.ByteBuffer;

/**
 * [PageManager] allows to allocate, read, write and free [Uint8List] pages.
 * 
 * @coverage dart.engine.index.structure
 */
public interface PageManager {
  /**
   * Allocates a new page and returns its identifier.
   */
  int alloc();

  /**
   * Frees the page with the given identifier.
   */
  void free(int id);

  /**
   * The size of pages provided by this [PageManager].
   */
  int getPageSizeInBytes();

  /**
   * Reads the page with the given identifier and returns its content. An internal representation of
   * the page is returned, any changes made to it may be accessible to other clients reading the
   * same page.
   */
  ByteBuffer read(int id);

  /**
   * Writes the given page.
   */
  void write(int id, ByteBuffer page);
}
