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

import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.server.AnalysisError;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.AnalysisStatus;
import com.google.dart.server.CompletionSuggestion;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.Outline;
import com.google.dart.server.ServerStatus;
import com.google.dart.server.StdioServerSocket;
import com.google.dart.server.internal.remote.RemoteAnalysisServerImpl;

import java.io.File;
import java.util.ArrayList;

/**
 * Instances of the class {@code AnalyzeEngineInServer} use an analysis server to analyze the
 * analysis server.
 */
public class AnalyzeEngineInServer extends TimingTest {
  /**
   * Instances of the class {@code ServerListener} listen for notifications from a server, recording
   * the state of analysis.
   */
  private static final class ServerListener implements AnalysisServerListener {
    /**
     * The most recently recorded value of the isAnalyzing flag from the server.
     */
    private boolean isAnalyzing = true;

    @Override
    public void computedCompletion(String completionId, CompletionSuggestion[] completions,
        boolean last) {
    }

    @Override
    public void computedErrors(String file, AnalysisError[] errors) {
    }

    @Override
    public void computedHighlights(String file, HighlightRegion[] highlights) {
    }

    @Override
    public void computedNavigation(String file, NavigationRegion[] targets) {
    }

    @Override
    public void computedOutline(String file, Outline outline) {
    }

    @Override
    public void serverConnected() {
    }

    @Override
    public void serverError(boolean isFatal, String message, String stackTrace) {
    }

    @Override
    public void serverStatus(ServerStatus status) {
      AnalysisStatus analysisStatus = status.getAnalysisStatus();
      if (analysisStatus != null) {
        isAnalyzing = analysisStatus.isAnalyzing();
      }
    }
  }

  /**
   * The root of the SDK directory structure.
   */
  private String sdkPath;

  /**
   * The path to the root of the analyzer directory structure.
   */
  private String enginePath;

  /**
   * The analysis server that will be used to analyze the analysis engine.
   */
  private AnalysisServer server;

  /**
   * The listener used to watch for the server to complete.
   */
  private ServerListener listener;

  /**
   * Initialize a newly created timing test.
   */
  public AnalyzeEngineInServer() {
    super("Analyze engine in Server");
  }

  @Override
  protected void oneTimeSetUp() throws Exception {
    //
    // Get the root of the SDK directory structure.
    //
    File sdkDirectory = DirectoryBasedDartSdk.getDefaultSdkDirectory();
    if (sdkDirectory == null) {
      throw new IllegalStateException(
          "Missing or invalid property value: set using -Dcom.google.dart.sdk=...");
    }
    sdkPath = sdkDirectory.getAbsolutePath();
    //
    // Get the root of the SVN directory structure.
    //
    String svnPath = System.getProperty("svnRoot");
    if (svnPath == null) {
      throw new IllegalStateException("Missing property value: set using -DsvnRoot=...");
    }
    File svnDirectory = new File(svnPath);
    if (!svnDirectory.exists()) {
      throw new IllegalStateException("Invalid property value: svnRoot directory does not exist");
    }
    //
    // Compute the root of the code to be analyzed.
    //
    enginePath = buildPath(svnDirectory, new String[] {"pkg", "analyzer"});
    //
    // Create the analysis server.
    //
    String runtimePath = buildPath(svnDirectory, new String[] {"sdk", "bin", "dart"});
    String analysisServerPath = buildPath(svnDirectory, new String[] {
        "pkg", "analysis_server", "bin", "server.dart"});
    StdioServerSocket serverSocket = new StdioServerSocket(runtimePath, analysisServerPath, null);
    serverSocket.start(false);
    server = new RemoteAnalysisServerImpl(
        serverSocket.getRequestSink(),
        serverSocket.getResponseStream(),
        serverSocket.getErrorStream());
  }

  @Override
  protected void oneTimeTearDown() {
    server.shutdown();
  }

  @Override
  protected void perform() {
    //
    // This test sends the following commands to the server:
    //
    // {"id":"0","method":"analysis.setAnalysisRoots","params":{
    //   "included":["$enginePath"],
    //   "excluded":[]}}
    // {"id":"1","method":"analysis.setAnalysisRoots","params":{
    //   "included":[],
    //   "excluded":[]}}
    // {"id":"2","method":"server.shutdown"}
    //
    // Set the analysis options.
    //
//    AnalysisOptions options = new AnalysisOptions();
//    // TODO Populate the options with at least the default subscriptions.
//    server.updateAnalysisOptions(options);
    //
    // Configure the SDK.
    //
//    ArrayList<String> added = new ArrayList<String>();
//    added.add(sdkPath);
//    ArrayList<String> removed = new ArrayList<String>();
//    server.updateSdks(added, removed, sdkPath);
    //
    // Configure the analysis roots.
    //
    ArrayList<String> includedPaths = new ArrayList<String>();
    includedPaths.add(enginePath);
    ArrayList<String> excludedPaths = new ArrayList<String>();
    server.setAnalysisRoots(includedPaths, excludedPaths);
    //
    // Wait for all work to be completed.
    //
    waitForAnalysis(20000);
  }

  @Override
  protected void setUp() {
    listener = new ServerListener();
    server.addAnalysisServerListener(listener);
  }

  @Override
  protected void tearDown() {
    server.removeAnalysisServerListener(listener);
    //
    // Clear the analysis roots.
    //
    ArrayList<String> includedPaths = new ArrayList<String>();
    ArrayList<String> excludedPaths = new ArrayList<String>();
    server.setAnalysisRoots(includedPaths, excludedPaths);
  }

  /**
   * Wait for up to the given number of milliseconds for analysis to complete. Return {@code true}
   * if analysis is still going when this method stopped waiting.
   * 
   * @param timeout the maximum number of milliseconds to wait for analysis to complete
   * @return {@code true} if analysis is still going when this method stopped waiting
   */
  private boolean waitForAnalysis(long timeout) {
    long endTime = System.currentTimeMillis() + timeout;
    while (listener.isAnalyzing && System.currentTimeMillis() < endTime) {
      Thread.yield();
    }
    if (listener.isAnalyzing) {
      System.out.println(System.currentTimeMillis() + "    Timed out");
    }
    return listener.isAnalyzing;
  }
}
