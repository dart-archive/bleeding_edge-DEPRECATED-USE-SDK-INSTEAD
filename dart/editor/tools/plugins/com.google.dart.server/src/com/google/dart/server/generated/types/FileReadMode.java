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
 *
 * This file has been automatically generated.  Please do not edit it manually.
 * To regenerate the file, use the script "pkg/analysis_server/tool/spec/generate_files".
 */
package com.google.dart.server.generated.types;

/**
 * An enumeration of the ways files can be read from disk. Some clients normalize end of line
 * characters which would make the file offset and range information incorrect.
 *
 * @coverage dart.server.generated.types
 */
public class FileReadMode {

  /**
   * File contents are read as-is, no file changes occur.
   */
  public static final String AS_IS = "AS_IS";

  /**
   * File contents normalize the end of line characters to the single character new line '\n'.
   */
  public static final String NORMALIZE_EOL = "NORMALIZE_EOL";

}
