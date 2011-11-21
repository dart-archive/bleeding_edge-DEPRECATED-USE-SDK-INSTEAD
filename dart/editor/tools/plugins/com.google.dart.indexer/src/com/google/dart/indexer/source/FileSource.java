/*
 * Copyright 2011, the Dart project authors.
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
package com.google.dart.indexer.source;

import java.io.File;
import java.net.URI;

/**
 * Instances of the class <code>FileSource</code> represent a file containing data that can be
 * indexed.
 */
public class FileSource extends IndexableSource {
  /**
   * The file containing the data that can be indexed
   */
  private File file;

  /**
   * Initialize a newly created instance to represent the given file.
   * 
   * @param file the file containing the data that can be indexed
   */
  public FileSource(File file) {
    this.file = file;
  }

  @Override
  public String getFileExtension() {
    return getFileExtension(file.getName());
  }

  @Override
  public long getModificationStamp() {
    return file.lastModified();
  }

  @Override
  public URI getUri() {
    return file.toURI();
  }

  protected String getFileExtension(String fileName) {
    int index = fileName.lastIndexOf('.');
    if (index < 0) {
      return "";
    }
    return fileName.substring(index + 1);
  }
}
