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
package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;

/**
 * {@link AnalysisServer} subclass that intercepts requests to analyze context
 */
class AnalysisServerAdapter extends AnalysisServer {
  private boolean analyzeContext = false;
  private HashSet<File> analyzeFiles = new HashSet<File>();

  public AnalysisServerAdapter() {
    this(PackageLibraryManagerProvider.getAnyLibraryManager());
  }

  public AnalysisServerAdapter(PackageLibraryManager libraryManager) {
    super(libraryManager);
  }

  public void assertAnalyze(boolean expected, File... expectedFiles) {
    if (analyzeContext != expected) {
      fail("Expected analyze context " + expected + " but found " + analyzeContext);
    }
    if (!wasAnalyzed(expectedFiles)) {
      failAnalyzed(expectedFiles);
    }
  }

  public void assertAnalyzeContext(boolean expectedState) {
    if (analyzeContext != expectedState) {
      fail("Expected background analysis " + expectedState + " but found " + analyzeContext);
    }
  }

  public void resetAnalyze() {
    analyzeContext = false;
    analyzeFiles.clear();
  }

  public void resetAnalyzeContext() {
    analyzeContext = false;
  }

  @Override
  protected void queueAnalyzeContext() {
    analyzeContext = true;
  }

  @Override
  protected void queueAnalyzeSubTask(File libraryFile) {
    analyzeFiles.add(libraryFile);
  }

  private void failAnalyzed(File... expectedFiles) {
    PrintStringWriter psw = new PrintStringWriter();
    psw.println("Expected " + expectedFiles.length + " library files analyzed, but found "
        + analyzeFiles.size());
    if (expectedFiles.length > 0) {
      psw.println("  expected:");
      for (File file : expectedFiles) {
        psw.println("    " + file.getPath());
      }
    }
    if (analyzeFiles.size() > 0) {
      psw.println("  found:");
      for (File file : analyzeFiles) {
        psw.println("    " + file);
      }
    }
    fail(psw.toString().trim());
  }

  private boolean wasAnalyzed(File... expectedFiles) {
    if (expectedFiles.length != analyzeFiles.size()) {
      return false;
    }
    for (File file : expectedFiles) {
      if (!analyzeFiles.contains(file)) {
        return false;
      }
    }
    return true;
  }
}
