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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.analysis.model.ProjectManagerImpl;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockWorkspace;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AnalysisMarkerManagerTest extends TestCase {

  private MockWorkspace workspace;
  private MockWorkspaceRoot rootRes;
  private MockProject projectRes;
  private MockFile fileRes;
  private DartSdk sdk;
  private ProjectManagerImpl projectManager;
  private Project project;
  private AnalysisContext context;
  private FileBasedSource source;

  private CountDownLatch fileDeleteMarkerStartLatch;
  private CountDownLatch fileDeleteMarkerEndLatch;

  public void test_getInstance() {
    assertNotNull(AnalysisMarkerManager.getInstance());
  }

  public void test_stop() throws Exception {
    fileDeleteMarkerStartLatch = new CountDownLatch(1);
    fileDeleteMarkerEndLatch = new CountDownLatch(1);

    AnalysisMarkerManager markerManager = new AnalysisMarkerManager(workspace);

    LineInfo lineInfo = new LineInfo(new int[] {0, 10});
    ParserErrorCode errCode = ParserErrorCode.DIRECTIVE_AFTER_DECLARATION;
    AnalysisError[] errors = new AnalysisError[] {new AnalysisError(source, 0, 10, errCode)};
    markerManager.queueErrors(fileRes, lineInfo, errors);
    markerManager.queueErrors(fileRes, lineInfo, errors);
    markerManager.done();

    // Wait for once cycle before stopping the background thread
    fileDeleteMarkerStartLatch.await(10000, TimeUnit.MILLISECONDS);
    markerManager.stop();
    fileDeleteMarkerEndLatch.countDown();
    markerManager.waitForMarkers(10000);

    // Assert background thread exited before 2nd cycle
    fileRes.assertMarkersDeleted();
    fileRes.assertMarkersNotDeleted();
  }

  public void test_translateMarkers() throws Exception {
    AnalysisMarkerManager markerManager = new AnalysisMarkerManager(workspace);

    LineInfo lineInfo = new LineInfo(new int[] {0, 10});
    ParserErrorCode errCode = ParserErrorCode.DIRECTIVE_AFTER_DECLARATION;
    AnalysisError[] errors = new AnalysisError[] {new AnalysisError(source, 0, 10, errCode)};
    markerManager.queueErrors(fileRes, lineInfo, errors);
    markerManager.done();
    markerManager.waitForMarkers(10000);

    fileRes.assertMarkersDeleted();
    assertTrue(fileRes.getMarkers().size() > 0);
  }

  @Override
  protected void setUp() {
    workspace = new MockWorkspace();
    rootRes = workspace.getRoot();
    projectRes = rootRes.add(new MockProject(rootRes, getClass().getSimpleName()));

    fileRes = projectRes.add(new MockFile(projectRes, "a.dart", "library a;#") {
      @Override
      public void deleteMarkers(String type, boolean includeSubtypes, int depth)
          throws CoreException {

        // Notify test_stop that this method has been called
        if (fileDeleteMarkerStartLatch != null) {
          fileDeleteMarkerStartLatch.countDown();
        }
        super.deleteMarkers(type, includeSubtypes, depth);

        // Wait for test_stop to signal the background thread to continue
        if (fileDeleteMarkerEndLatch != null) {
          try {
            fileDeleteMarkerEndLatch.await(10000, TimeUnit.MILLISECONDS);
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
        }
      }
    });

    sdk = DirectoryBasedDartSdk.getDefaultSdk();
    projectManager = new ProjectManagerImpl(rootRes, sdk, new DartIgnoreManager());
    project = projectManager.getProject(projectRes);
    context = project.getDefaultContext();

    File file = fileRes.getLocation().toFile();
    source = new FileBasedSource(context.getSourceFactory().getContentCache(), file);
  }
}
