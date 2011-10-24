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
package com.google.dart.tools.core.utilities.compiler;

import com.google.dart.compiler.CommandLineOptions.CompilerOptions;
import com.google.dart.compiler.CompilerConfiguration;
import com.google.dart.compiler.DartArtifactProvider;
import com.google.dart.compiler.DartCompilerListener;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.DefaultCompilerConfiguration;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.UrlSource;
import com.google.dart.compiler.backend.js.AbstractJsBackend;
import com.google.dart.compiler.util.DartSourceString;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.CachingArtifactProvider;
import com.google.dart.tools.core.internal.builder.RootArtifactProvider;
import com.google.dart.tools.core.internal.compiler.LoggingDartCompilerListener;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

/**
 * Utility class for "warming up" the compiler by loading artifacts and performing some simple
 * compilations to load and jit classes.
 */
public class DartCompilerWarmup {
  /**
   * Wrapper the root provider to prevent the warmup specific content from being cached in the
   * {@link RootArtifactProvider}.
   */
  private static class ArtifactProvider extends CachingArtifactProvider {
    private final DartArtifactProvider rootProvider;

    public ArtifactProvider(DartArtifactProvider rootProvider) {
      this.rootProvider = rootProvider;
    }

    @Override
    public Reader getArtifactReader(Source source, String part, String extension)
        throws IOException {
      if (source.getName().equals(WARMUP_DART)) {
        return super.getArtifactReader(source, part, extension);
      }
      return rootProvider.getArtifactReader(source, part, extension);
    }

    @Override
    public URI getArtifactUri(Source source, String part, String extension) {
      if (source.getName().equals(WARMUP_DART)) {
        return super.getArtifactUri(source, part, extension);
      }
      return rootProvider.getArtifactUri(source, part, extension);
    }

    @Override
    public Writer getArtifactWriter(Source source, String part, String extension)
        throws IOException {
      if (source.getName().equals(WARMUP_DART)) {

        // Don't write the final application JS and map files

        if (extension.startsWith(AbstractJsBackend.EXTENSION_APP_JS)) {
          return new Writer() {

            @Override
            public void close() throws IOException {
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
            }
          };
        }

        // Cache "warmup" artifacts locally so that they can be thrown away

        return super.getArtifactWriter(source, part, extension);
      }
      return rootProvider.getArtifactWriter(source, part, extension);
    }

    @Override
    public boolean isOutOfDate(Source source, Source base, String extension) {
      if (source.getName().equals(WARMUP_DART)) {
        return super.isOutOfDate(source, base, extension);
      }
      return rootProvider.isOutOfDate(source, base, extension);
    }
  }

  /**
   * An in-memory {@link LibrarySource} wrappering a {@link DartSource}
   */
  private static class LibraryDartSource extends UrlSource implements LibrarySource {
    private DartSource dartSrc;

    public LibraryDartSource(DartSource dartSrc, SystemLibraryManager sysLibMgr) {
      super(dartSrc.getUri(), sysLibMgr);
      this.dartSrc = dartSrc;
    }

    @Override
    public boolean exists() {
      return true;
    }

    @Override
    public LibrarySource getImportFor(String relPath) throws IOException {
      return new UrlLibrarySource(getAbsoluteUri().resolve(relPath).normalize(),
          systemLibraryManager);
    }

    @Override
    public long getLastModified() {
      return dartSrc.getLastModified();
    }

    @Override
    public String getName() {
      return dartSrc.getName();
    }

    @Override
    public DartSource getSourceFor(String relPath) {
      if (relPath != null) {
        if (relPath.equals(dartSrc.getName())) {
          return dartSrc;
        }
      }
      return null;
    }

    @Override
    public Reader getSourceReader() throws IOException {
      return dartSrc.getSourceReader();
    }

    @Override
    public URI getUri() {
      return dartSrc.getUri();
    }
  }

  private static final String WARMUP_DART = "warmup.dart";

  /**
   * For testing purposes only.
   * 
   * @see #warmUpCompiler()
   */
  public static void warmUpCompiler(DartArtifactProvider rootProvider, DartCompilerListener listener) {
    String warmupSrcCode = "#import('dart:dom');\n"
        + "main() {if (window != null) print('success');}";

    DartSource dartSrc = new DartSourceString(WARMUP_DART, warmupSrcCode);

    SystemLibraryManager sysLibMgr = SystemLibraryManagerProvider.getSystemLibraryManager();
    LibrarySource libSrc = new DartCompilerWarmup.LibraryDartSource(dartSrc, sysLibMgr);
    CompilerOptions options = new CompilerOptions();
    DartArtifactProvider provider = new ArtifactProvider(rootProvider);

    try {
      CompilerConfiguration config = new DefaultCompilerConfiguration(options, sysLibMgr);
      DartCompilerUtilities.secureCompileLib(libSrc, config, provider, listener);
    } catch (IOException e) {
      DartCore.logError(e);
    }
  }

  /**
   * Perform some work that will cause compiler classes to be loaded, Dart core and dom classes to
   * be resolved, and possibly cause some compiler classes to be jitted.
   */
  public static void warmUpCompiler() {
    RootArtifactProvider rootProvider = RootArtifactProvider.getInstance();
    LoggingDartCompilerListener listener = LoggingDartCompilerListener.INSTANCE;
    warmUpCompiler(rootProvider, listener);
  }

}
