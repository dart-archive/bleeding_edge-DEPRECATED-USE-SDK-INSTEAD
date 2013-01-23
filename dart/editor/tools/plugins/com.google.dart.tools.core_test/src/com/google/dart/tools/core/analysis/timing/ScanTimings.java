/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.analysis.timing;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.internal.analysis.model.ProjectImpl;
import com.google.dart.tools.core.internal.builder.AnalysisEngineParticipant;
import com.google.dart.tools.core.internal.builder.DeltaProcessor;
import com.google.dart.tools.core.internal.builder.MockContext;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import java.io.File;

/**
 * Evaluate time to scan a directory using {@link File} versus {@link IResourceProxyVisitor}
 */
public class ScanTimings extends TestCase {

  /**
   * Simplified analysis context for performing {@link AnalysisEngineParticipant} timings
   */
  private final class MockContextForScan extends MockContext {
    @Override
    public AnalysisContext extractAnalysisContext(SourceContainer container) {
      contextCount++;
      return new MockContextForScan();
    }
  }

  private int fileCount;
  private int dartCount;
  private int contextCount;

  public void test_large_scan() throws Exception {

    String key = getClass().getSimpleName() + ".dir";
    String dirPath = System.getProperty(key);
    assertNotNull("Must define system property " + key, dirPath);
    File dir = new File(dirPath);
    assertTrue("Must exist " + dirPath, dir.exists());
    System.out.println("Scanning files in " + dirPath);

    System.out.println("\nScan using java.io.File");
    runScanFiles(dir);
    runScanFiles(dir);
    runScanFiles(dir);

    System.out.println("\nOpening project...");
    IProject project = createProject(dirPath);

    System.out.println("\nScan using proxy...");
    runScanProxies(project);
    runScanProxies(project);
    runScanProxies(project);

    System.out.println("\nScan using proxy and container");
    runScanProxiesAndContainers(project);
    runScanProxiesAndContainers(project);
    runScanProxiesAndContainers(project);

    System.out.println("\nScan using proxy and container and dart source");
    runScanProxiesAndSource(project);
    runScanProxiesAndSource(project);
    runScanProxiesAndSource(project);

    System.out.println("\nScan all resources");
    runScanResources(project);
    runScanResources(project);
    runScanResources(project);

    System.out.println("\nScan with participant");
    runScanWithParticipant(project);
    runScanWithParticipant(project);
    runScanWithParticipant(project);
  }

  private void checkName(String name) {
    fileCount++;
    if (DartCore.isDartLikeFileName(name)) {
      dartCount++;
    }
  }

  private IProject createProject(String dirPath) throws CoreException {
    long start = System.currentTimeMillis();
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    String projectName = "test";
    IProject project = root.getProject(projectName);
    IProjectDescription description = workspace.newProjectDescription(projectName);
    description.setLocation(new Path(dirPath));
    project.create(description, null);
    project.open(null);
    long delta = System.currentTimeMillis() - start;
    System.out.println("Open project in " + delta + " ms");
    return project;
  }

  private void runScanFiles(File dir) {
    fileCount = 0;
    dartCount = 0;
    long start = System.currentTimeMillis();
    scanFiles(dir);
    long delta = System.currentTimeMillis() - start;
    System.out.println("File: " + fileCount + " (" + dartCount + ") files in " + delta + " ms");
  }

  private void runScanProxies(IProject project) throws CoreException {
    fileCount = 0;
    dartCount = 0;
    long start = System.currentTimeMillis();
    scanProxies(project);
    long delta = System.currentTimeMillis() - start;
    System.out.println("IRes: " + fileCount + " (" + dartCount + ") files in " + delta + " ms");
  }

  private void runScanProxiesAndContainers(IProject project) throws CoreException {
    fileCount = 0;
    dartCount = 0;
    long start = System.currentTimeMillis();
    scanProxiesAndContainers(project);
    long delta = System.currentTimeMillis() - start;
    System.out.println("IRes: " + fileCount + " (" + dartCount + ") files in " + delta + " ms");
  }

