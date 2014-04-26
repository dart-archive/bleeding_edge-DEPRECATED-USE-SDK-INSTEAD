/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.engine.services.change;

import java.io.File;

/**
 * {@link Change} to create new file.
 */
public class CreateFileChange extends Change {
  private final File file;
  private final String content;

  public CreateFileChange(String name, File file, String content) {
    super(name);
    this.file = file;
    this.content = content;
  }

  /**
   * @return the content for the created file.
   */
  public String getContent() {
    return content;
  }

  /**
   * @return the {@link File} to create.
   */
  public File getFile() {
    return file;
  }
}
