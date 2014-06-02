/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.server.timing;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisResult;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.SourceFactory;

import java.io.File;
import java.util.ArrayList;

/**
 * Instances of the class {@code AnalyzeEngineInContext} use an analysis context to analyze the
 * analysis engine.
 */
public class AnalyzeEngineInContext extends TimingTest {
  /**
   * The root of the SDK directory structure.
   */
  private File sdkDirectory;

  /**
   * The root of the SVN directory structure.
   */
  private File svnDirectory;

  /**
   * The root of the analysis engine's directory structure.
   */
  private File engineDirectory;

  /**
   * Initialize a newly created timing test.
   */
  public AnalyzeEngineInContext() {
    super("Analyze engine locally");
  }

  @Override
  protected void oneTimeSetUp() throws Exception {
    //
    // Get the root of the SDK directory structure.
    //
    sdkDirectory = DirectoryBasedDartSdk.getDefaultSdkDirectory();
    if (sdkDirectory == null) {
      throw new IllegalStateException(
          "Missing or invalid property value: set using -Dcom.google.dart.sdk=...");
    }
    //
    // Get the root of the SVN directory structure.
    //
    String svnRootName = System.getProperty("svnRoot");
    if (svnRootName == null) {
      throw new IllegalStateException("Missing property value: set using -DsvnRoot=...");
    }
    svnDirectory = new File(svnRootName);
    if (!svnDirectory.isDirectory()) {
      throw new IllegalStateException("Invalid property value: svnRoot directory does not exist");
    }
    //
    // Compute the root of the code to be analyzed.
    //
    String enginePath = buildPath(svnDirectory, new String[] {"pkg", "analyzer"});
    engineDirectory = new File(enginePath);
    if (!engineDirectory.isDirectory()) {
      throw new IllegalStateException("Invalid path to analysis engine: " + enginePath);
    }
  }

  @Override
  protected void perform() {
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    //
    // Set the analysis options.
    //
//    AnalysisOptionsImpl options = new AnalysisOptionsImpl(context.getAnalysisOptions());
//    context.setAnalysisOptions(options);
    //
    // Configure the source factory.
    //
    SourceFactory factory = new SourceFactory(
        new DartUriResolver(new DirectoryBasedDartSdk(sdkDirectory)),
        new PackageUriResolver(new File(engineDirectory, "packages")),
        new FileUriResolver());
    context.setSourceFactory(factory);
    //
    // Add the files.
    //
    ChangeSet changeSet = new ChangeSet();
    ArrayList<File> files = computeFiles(engineDirectory);
    int fileCount = files.size();
    for (int i = 0; i < fileCount; i++) {
      changeSet.addedSource(new FileBasedSource(files.get(i)));
    }
    context.applyChanges(changeSet);
    //
    // Perform analysis.
    //
    AnalysisResult result = context.performAnalysisTask();
    while (result.hasMoreWork()) {
      result = context.performAnalysisTask();
    }
  }
}
