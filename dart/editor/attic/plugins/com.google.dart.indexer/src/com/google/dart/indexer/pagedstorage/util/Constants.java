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
package com.google.dart.indexer.pagedstorage.util;

public class Constants {
  public static final int CACHE_MIN_RECORDS = 16;

  public static final int FILE_BLOCK_SIZE = 16;

  public static final int IO_BUFFER_SIZE = 4 * 1024;

  public static final String MAGIC_FILE_HEADER = "JavaIndexer 1.0-      ".substring(0,
      FILE_BLOCK_SIZE - 1) + "\n";

  public static final String UTF8 = "UTF8";

  private Constants() {
    // utility class
  }
}
