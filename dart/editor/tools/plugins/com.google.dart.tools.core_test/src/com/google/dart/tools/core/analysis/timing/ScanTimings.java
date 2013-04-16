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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.index.RelationshipCallback;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectListener;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.internal.analysis.model.ProjectImpl;
import com.google.dart.tools.core.internal.analysis.model.ProjectImpl.AnalysisContextFactory;
import com.google.dart.tools.core.internal.builder.AnalysisEngineParticipant;
import com.google.dart.tools.core.internal.builder.AnalysisMarkerManager;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.core.internal.builder.DeltaProcessor;
import com.google.dart.tools.core.internal.builder.MockContext;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.mock.MockWorkspace;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import static org.mockito.Mockito.mock;

import java.io.File;

/**
 * Evaluate time to scan a directory using {@link File} versus {@link IResourceProxyVisitor}
 */
public class ScanTimings extends TestCase {

  /**
   * Simplified analysis context for performing {@link AnalysisEngineParticipant} timings
   */
  private final class MockContextForScan extends MockContext {
    final CompilationUnit compilationUnit = new CompilationUnit(null, null, null, null, null);

    @Override
    public AnalysisContext extractContext(SourceContainer container) {
      contextCount++;
      return new MockContextForScan();
    }

    @Override
    public CompilationUnit parseCompilationUnit(Source source) throws AnalysisException {
      return compilationUnit;
    }
  }

  private final class MockIndexForScan implements Index {
    @Override
    public void getRelationships(Element element, Relationship relationship,
        RelationshipCallback callback) {
      // ignored
    }

    @Override
    public String getStatistics() {
      // ignored
      return null;
    }

    @Override
    public void indexUnit(AnalysisContext context, CompilationUnit unit) {
      // ignored
    }

    @Override
    public void removeContext(AnalysisContext context) {
      // ignored
    }

    @Override
    public void removeSource(AnalysisContext context, Source source) {
      // ignored
    }

    @Override
    public void removeSources(AnalysisContext context, SourceContainer container) {
      // ignored
    }

    @Override
    public void run() {
      // ignored
    }

    @Override
    public void stop() {
      // ignored
    }
  }

  private final class MockProjectManagerForScan implements ProjectManager {
    private final MockIndexForScan index = new MockIndexForScan();
    private final DartIgnoreManager ignoreManager = new DartIgnoreManager();
    private ProjectImpl project;

    @Override
    public void addProjectListener(ProjectListener listener) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addWorker(AnalysisWorker worker) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AnalysisContext getContext(IResource resource) {
      throw new UnsupportedOperationException();
    }

    @Override
    public HtmlElement getHtmlElement(IFile file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public IResource getHtmlFileForLibrary(Source source) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DartIgnoreManager getIgnoreManager() {
      return ignoreManager;
    }

    @Override
    public Index getIndex() {
      return index;
    }

    @Override
    public Source[] getLaunchableClientLibrarySources() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Source[] getLaunchableClientLibrarySources(IProject project) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Source[] getLaunchableServerLibrarySources() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Source[] getLaunchableServerLibrarySources(IProject project) {
      throw new UnsupportedOperationException();
    }

    @Override
    public LibraryElement[] getLibraries(IContainer container) {
      throw new UnsupportedOperationException();
    }

    @Override
    public LibraryElement getLibraryElement(IFile file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public LibraryElement getLibraryElementOrNull(IFile file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Source[] getLibrarySources(IFile resource) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Source[] getLibrarySources(IProject project) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Project getProject(IProject resource) {
      if (project == null) {
        project = new ProjectImpl(
            resource,
            mock(DartSdk.class),
            index,
            new AnalysisContextFactory() {
              @Override
              public AnalysisContext createContext() {
                contextCount++;
                return new MockContextForScan();
              }
            });
      }
      return project;
    }

    @Override
    public Project[] getProjects() {
      throw new UnsupportedOperationException();
    }

    @Override
    public PubFolder getPubFolder(IResource resource) {
      throw new UnsupportedOperationException();
    }

    @Override
    public IWorkspaceRoot getResource() {
      throw new UnsupportedOperationException();
    }

    @Override
    public IResource getResource(Source source) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DartSdk getSdk() {
      throw new UnsupportedOperationException();
    }

    @Override
    public AnalysisContext getSdkContext() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Source getSource(IFile file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SourceKind getSourceKind(IFile file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClientLibrary(Source librarySource) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isServerLibrary(Source librarySource) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SearchEngine newSearchEngine() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void projectAnalyzed(Project project) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void removeProjectListener(ProjectListener listener) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void removeWorker(AnalysisWorker analysisWorker) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void start() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void stop() {
      throw new UnsupportedOperationException();
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

  private void runScanWithParticipant(final IProject project) throws CoreException {
    fileCount = 0;
    dartCount = 0;
    contextCount = 0;
    final long[] delta = new long[1];
    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        long start = System.currentTimeMillis();
        scanWithParticipant(project);
        delta[0] = System.currentTimeMillis() - start;
      }
    }, null);
    System.out.println("IRes: " + fileCount + " (" + dartCount + ") files in " + delta[0]
        + " ms in " + contextCount + " contexts");
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

  private void scanWithParticipant(final IProject project) throws CoreException {
    IProgressMonitor monitor = new NullProgressMonitor();
    MockWorkspace workspace = new MockWorkspace();
    final AnalysisEngineParticipant participant = new AnalysisEngineParticipant(
        true,
        new MockProjectManagerForScan(),
        new AnalysisMarkerManager(workspace)) {

      @Override
      protected DeltaProcessor createProcessor(Project project) {
        return new DeltaProcessor(project) {
          @Override
          protected boolean visitPackagesProxy(IResourceProxy proxy, String name, File packageDir,
              IPath packagePath) {
            checkName(name);
            return super.visitPackagesProxy(proxy, name, packageDir, packagePath);
          }

          @Override
          protected boolean visitProxy(IResourceProxy proxy, String name) {
            checkName(name);
            return super.visitProxy(proxy, name);
          }
        };
      }
    };
    participant.build(new BuildEvent(project, null, monitor), monitor);
  }
}
