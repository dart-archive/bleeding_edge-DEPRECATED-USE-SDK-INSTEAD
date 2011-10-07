/*
 * Copyright 2011 Dart project authors.
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
 * An in memory caching implementation of {@link DartArtifactProvider} that caches all content in
 * memory, never writing anything to disk.
 */
public abstract class CachingArtifactProvider extends DartArtifactProvider {

  /**
   * Answer the encoded URI scheme specific part so that we don't have to decode then encode when we
   * construct a URI. In other words, you can safely call {@link URI#create(String)} and
   * {@link URI#URI(String)} with the result of this method.
   */
  private static String getLocalUriPart(Source source, String part, String extension) {
    // Obtain the encoded scheme specific part so that we don't decode then encode
    StringBuilder builder = new StringBuilder(source.getUri().getRawSchemeSpecificPart());
    if (part != null && part.length() > 0) {
      builder.append("$");
      builder.append(part);
    }
    builder.append(".");
    builder.append(extension);
    return builder.toString();
  }

  /**
   * A mapping of the value returned by {@link #getLocalUriPart(Source, String, String)} to artifact
   * content
   */
  private final Map<String, String> cache = new HashMap<String, String>();

  /**
   * Remove all artifacts from the cache
   */
  public void clearCachedArtifacts() {
    synchronized (cache) {
      cache.clear();
    }
  }

  /**
   * If content is cached locally, then return a reader for that content, otherwise return
   * <code>null</code>.
   */
  @Override
  public Reader getArtifactReader(Source source, String part, String extension) throws IOException {
    String content;
    synchronized (cache) {
      content = cache.get(getLocalUriPart(source, part, extension));
    }
    if (content != null) {
      return new StringReader(content);
    }
    return null;
  }

  @Override
  public URI getArtifactUri(Source source, String part, String extension) {
    return URI.create(getLocalUriPart(source, part, extension));
  }

  /**
   * Return a writer that will cache its contents locally in memory rather than wherever the parent
   * provider chooses (e.g. on disk).
   */
  @Override
  public Writer getArtifactWriter(Source source, String part, String extension) throws IOException {
    final String uriPart = getLocalUriPart(source, part, extension);
    return new StringWriter(4096) {
      @Override
      public void close() throws IOException {
        super.close();
        synchronized (cache) {
          cache.put(uriPart, toString());
        }
      }
    };
  }

  /**
   * Return <code>true</code> if the artifact is cached locally.
   */
  @Override
  public boolean isOutOfDate(Source source, Source base, String extension) {
    synchronized (cache) {
      return cache.get(getLocalUriPart(source, "", extension)) == null;
    }
  }
}
