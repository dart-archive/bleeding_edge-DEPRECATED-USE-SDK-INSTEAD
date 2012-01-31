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
package com.google.dart.tools.core.internal.builder;

import com.google.common.io.Closeables;
import com.google.dart.compiler.DartArtifactProvider;
import com.google.dart.compiler.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An in memory caching implementation of {@link DartArtifactProvider} that caches all content in
 * memory, never writing anything to disk.
 */
public abstract class CachingArtifactProvider extends DartArtifactProvider {
  class CacheElement {
    String part;
    String extension;
    String content;
    long lastModified;
    CacheElement nextElement;

    /**
     * Determine if the specified element represents the specified part and extension
     * 
     * @param part the part (may be <code>null</code>)
     * @param extension the extension (not <code>null</code>)
     * @return <code>true</code> if match, else false
     */
    boolean match(String part, String extension) {
      if (!this.extension.equals(extension)) {
        return false;
      }
      if (this.part == null || this.part.length() == 0) {
        return part == null || part.length() == 0;
      }
      return this.part.equals(part);
    }
  }

  public static final String INVALID_FORMAT_PREFIX = "Invalid artifact file format";

  private int cacheSize = 0;

  /**
   * A mapping of {@link Source#getName()} to {@link CacheElement}s. Multiple {@link CacheElement}s
   * are associated with a single {@link Source#getName()} via the {@link CacheElement#nextElement}
   * field.
   */
  private final Map<String, CacheElement> cache = new HashMap<String, CacheElement>();

