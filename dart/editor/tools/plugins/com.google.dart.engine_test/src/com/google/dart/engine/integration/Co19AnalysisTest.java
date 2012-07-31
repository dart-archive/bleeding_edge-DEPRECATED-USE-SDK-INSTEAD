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
package com.google.dart.engine.integration;

import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.FileUtilities;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;

public class Co19AnalysisTest extends DirectoryBasedSuiteBuilder {
  public static Test suite() {
    String directoryName = System.getProperty("co19Directory");
    if (directoryName != null) {
      File directory = new File(directoryName);
      return new SDKAnalysisTest().buildSuite(directory, "Analyze co19 files");
    }
    return new TestSuite("Analyze co19 files");
  }

  @Override
  protected void testSingleFile(File sourceFile) throws IOException {
    String contents = FileUtilities.getContents(sourceFile);
    boolean errorExpected = contents.indexOf("@compile-error") > 0
        || contents.indexOf("@static-warning") > 0;

    Source source = new SourceFactory().forFile(sourceFile);
    GatheringErrorListener listener = new GatheringErrorListener();
    StringScanner scanner = new StringScanner(source, contents, listener);
    Token token = scanner.tokenize();
    if (listener.getErrors().size() > 0) {
      if (errorExpected) {
        return;
      } else {
        listener.assertNoErrors();
      }
    }
    Parser parser = new Parser(source, listener);
    parser.parseCompilationUnit(token);
    if (errorExpected) {
      Assert.assertTrue("Expected errors", listener.getErrors().size() > 0);
    } else {
      listener.assertNoErrors();
    }
  }
}
