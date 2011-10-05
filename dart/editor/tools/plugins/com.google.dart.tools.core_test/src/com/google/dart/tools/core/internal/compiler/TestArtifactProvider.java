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
package com.google.dart.tools.core.internal.compiler;

import com.google.dart.compiler.DartArtifactProvider;
import com.google.dart.compiler.Source;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * An in-memory {@link DartArtifactProvider} for testing.
 */
public class TestArtifactProvider extends DartArtifactProvider {
  /**
   * Answer the local URI for the specified artifact
   */
  private static URI getLocalUri(Source source, String part, String extension) {
    String path = source.getUri().toString();
    if (part != null && path.length() > 0) {
      path += "$" + part;
    }
    path += extension;
    return URI.create(path);
  }

  private final Map<URI, String> cache = new HashMap<URI, String>();
  private final Map<URI, Long> lastModified = new HashMap<URI, Long>();

  @Override
  public Reader getArtifactReader(Source source, String part, String extension) throws IOException {
    final URI uri = getLocalUri(source, part, extension);
    String content = cache.get(uri);
    if (content != null) {
      return new StringReader(content);
    }
    return null;
  }

  @Override
  public URI getArtifactUri(Source source, String part, String extension) {
    return getLocalUri(source, part, extension);
  }

  @Override
  public Writer getArtifactWriter(Source source, String part, String extension) throws IOException {
    final URI uri = getLocalUri(source, part, extension);
    StringWriter writer = new StringWriter(4096) {
      @Override
      public void close() throws IOException {
        super.close();
        cache.put(uri, toString());
        lastModified.put(uri, System.currentTimeMillis());
      }
    };
    return writer;
  }

  @Override
  public boolean isOutOfDate(Source source, Source base, String extension) {
    final URI uri = getLocalUri(base, "", extension);
    Long artifactLastModified = lastModified.get(uri);
    if (artifactLastModified == null) {
      return true;
    }
    long baseLastModified = ((TestDartSource) base).getLastModified();
    return artifactLastModified < baseLastModified;
  }
}
