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
package com.google.dart.tools.core.test.util;

import java.io.File;

/**
 * The interface <code>FileOperation</code> defines the behavior of objects that perform an
 * operation on a {@link File file}.
 */
public interface FileOperation {
  /**
   * Execute this operation with the given file.
   * 
   * @param file the file on which this operation is to be executed
   * @throws Exception if there is a problem
   */
  public void run(File file) throws Exception;
}
