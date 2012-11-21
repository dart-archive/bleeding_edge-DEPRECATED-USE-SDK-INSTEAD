/*
 * Copyright (c) 2012, the Dart project authors.
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
import java.util.HashMap;

/**
 * Instances of the class {@code TestSourceFactory} implement a source factory that can be used for
 * testing.
 */
public class TestSourceFactory extends SourceFactory {
  /**
   * Instances of the class {@code TestResolver} implement a UriResolver that creates instances of
   * {@link TestSource}.
   */
  private static class TestResolver extends UriResolver {
    /**
     * The name of the {@code file} scheme.
     */
    private static final String FILE_SCHEME = "file";

    /**
     * Return {@code true} if the given URI is a {@code file} URI.
     * 
     * @param uri the URI being tested
     * @return {@code true} if the given URI is a {@code file} URI
     */
    private static boolean isFileUri(URI uri) {
      return uri.getScheme().equals(FILE_SCHEME);
    }

    /**
     * Initialize a newly created resolver to resolve {@code file} URI's relative to the given root
     * directory.
     */
    public TestResolver() {
      super();
    }

    @Override
    protected Source resolveAbsolute(SourceFactory factory, URI uri) {
      if (!isFileUri(uri)) {
        return null;
      }
      return ((TestSourceFactory) factory).forFile(new File(uri));
    }

    @Override
    protected Source resolveRelative(SourceFactory factory, Source containingSource,
        URI containedUri) {
      if (containingSource instanceof SourceImpl) {
        try {
          URI resolvedUri = ((SourceImpl) containingSource).getFile().toURI().resolve(containedUri).normalize();
          return ((TestSourceFactory) factory).forFile(new File(resolvedUri));
        } catch (Exception exception) {
          // Fall through to return null
        }
      }
      return null;
    }
  }

  /**
   * A table mapping files to the contents of those files.
   */
  private HashMap<File, String> sourceMap = new HashMap<File, String>();

  /**
   * Initialize a newly created factory.
   */
  public TestSourceFactory() {
    super(new TestResolver());
  }

  @Override
  public Source forFile(File file) {
    return new TestSource(this, file, sourceMap.get(file));
  }

  /**
   * Set the contents of the file to the given source.
   * 
   * @param file the file whose source is being recorded
   * @param source the contents of the specified file
   */
  public void setSource(File file, String source) {
    sourceMap.put(file, source);
  }
}
