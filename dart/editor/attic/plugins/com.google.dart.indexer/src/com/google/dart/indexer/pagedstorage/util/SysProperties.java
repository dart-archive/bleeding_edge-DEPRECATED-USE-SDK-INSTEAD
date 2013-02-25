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

public class SysProperties {
  /**
   * Number of times to retry file delete and rename.
   */
  public static final int MAX_FILE_RETRY = 16;

  /**
   * If the mapped buffer should be loaded when the file is opened. This can improve performance.
   */
  public static final boolean NIO_LOAD_MAPPED = false;

  /**
   * If possible, use a hack to un-map the mapped file. See also
   * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038
   */
  public static final boolean NIO_CLEANER_HACK = true;

  public static final boolean runFinalize = true;

  private SysProperties() {
    // utility class
  }
}
