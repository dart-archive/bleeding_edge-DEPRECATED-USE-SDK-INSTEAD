/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.core.generator;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import junit.framework.Assert;

import org.eclipse.core.resources.IFile;

import static org.junit.Assert.assertNotNull;

public class GeneratorUtils {

  /**
   * Assert that there are no analysis errors in the given compilation unit.
   * 
   * @param file
   * @throws AnalysisException
   */
  public static void assertNoAnalysisErrors(IFile file) throws AnalysisException {
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    DartSdk sdk = DirectoryBasedDartSdk.getDefaultSdk();
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    context.setSourceFactory(sourceFactory);

    Source librarySource = new FileBasedSource(
        sourceFactory.getContentCache(),
        file.getLocation().toFile());
    context.computeLibraryElement(librarySource);

    AnalysisError[] errors = context.getErrors(librarySource).getErrors();
    assertNotNull(errors);

    if (errors.length > 0) {
      StringBuilder builder = new StringBuilder();
      builder.append("Expected 0 errors, found " + errors.length + ":\n");

      for (AnalysisError error : errors) {
        builder.append(error + "\n");
      }

      Assert.fail(builder.toString().trim());
    }
  }

}
