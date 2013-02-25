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

import com.google.dart.indexer.IndexerPlugin;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Instances of the class <code>JarSource</code> represent an entry within a JAR file that contains
 * data that can be indexed.
 */
public class JarSource extends FileSource {
  /**
   * The path to the entry within the JAR file containing the data that can be indexed.
   */
  private String entryPath;

  /**
   * Initialize a newly created instance to represent the given entry in the given JAR file.
   * 
   * @param file the JAR file containing the entry
   * @param entryPath the path to the entry within the JAR file containing the data that can be
   *          indexed
   */
  public JarSource(File file, String entryPath) {
    super(file);
    this.entryPath = entryPath;
  }

  @Override
  public String getFileExtension() {
    int index = entryPath.lastIndexOf('/');
    if (index < 0) {
      return getFileExtension(entryPath);
    }
    return getFileExtension(entryPath.substring(index + 1));
  }

  @Override
  public URI getUri() {
    try {
      return new URI(JAR_SCHEME, super.getUri() + JAR_SUFFIX + entryPath, null);
    } catch (URISyntaxException exception) {
      // We should never get here, but we'll log the exception just in case.
      IndexerPlugin.getLogger().logError(
          exception,
          "Could not create URI for JarSource (file URI = " + super.getUri() + ", entry path = "
              + entryPath + ")");
      return null;
    }
  }
}
