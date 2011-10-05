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
package com.google.dart.tools.core.internal.builder;

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
 * A read-through caching implementation of {@link DartArtifactProvider} that pulls existing content
 * from a parent artifact provider but caches new content in memory.
 */
public class LocalArtifactProvider extends DartArtifactProvider {
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

  private final DartArtifactProvider parent;
  private final Map<URI, String> cache = new HashMap<URI, String>();

  public LocalArtifactProvider(DartArtifactProvider parent) {
    if (parent == null) {
      throw new IllegalArgumentException();
    }
    this.parent = parent;
  }

  /**
   * If content is cached locally, then return a reader for that content, otherwise defer to the
   * parent artifact provider.
   */
  @Override
  public Reader getArtifactReader(Source source, String part, String extension) throws IOException {
    final URI uri = getLocalUri(source, part, extension);
    String content = cache.get(uri);
    if (content != null) {
      return new StringReader(content);
    }
    return parent.getArtifactReader(source, part, extension);
  }

  // TODO (danrubel): convert first argument to Source so that it matches other methods?
  @Override
  public URI getArtifactUri(Source source, String part, String extension) {
    return parent.getArtifactUri(source, part, extension);
  }

  /**
   * Return a writer that will cache its contents locally in memory rather than wherever the parent
   * provider chooses (e.g. on disk).
   */
  @Override
  public Writer getArtifactWriter(Source source, String part, String extension) throws IOException {
    final URI uri = getLocalUri(source, part, extension);
    StringWriter writer = new StringWriter(4096) {
      @Override
      public void close() throws IOException {
        super.close();
        cache.put(uri, toString());
      }
    };
    return writer;
  }

  /**
   * If content for this extension has been cached locally, then assume that content is up to date.
   * Otherwise defer to the parent artifact provider.
   */
  @Override
  public boolean isOutOfDate(Source source, Source base, String extension) {
    final URI uri = getLocalUri(base, "", extension);
    String content = cache.get(uri);
    if (content != null) {
      return false;
    }
    return parent.isOutOfDate(source, base, extension);
  }
}
