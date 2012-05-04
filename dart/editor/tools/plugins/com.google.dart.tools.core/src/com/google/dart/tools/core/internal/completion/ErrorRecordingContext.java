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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.compiler.CommandLineOptions.CompilerOptions;
import com.google.dart.compiler.CompilerConfiguration;
import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartCompilerContext;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.DefaultCompilerConfiguration;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.compiler.metrics.CompilerMetrics;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.utilities.net.URIUtilities;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

class ErrorRecordingContext implements DartCompilerContext {
  private CompilerConfiguration configuration;
  {
    configuration = new DefaultCompilerConfiguration(
        new CompilerOptions(),
        SystemLibraryManagerProvider.getSystemLibraryManager());
  }

  private List<DartCompilationError> errors = new ArrayList<DartCompilationError>();

  @Override
  public LibraryUnit getAppLibraryUnit() {
    return null;
  }

  @Override
  public LibraryUnit getApplicationUnit() {
    throw new AssertionError();
  }

  @Override
  public Reader getArtifactReader(Source source, String part, String extension) throws IOException {
    throw new AssertionError();
  }

  @Override
  public URI getArtifactUri(DartSource source, String part, String extension) {
    throw new AssertionError();
  }

  @Override
  public Writer getArtifactWriter(Source source, String part, String extension) throws IOException {
    throw new AssertionError();
  }

  @Override
  public CompilerConfiguration getCompilerConfiguration() {
    return configuration;
  }

  @Override
  public CompilerMetrics getCompilerMetrics() {
    return null;
  }

  @Override
  public LibraryUnit getLibraryUnit(LibrarySource lib) {
    throw new AssertionError();
  }

  @Override
  public LibrarySource getSystemLibraryFor(String importSpec) {
    // TODO(brianwilkerson) This should really return configuration.getSystemLibraryFor(...), but
    // that method is currently broken.
    try {
      return new UrlLibrarySource(URIUtilities.safelyResolveDartUri(new URI(importSpec)));
    } catch (URISyntaxException exception) {
      return null;
    }
  }

  @Override
  public boolean isOutOfDate(Source source, Source base, String extension) {
    return false;
  }

  @Override
  public void onError(DartCompilationError event) {
    errors.add(event);
  }

  public void reset() {
    errors.clear();
  }
}
