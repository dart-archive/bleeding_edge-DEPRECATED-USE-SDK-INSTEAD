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
package com.google.dart.command.analyze;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.ContentCache;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Scans, parses, and analyzes a library.
 */
class AnalyzerImpl {

  private static ErrorSeverity getMaxErrorSeverity(List<AnalysisError> errors) {
    ErrorSeverity status = ErrorSeverity.NONE;

    for (AnalysisError error : errors) {
      ErrorSeverity severity = error.getErrorCode().getErrorSeverity();

      status = status.max(severity);
    }

    return status;
  }

  private AnalyzerOptions options;
  private DartSdk sdk;

  public AnalyzerImpl(AnalyzerOptions options) {
    this.options = options;

    // This sdk is shared between multiple runs of the analyzer.
    sdk = new DirectoryBasedDartSdk(options.getDartSdkPath());
  }

  /**
   * Treats the {@code sourceFile} as the top level library and analyzes the unit for warnings and
   * errors.
   * 
   * @param sourceFile file to analyze
   * @param options configuration for this analysis pass
   * @param errors the list to add errors to
   * @return {@code  true} on success, {@code false} on failure.
   */
  public ErrorSeverity analyze(File sourceFile, List<AnalysisError> errors) throws IOException,
      AnalysisException {
    if (sourceFile == null) {
      throw new IllegalArgumentException("sourceFile cannot be null");
    }

    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    ContentCache contentCache = new ContentCache();
    SourceFactory sourceFactory;

    if (options.getPackageRootPath() != null) {
      sourceFactory = new SourceFactory(
          contentCache,
          new DartUriResolver(sdk),
          new FileUriResolver(),
          new PackageUriResolver(options.getPackageRootPath()));
    } else {
      sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    }

    context.setSourceFactory(sourceFactory);

    Source librarySource = new FileBasedSource(contentCache, sourceFile);
    LibraryElement library = context.computeLibraryElement(librarySource);

    CompilationUnit unit = context.resolveCompilationUnit(librarySource, library);

    // TODO: implement options.getShowSdkWarnings() && library.getName().startsWith("dart.")

    // TODO: this needs to be changed to collect all errors from this library and referenced
    // libraries (modulo the --show-sdk-warnings flag).

    errors.addAll(Arrays.asList(unit.getErrors()));

    return getMaxErrorSeverity(errors);
  }

  /**
   * Create the serialized element file for the SDK.
   * 
   * @return true on success, false if an error occurred
   */
  public boolean createSdkIndex() {
    @SuppressWarnings("unused")
    DartSdk sdk = new DirectoryBasedDartSdk(options.getDartSdkPath());

    try {
      // TODO(devoncarew): call analysis engine methods to create an index file

      FileOutputStream out = new FileOutputStream(options.getSdkIndexLocation());
      out.write(0);
      out.write(0);
      out.write(0);
      out.write(0);
      out.close();

      return true;
    } catch (IOException ioe) {
      ioe.printStackTrace();

      return false;
    }
  }

}
