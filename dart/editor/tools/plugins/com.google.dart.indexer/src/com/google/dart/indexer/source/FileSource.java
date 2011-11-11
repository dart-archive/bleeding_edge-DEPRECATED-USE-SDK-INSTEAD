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

  /**
   * Return an indication of when the target was last modified. The stamp can be any monotonically
   * increasing value, such as a modification time for a file or a simple counter.
   * 
   * @return the modification stamp associated with the target
   */
  @Override
  public long getModificationStamp() {
    return file.lastModified();
  }

  /**
   * Return the URI uniquely identifying this target.
   * 
   * @return the URI uniquely identifying this target
   */
  @Override
  public URI getUri() {
    return file.toURI();
  }
}
