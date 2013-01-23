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
import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockProject;

import static com.google.dart.tools.core.internal.builder.TestProjects.MONITOR;
import static com.google.dart.tools.core.internal.builder.TestProjects.newPubProject3;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
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
    public AnalysisContext getContext(IContainer container) {
      throw new RuntimeException("Unexpected call");
    }

    @Override
    public IProject getResource() {
      return resource;
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
   * Specialized {@link AnalysisEngineParticipant} that returns a mock context for recording what
   * analysis is requested rather than a context that would actually analyze the source.
   */
  private class Target extends AnalysisEngineParticipant {

    private MockProjectImpl project;
    MockDeltaProcessor processor;

    Target(MockProject resource) {
      super(true);
    }

    @Override
    protected DeltaProcessor createProcessor(Project project) {
      assertNotNull(project);
      if (processor == null) {
        processor = new MockDeltaProcessor(project);
      }
      return processor;
    }

    @Override
    protected Project createProject(IProject resource) {
      project = new MockProjectImpl(resource);
      return project;
    }
  }

  private MockProject projectContainer;
  private Target participant;

  public void test_build_delta() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    participant.build(new BuildEvent(projectContainer, delta, MONITOR), MONITOR);
    participant.processor.assertTraversed(projectContainer);
    participant.processor.assertTraversed(delta);
    participant.processor.assertTraversed(delta);
    participant.processor.assertNoCalls();

    participant.build(new BuildEvent(projectContainer, delta, MONITOR), MONITOR);
    participant.processor.assertTraversed(delta);
    participant.processor.assertTraversed(delta);
    participant.processor.assertNoCalls();
  }

  public void test_build_noDelta() throws Exception {
    participant.build(new BuildEvent(projectContainer, null, MONITOR), MONITOR);
    participant.processor.assertTraversed(projectContainer);
    participant.processor.assertTraversed(projectContainer);
    participant.processor.assertNoCalls();

    participant.build(new BuildEvent(projectContainer, null, MONITOR), MONITOR);
    participant.processor.assertTraversed(projectContainer);
    participant.processor.assertTraversed(projectContainer);
    participant.processor.assertNoCalls();
  }

  @Override
  protected void setUp() throws Exception {
    projectContainer = newPubProject3();
    participant = new Target(projectContainer);
  }
}
