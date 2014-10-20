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
 * Instances of the class {@code JavaUriResolver} resolve {@code java} URI's.
 * 
 * @coverage dart.engine.source
 */
public class JavaUriResolver extends UriResolver {
  /**
   * The name of the {@code java} scheme.
   */
  public static final String JAVA_SCHEME = "java";

  /**
   * Return {@code true} if the given URI is a {@code java} URI.
   * 
   * @param uri the URI being tested
   * @return {@code true} if the given URI is a {@code java} URI
   */
  public static boolean isJavaUri(URI uri) {
    String scheme = uri.getScheme();
    return JAVA_SCHEME.equals(scheme);
  }

  private final File[] roots;

  /**
   * Initialize a newly created resolver to resolve {@code java} URI's relative to one of the given
   * root directories.
   */
  public JavaUriResolver(File... roots) {
    this.roots = roots;
  }

  @Override
  public Source resolveAbsolute(URI uri) {
    if (!isJavaUri(uri)) {
      return null;
    }
    String path = uri.getSchemeSpecificPart();
    path = path.replace('.', '/') + ".dart";
    for (File root : roots) {
      File file = new File(root, path);
      if (file.isFile()) {
        return new FileBasedSource(uri, file);
      }
    }
    return new NonExistingSource(path, UriKind.JAVA_URI);
  }
}
