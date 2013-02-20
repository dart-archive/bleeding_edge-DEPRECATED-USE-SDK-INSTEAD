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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectListener;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockProject;

import static com.google.dart.tools.core.internal.builder.TestProjects.MONITOR;
import static com.google.dart.tools.core.internal.builder.TestProjects.newPubProject3;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;

import java.util.ArrayList;

public class AnalysisEngineParticipantTest extends AbstractDartCoreTest {

  /**
   * Mock {@link DeltaProcessor} for testing {@link AnalysisEngineParticipant}
   */
  private class MockDeltaProcessor extends DeltaProcessor {

    private ArrayList<Object> called = new ArrayList<Object>();

    public MockDeltaProcessor(Project project) {
      super(project);
    }

    @Override
    public void traverse(IContainer resource) throws CoreException {
      called.add(resource);
    }

    @Override
    public void traverse(IResourceDelta delta) throws CoreException {
      called.add(delta);
    }

    void assertNoCalls() {
      if (called.size() > 0) {
        fail("Unexpected call " + called.get(0));
      }
    }

    void assertTraversed(Object arg) {
      if (called.size() == 0) {
        fail("Expected traverse " + arg);
      }
      Object call = called.remove(0);
      assertSame(arg, call);
    }
  }

  /**
   * Mock {@link Project} for testing {@link AnalysisEngineParticipant}
   */
  private class MockProjectImpl implements Project {

    private final IProject resource;

    public MockProjectImpl(IProject resource) {
      this.resource = resource;
    }

    @Override
    public void discardContextsIn(IContainer container) {
      throw new RuntimeException("Unexpected call");
    }

    @Override
    public AnalysisContext getContext(IResource resource) {
      throw new RuntimeException("Unexpected call");
    }

    @Override
    public AnalysisContext getDefaultContext() {
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
    public PubFolder getPubFolder(IResource resource) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PubFolder[] getPubFolders() {
      throw new UnsupportedOperationException();
    }

    @Override
    public IProject getResource() {
      return resource;
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
    public Source getSource(IFile file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SourceKind getSourceKind(IFile file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void pubspecAdded(IContainer container) {
      throw new RuntimeException("Unexpected call");
    }

    @Override
    public void pubspecRemoved(IContainer container) {
      throw new RuntimeException("Unexpected call");
    }
  }

  /**
   * Mock {@link ProjectManager} for testing {@link AnalysisEngineParticipant}
   */
  private final class MockProjectManager implements ProjectManager {
    private final Index index = IndexFactory.newIndex(IndexFactory.newMemoryIndexStore());
    private final DartIgnoreManager ignoreManager = new DartIgnoreManager();
    private final ArrayList<Project> analyzed = new ArrayList<Project>();
    private MockProjectImpl project;

    @Override
    public void addProjectListener(ProjectListener listener) {
      throw new UnsupportedOperationException();
    }

    public void assertProjectAnalyzed(Project expected) {
      assertEquals(1, analyzed.size());
      assertSame(expected, analyzed.get(0));
      analyzed.clear();
    }

    @Override
    public AnalysisContext getContext(IResource resource) {
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
    public LibraryElement getLibraryElement(IFile file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public LibraryElement getLibraryElementOrNull(IFile file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Project getProject(IProject resource) {
      if (project == null) {
        project = new MockProjectImpl(resource);
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
    public Source getSource(IFile file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SourceKind getSourceKind(IFile file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SearchEngine newSearchEngine() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void projectAnalyzed(Project project) {
      analyzed.add(project);
    }

    @Override
    public void removeProjectListener(ProjectListener listener) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Specialized {@link AnalysisEngineParticipant} that returns a mock context for recording what
   * analysis is requested rather than a context that would actually analyze the source.
   */
  private class Target extends AnalysisEngineParticipant {
    private MockDeltaProcessor processor;

    Target() {
      super(true, manager);
    }

    @Override
    protected DeltaProcessor createProcessor(Project project) {
      assertNotNull(project);
      if (processor == null) {
        processor = new MockDeltaProcessor(project);
      }
      return processor;
    }
  }

  private MockProject projectContainer;
  private MockProjectManager manager = new MockProjectManager();
  private Target participant;

  public void test_build_delta() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    participant.build(new BuildEvent(projectContainer, delta, MONITOR), MONITOR);
    participant.processor.assertTraversed(delta);
    participant.processor.assertTraversed(delta);
    participant.processor.assertNoCalls();
    manager.assertProjectAnalyzed(manager.getProject(projectContainer));

    participant.build(new BuildEvent(projectContainer, delta, MONITOR), MONITOR);
    participant.processor.assertTraversed(delta);
    participant.processor.assertTraversed(delta);
    participant.processor.assertNoCalls();
    manager.assertProjectAnalyzed(manager.getProject(projectContainer));
  }

  public void test_build_noDelta() throws Exception {
    participant.build(new BuildEvent(projectContainer, null, MONITOR), MONITOR);
    participant.processor.assertTraversed(projectContainer);
    participant.processor.assertTraversed(projectContainer);
    participant.processor.assertNoCalls();
    manager.assertProjectAnalyzed(manager.getProject(projectContainer));

    participant.build(new BuildEvent(projectContainer, null, MONITOR), MONITOR);
    participant.processor.assertTraversed(projectContainer);
    participant.processor.assertTraversed(projectContainer);
    participant.processor.assertNoCalls();
    manager.assertProjectAnalyzed(manager.getProject(projectContainer));
  }

  @Override
  protected void setUp() throws Exception {
    projectContainer = newPubProject3();
    participant = new Target();
  }
}
