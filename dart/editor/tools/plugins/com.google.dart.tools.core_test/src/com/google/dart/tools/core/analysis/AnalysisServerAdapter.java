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
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;

/**
 * {@link AnalysisServer} subclass that intercepts requests to analyze context
 */
class AnalysisServerAdapter extends AnalysisServer {
  private boolean analyzeContext = false;

  public AnalysisServerAdapter() {
    this(PackageLibraryManagerProvider.getAnyLibraryManager());
  }

  public AnalysisServerAdapter(PackageLibraryManager libraryManager) {
    super(libraryManager);
  }

  public void assertAnalyzeContext(boolean expectedState) {
    if (analyzeContext != expectedState) {
      AnalyzeLibraryTaskTest.fail(
          "Expected background analysis " + expectedState + " but found " + analyzeContext);
    }
  }

  public void resetAnalyzeContext() {
    analyzeContext = false;
  }

  @Override
  protected void queueAnalyzeContext() {
    analyzeContext = true;
  }
}