  private void runScanProxiesAndSource(IProject project) throws CoreException {
    fileCount = 0;
    dartCount = 0;
    long start = System.currentTimeMillis();
    scanProxiesAndSource(project);
    long delta = System.currentTimeMillis() - start;
    System.out.println("IRes: " + fileCount + " (" + dartCount + ") files in " + delta + " ms");
  }

  private void runScanResources(IProject project) throws CoreException {
    fileCount = 0;
    dartCount = 0;
    long start = System.currentTimeMillis();
    scanResources(project);
    long delta = System.currentTimeMillis() - start;
    System.out.println("IRes: " + fileCount + " (" + dartCount + ") files in " + delta + " ms");
  }

  private void runScanWithParticipant(IProject project) throws CoreException {
    fileCount = 0;
    dartCount = 0;
    contextCount = 0;
    long start = System.currentTimeMillis();
    scanWithParticipant(project);
    long delta = System.currentTimeMillis() - start;
    System.out.println("IRes: " + fileCount + " (" + dartCount + ") files in " + delta + " ms in "
        + contextCount + " contexts");
  }

  private void scanFiles(File dir) {
    String[] names = dir.list();
    for (String name : names) {
      checkName(name);
      File file = new File(dir, name);
      if (file.isDirectory()) {
        scanFiles(file);
      }
    }
  }

  private void scanProxies(IProject project) throws CoreException {
    project.accept(new IResourceProxyVisitor() {
      @Override
      public boolean visit(IResourceProxy proxy) throws CoreException {
        checkName(proxy.getName());
//        proxy.requestFullPath();
//        proxy.requestResource();
        return true;
      }
    }, 0);
  }

  private void scanProxiesAndContainers(IProject project) throws CoreException {
    project.accept(new IResourceProxyVisitor() {
      @Override
      public boolean visit(IResourceProxy proxy) throws CoreException {
        checkName(proxy.getName());
//        proxy.requestFullPath();
        if (proxy.getType() != IResource.FILE) {
          proxy.requestResource();
        }
        return true;
      }
    }, 0);
  }

  private void scanProxiesAndSource(IProject project) throws CoreException {
    project.accept(new IResourceProxyVisitor() {
      @Override
      public boolean visit(IResourceProxy proxy) throws CoreException {
        checkName(proxy.getName());
//        proxy.requestFullPath();
        if (proxy.getType() != IResource.FILE || proxy.getName().endsWith(".dart")) {
          proxy.requestResource();
        }
        return true;
      }
    }, 0);
  }

  private void scanResources(IProject project) throws CoreException {
    project.accept(new IResourceVisitor() {
      @Override
      public boolean visit(IResource resource) throws CoreException {
        checkName(resource.getName());
        return true;
      }
    });
  }

  private void scanWithParticipant(IProject project) throws CoreException {
    IProgressMonitor monitor = new NullProgressMonitor();
    AnalysisEngineParticipant participant = new AnalysisEngineParticipant(true) {
      private ProjectImpl project;

      @Override
      protected DeltaProcessor createProcessor(Project project) {
        return new DeltaProcessor(project) {
          @Override
          protected boolean visitPackagesProxy(IResourceProxy proxy, String name) {
            checkName(name);
            return super.visitPackagesProxy(proxy, name);
          }

          @Override
          protected boolean visitProxy(IResourceProxy proxy, String name) {
            checkName(name);
            return super.visitProxy(proxy, name);
          }
        };
      }

      @Override
      protected Project createProject(IProject resource) {
        project = new ProjectImpl(resource) {
          @Override
          protected AnalysisContext createDefaultContext() {
            contextCount++;
            return new MockContextForScan();
          }
        };
        return project;
      }
    };
    participant.build(new BuildEvent(project, null, monitor), monitor);
  }
}
