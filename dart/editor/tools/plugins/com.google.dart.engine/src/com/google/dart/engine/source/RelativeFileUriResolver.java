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
package com.google.dart.engine.source;

import java.io.File;
import java.net.URI;

/**
 * Instances of the class {@code RelativeFileUriResolver} resolve {@code file} URI's.
 * 
 * @coverage dart.engine.source
 */
public class RelativeFileUriResolver extends UriResolver {

  /**
   * The name of the {@code file} scheme.
   */
  public static final String FILE_SCHEME = "file";

  /**
   * Return {@code true} if the given URI is a {@code file} URI.
   * 
   * @param uri the URI being tested
   * @return {@code true} if the given URI is a {@code file} URI
   */
  public static boolean isFileUri(URI uri) {
    return uri.getScheme().equals(FILE_SCHEME);
  }

  /**
   * The directories for the relatvie URI's
   */
  private File[] relativeDirectories;

  /**
   * The root directory for all the source trees
   */
  private File rootDirectory;

  /**
   * Initialize a newly created resolver to resolve {@code file} URI's relative to the given root
   * directory.
   */
  public RelativeFileUriResolver(File rootDirectory, File... relativeDirectories) {
    super();
    this.rootDirectory = rootDirectory;
    this.relativeDirectories = relativeDirectories;
  }

  @Override
  public Source resolveAbsolute(URI uri) {
    String rootPath = rootDirectory.toURI().getPath();
    String uriPath = uri.getPath();
    if (uriPath != null && uriPath.startsWith(rootPath)) {
      String filePath = uri.getPath().substring(rootPath.length());
      for (File dir : relativeDirectories) {
        File file = new File(dir, filePath);
        if (file.exists()) {
          return new FileBasedSource(uri, file);
        }
      }
    }
    return null;
  }
}