  /**
   * Remove all artifacts from the cache.
   */
  public void clearCachedArtifacts() {
    synchronized (cache) {
      cache.clear();
      cacheSize = 0;
    }
    if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
      DartCore.logInformation("CachingArtifactProvider.clearCachedArtifacts()");
    }
  }

  /**
   * Return the last modified time if the artifact is cached locally, or -1 if not.
   * 
   * @param source the source file
   * @param base the base of the artifact to which the comparison is made
   * @param extension the artifact extension
   */
  public long getArtifactLastModified(Source source, Source base, String extension) {
    synchronized (cache) {
      CacheElement elem = cache.get(base.getName());
      while (elem != null) {
        if (elem.match("", extension)) {
          if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
            DartCore.logInformation("CachingArtifactProvider.getArtifactLastModified("
                + source.getName() + ", " + base.getName() + ", " + extension + ") => "
                + elem.lastModified);
          }
          return elem.lastModified;
        }
        elem = elem.nextElement;
      }
    }
    if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
      DartCore.logInformation("CachingArtifactProvider.getArtifactLastModified(" + source.getName()
          + ", " + base.getName() + ", " + extension + ") => -1");
    }
    return -1;
  }

  /**
   * If content is cached locally, then return a reader for that content, otherwise return
   * <code>null</code>.
   */
  @Override
  public Reader getArtifactReader(Source source, String part, String extension) throws IOException {
    synchronized (cache) {
      CacheElement elem = cache.get(source.getName());
      while (elem != null) {
        if (elem.match(part, extension)) {
          if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
            DartCore.logInformation("CachingArtifactProvider.getArtifactReader(" + source.getName()
                + ", " + part + ", " + extension + ") => content size = " + elem.content.length());
          }
          return new StringReader(elem.content);
        }
        elem = elem.nextElement;
      }
    }
    if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
      DartCore.logInformation("CachingArtifactProvider.getArtifactReader(" + source.getName()
          + ", " + part + ", " + extension + ") => null");
    }
    return null;
  }

  @Override
  public URI getArtifactUri(Source source, String part, String extension) {

    // even though the source uri uniquely identifies the source, we must use getName()
    // because the same source file can be referenced by two different libraries
    // and getName() returns a unique string which includes the library name

    StringBuilder builder = new StringBuilder(source.getName());
    if (part != null && part.length() > 0) {
      builder.append("$");
      builder.append(part);
    }
    builder.append(".");
    builder.append(extension);
    String uri = builder.toString();
    if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
      DartCore.logInformation("CachingArtifactProvider.getArtifactUri(" + source.getName() + ", "
          + part + ", " + extension + ") => " + uri);
    }
    return URI.create(uri);
  }

  /**
   * Return a writer that will cache its contents locally in memory rather than wherever the parent
   * provider chooses (e.g. on disk).
   */
  @Override
  public Writer getArtifactWriter(final Source source, final String part, final String extension)
      throws IOException {
    if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
      DartCore.logInformation("CachingArtifactProvider.getArtifactWriter(" + source.getName()
          + ", " + part + ", " + extension + ") => StringWriter");
    }
    return new StringWriter(4096) {
      @Override
      public void close() throws IOException {
        super.close();
        synchronized (cache) {
          CacheElement elem = cache.get(source.getName());
          CacheElement prevElem = null;
          while (elem != null) {
            if (elem.match(part, extension)) {
              break;
            }
            prevElem = elem;
            elem = elem.nextElement;
          }
          if (elem == null) {
            cacheSize++;
            elem = new CacheElement();
            elem.part = part;
            elem.extension = extension;
            if (prevElem != null) {
              prevElem.nextElement = elem;
            } else {
              cache.put(source.getName(), elem);
            }
          }
          elem.content = toString();
          elem.lastModified = System.currentTimeMillis();
        }
      }
    };
  }

  public int getCacheSize() {
    synchronized (cache) {
      if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
        DartCore.logInformation("CachingArtifactProvider.getCacheSize() => " + cacheSize);
      }
      return cacheSize;
    }
  }

  /**
   * Return <code>true</code> if the artifact is cached locally and was generated after the last
   * modification to source.
   * 
   * @param source the source file
   * @param base the base of the artifact to which the comparison is made
   * @param extension the artifact extension
   */
  @Override
  public boolean isOutOfDate(Source source, Source base, String extension) {
    boolean result = getArtifactLastModified(source, base, extension) < source.getLastModified();
    if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
      DartCore.logInformation("CachingArtifactProvider.getArtifactWriter(" + source.getName()
          + ", " + base.getName() + ", " + extension + ") => " + result);
    }
    return result;
  }

  /**
   * Read artifacts from the specified file and add them to the cache
   * 
   * @param file the zip file from which the artifacts are read (not <code>null</code>)
   * @return the number of artifacts written
   * @see #saveCachedArtifacts(File)
   */
  public int loadCachedArtifacts(File file) throws IOException {
    synchronized (cache) {

      // Guard code because this method assumes that the cache is empty 
      // so that it does not have to deal with merging loaded elements in with existing elements.
      if (cacheSize != 0) {
        if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
          DartCore.logInformation("CachingArtifactProvider.loadCachedArtifacts("
              + file.getAbsolutePath() + ") => throw UnsupportedOperationException");
        }
        throw new UnsupportedOperationException();
      }

      BufferedReader reader = new BufferedReader(new FileReader(file));
      boolean failed = true;
      try {

        // First line, "v####", indicate the version
        StringBuilder version = new StringBuilder(20);
        while (true) {
          int ch = reader.read();
          if (ch == '\n' || version.length() >= 20) {
            break;
          }
          version.append((char) ch);
        }
        if (!version.toString().equals(getExpectedVersion())) {
          if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
            DartCore.logInformation("CachingArtifactProvider.loadCachedArtifacts("
                + file.getAbsolutePath() + ") => throw IOException");
          }
          throw new IOException(INVALID_FORMAT_PREFIX + ": expected=" + getExpectedVersion()
              + " actual=" + version.toString());
        }

        int state = 0;
        int contentLength = 0;
        long lastModified = 0;
        StringBuilder key = new StringBuilder(200);
        StringBuilder part = new StringBuilder(200);
        StringBuilder extension = new StringBuilder(200);
        char[] buf = new char[4096];
        CacheElement prevElem = null;

        while (true) {
          int ch = reader.read();
          if (ch == -1) {
            if (state != 0) {
              if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
                DartCore.logInformation("CachingArtifactProvider.loadCachedArtifacts("
                    + file.getAbsolutePath() + ") => throw IOException");
              }
              throw new IOException("Unexpected end of artifact file");
            }
            break;
          }
          switch (state) {

            case 0: // = or digit
              if (ch == '=') {
                state = 1;
                key.setLength(0);
                prevElem = null;
              } else {
                state = 2;
                contentLength = ch - '0';
              }
              break;

            case 1: // key
              if (ch != '\n') {
                key.append((char) ch);
              } else {
                state = 0;
              }
              break;

            case 2: // content length
              if (ch != ',') {
                contentLength = 10 * contentLength + ch - '0';
              } else {
                state = 3;
                lastModified = 0;
              }
              break;

            case 3: // last modified
              if (ch != ',') {
                lastModified = 10 * lastModified + ch - '0';
              } else {
                state = 4;
                part.setLength(0);
              }
              break;

            case 4: // part
              if (ch != ',') {
                part.append((char) ch);
              } else {
                state = 5;
                extension.setLength(0);
              }
              break;

            case 5: // extension and content
              if (ch != '\n') {
                extension.append((char) ch);
              } else {
                state = 0;
                if (buf.length < contentLength) {
                  buf = new char[contentLength + 200];
                }
                int readLength = reader.read(buf, 0, contentLength);
                if (readLength != contentLength) {
                  if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
                    DartCore.logInformation("CachingArtifactProvider.loadCachedArtifacts("
                        + file.getAbsolutePath() + ") => throw IOException");
                  }
                  throw new IOException("Expected " + contentLength + " characters, but read "
                      + readLength);
                }
                if (reader.read() != '\n') {
                  if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
                    DartCore.logInformation("CachingArtifactProvider.loadCachedArtifacts("
                        + file.getAbsolutePath() + ") => throw IOException");
                  }
                  throw new IOException("Expected newline after artifact");
                }

                CacheElement elem = new CacheElement();
                elem.part = part.toString();
                elem.extension = extension.toString();
                elem.content = new String(buf, 0, contentLength);
                elem.lastModified = lastModified;
                if (prevElem != null) {
                  prevElem.nextElement = elem;
                } else {
                  cache.put(key.toString(), elem);
                }
                prevElem = elem;

                cacheSize++;
              }
              break;

            default:
              if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
                DartCore.logInformation("CachingArtifactProvider.loadCachedArtifacts("
                    + file.getAbsolutePath() + ") => throw IllegalStateException");
              }
              throw new IllegalStateException("Invalid state: " + state);
          }
        }
        failed = false;
      } finally {
        Closeables.close(reader, failed);
        if (failed) {
          clearCachedArtifacts();
        }
      }
      if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
        DartCore.logInformation("CachingArtifactProvider.loadCachedArtifacts("
            + file.getAbsolutePath() + ") => " + cacheSize);
      }
      return cacheSize;
    }
  }

  /**
   * Remove all artifacts for the specified source
   */
  public void removeArtifactsFor(Source source) {
    synchronized (cache) {
      CacheElement elem = cache.remove(source.getName());
      while (elem != null) {
        cacheSize--;
        elem = elem.nextElement;
      }
    }
    if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
      DartCore.logInformation("CachingArtifactProvider.removeArtifactsFor(" + source.getName()
          + ")");
    }
  }

  /**
   * Write the currently cached artifacts to the specified file
   * 
   * @param file the zip file to which the artifacts are written (not <code>null</code>)
   * @return the number of artifacts written
   * @see #loadCachedArtifacts(File)
   */
  @SuppressWarnings("unchecked")
  public int saveCachedArtifacts(File file) throws IOException {
    int count = 0;
    Entry<String, CacheElement>[] entries;
    synchronized (cache) {
      entries = cache.entrySet().toArray(new Entry[cache.size()]);
    }
    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    boolean failed = true;
    try {
      writer.append(getExpectedVersion());
      writer.append("\n");
      for (Entry<String, CacheElement> entry : entries) {
        writer.append('=');
        writer.append(entry.getKey());
        writer.append('\n');
        CacheElement elem = entry.getValue();
        while (elem != null) {
          String content = elem.content;
          writer.append(Integer.toString(content.length()));
          writer.append(',');
          writer.append(Long.toString(elem.lastModified));
          writer.append(',');
          writer.append(elem.part);
          writer.append(',');
          writer.append(elem.extension);
          writer.append('\n');
          writer.append(content);
          writer.append('\n');
          count++;
          elem = elem.nextElement;
        }
      }
      failed = false;
    } finally {
      Closeables.close(writer, failed);
    }
    if (DartCoreDebug.TRACE_ARTIFACT_PROVIDER) {
      DartCore.logInformation("CachingArtifactProvider.saveCachedArtifacts("
          + file.getAbsolutePath() + ") => " + count);
    }
    return count;
  }

  /**
   * Answer the version used to determine if an artifact file is invalid or out of date. Since
   * artifacts are tightly coupled to the compiler itself, return a version string containing the
   * SVN build number. If this is a development environment, then return a version string containing
   * the date causing artifacts more than 24 hours old to be discarded.
   * 
   * @return the version (not <code>null</code>)
   */
  private String getExpectedVersion() {
    return "v" + DartCore.getBuildIdOrDate();
  }
}
