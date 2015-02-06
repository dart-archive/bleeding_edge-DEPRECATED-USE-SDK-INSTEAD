/*
 * Copyright (c) 2015, the Dart project authors.
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
package com.google.dart.eclipse.core.build;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.CleanEvent;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Performs a reanalyze sources for clean build
 */
public class DartBuildParticipant implements BuildParticipant {

  @Override
  public void build(BuildEvent event, IProgressMonitor monitor) throws CoreException {
    // no op
  }

  @Override
  public void clean(CleanEvent event, IProgressMonitor monitor) throws CoreException {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      DartCore.getAnalysisServer().analysis_reanalyze(null);
    }
  }
}
