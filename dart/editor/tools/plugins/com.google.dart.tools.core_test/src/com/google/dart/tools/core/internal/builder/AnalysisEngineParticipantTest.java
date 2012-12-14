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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceImpl;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockResource;
import com.google.dart.tools.core.test.AbstractDartCoreTest;

import static com.google.dart.tools.core.DartCore.BUILD_DART_FILE_NAME;
import static com.google.dart.tools.core.internal.builder.TestProjects.MONITOR;
import static com.google.dart.tools.core.internal.builder.TestProjects.newEmptyProject;
import static com.google.dart.tools.core.internal.builder.TestProjects.newPubProject1;
import static com.google.dart.tools.core.internal.builder.TestProjects.newPubProject2;
import static com.google.dart.tools.core.internal.builder.TestProjects.newSimpleProject;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class AnalysisEngineParticipantTest extends AbstractDartCoreTest {

  /**
   * Mock {@link AnalysisContext} that validates calls and returns Mocks rather than performing the
   * requested analysis.
   */
  private class MockContext implements AnalysisContext {
    private final Target target;
    private final Collection<Source> sources = new ArrayList<Source>();
    private SourceFactory sourceFactory;

    public MockContext(Target target) {
      this.target = target;
    }

    @Override
    public void clearResolution() {
      assertCall("clearResolution");
    }

    @Override
    public void directoryDeleted(File directory) {
      assertCall("directoryDeleted", directory);
    }

    @Override
    public void discard() {
      assertCall("discard");
    }

    @Override
    public AnalysisContext extractAnalysisContext(File directory) {
      assertCall("extractAnalysisContext", directory);
      return target.createMockContext(directory);
    }

    @Override
    public Collection<Source> getAvailableSources() {
      throw new RuntimeException("not implemented yet");
    }

    @Override
    public List<SourceContainer> getDependedOnContainers(SourceContainer container) {
      throw new RuntimeException("not implemented yet");
    }

    @Override
    public Element getElement(ElementLocation location) {
      throw new RuntimeException("not implemented yet");
    }

    @Override
    public AnalysisError[] getErrors(Source source) throws AnalysisException {
      throw new RuntimeException("not implemented yet");
    }

    @Override
    public HtmlElement getHtmlElement(Source source) {
      throw new RuntimeException("not implemented yet");
    }

    @Override
    public LibraryElement getLibraryElement(Source source) {
      throw new RuntimeException("not implemented yet");
    }

    @Override
    public SourceFactory getSourceFactory() {
      return sourceFactory;
    }

    @Override
    public void mergeAnalysisContext(AnalysisContext context) {
      throw new RuntimeException("not implemented yet");
    }

    @Override
    public CompilationUnit parse(Source source, AnalysisErrorListener errorListener)
        throws AnalysisException {
      throw new RuntimeException("not implemented yet");
    }

    @Override
    public CompilationUnit resolve(Source source, LibraryElement library,
        AnalysisErrorListener errorListener) throws AnalysisException {
      throw new RuntimeException("not implemented yet");
    }

    @Override
    public Token scan(Source source, AnalysisErrorListener errorListener) throws AnalysisException {
      throw new RuntimeException("not implemented yet");
    }

    @Override
    public void setSourceFactory(SourceFactory sourceFactory) {
      this.sourceFactory = sourceFactory;
    }

    @Override
    public void sourceAvailable(Source source) {
      assertCall("sourceAvailable", source);
      sources.add(source);
    }

    @Override
    public void sourceChanged(Source source) {
      assertCall("sourceChanged", source);
    }

    @Override
    public void sourceDeleted(Source source) {
      assertCall("sourceDeleted", source);
      sources.remove(source);
    }
  }

  /**
   * A mock used for matching {@link Call} arguments
   */
  private class MockSource implements Source {
    private final IFile file;

    public MockSource(IFile file) {
      if (file == null) {
        throw new IllegalArgumentException();
      }
      this.file = file;
    }

    public MockSource(MockProject project, String path) {
      this(project.getMockFile(path));
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof SourceImpl)) {
        return false;
      }
      return ((SourceImpl) obj).getFullName().equals(getFullName());
    }

    @Override
    public SourceContainer getContainer() {
      throw new RuntimeException("not implemented");
    }

    @Override
    public void getContents(ContentReceiver receiver) throws Exception {
      throw new RuntimeException("not implemented");
    }

    @Override
    public String getFullName() {
      return file.getLocation().toFile().getPath();
    }

    @Override
    public String getShortName() {
      return file.getName();
    }

    @Override
    public boolean isInSystemLibrary() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public void resetContainer() {
      throw new RuntimeException("not implemented");
    }

    @Override
    public Source resolve(String uri) {
      throw new RuntimeException("not implemented");
    }

    @Override
    public String toString() {
      return "Source[" + getFullName() + "]";
    }
  }

  /**
   * Specialized {@link AnalysisEngineParticipant} that returns a mock context for recording what
   * analysis is requested rather than a context that would actually analyze the source.
   */
  private class Target extends AnalysisEngineParticipant {

    private final MockProject project;

    Target(MockProject project) {
      super(true);
      this.project = project;
    }

    public MockContext createMockContext(File directory) {
      return new MockContext(this);
    }

    @Override
    public HashMap<IContainer, AnalysisContext> getAllContexts() {
      return super.getAllContexts();
    }

    public MockContext getContext(MockResource container) {
      return (MockContext) getAllContexts().get(container);
    }

    @Override
    protected AnalysisContext createRootContext() {
      assertCall("createRootContext");
      return createMockContext(project.getLocation().toFile());
    }
  }

  private ArrayList<Call> expectedCalls = new ArrayList<Call>();

  public void test_build_empty() throws Exception {
    MockProject project = newEmptyProject();
    Target target = new Target(project);
    expectCall("createRootContext");
    target.build(new BuildEvent(project, null, MONITOR), MONITOR);
  }

  public void test_build_pub1() throws Exception {
    MockProject project = newPubProject1();
    Target target = new Target(project);
    expectCall("createRootContext");
    expectCall("sourceAvailable", new MockSource(project, "some.dart"));
    expectCall("sourceAvailable", new MockSource(project, "web/" + BUILD_DART_FILE_NAME));
    expectCall("sourceAvailable", new MockSource(project, "web/other.dart"));

    target.build(new BuildEvent(project, null, MONITOR), MONITOR);

    assertEquals(1, target.getAllContexts().size());
    MockContext context = target.getContext(project);
    assertEquals(3, context.sources.size());
  }

  public void test_build_pub2() throws CoreException {
    MockProject project = newPubProject2();
    Target target = new Target(project);
    expectCall("createRootContext");
    expectCall("sourceAvailable", new MockSource(project, BUILD_DART_FILE_NAME));
    expectCall("sourceAvailable", new MockSource(project, "some.dart"));
    expectCall("sourceAvailable", new MockSource(project, "some1.dart"));
    expectCall("extractAnalysisContext", project.getMockFolder("myapp").toFile());
    expectCall("sourceAvailable", new MockSource(project, "myapp/" + BUILD_DART_FILE_NAME));
    expectCall("sourceAvailable", new MockSource(project, "myapp/other.dart"));

    target.build(new BuildEvent(project, null, MONITOR), MONITOR);

    assertEquals(2, target.getAllContexts().size());
    MockContext context = target.getContext(project);
    assertEquals(3, context.sources.size());
    context = target.getContext(project.getMockFolder("myapp"));
    assertEquals(2, context.sources.size());
  }

  public void test_build_simple() throws Exception {
    MockProject project = newSimpleProject();
    Target target = new Target(project);
    expectCall("createRootContext");
    expectCall("sourceAvailable", new MockSource(project, BUILD_DART_FILE_NAME));
    expectCall("sourceAvailable", new MockSource(project, "some.dart"));
    expectCall("sourceAvailable", new MockSource(project, "some1.dart"));
    expectCall("sourceAvailable", new MockSource(project, "web/" + BUILD_DART_FILE_NAME));
    expectCall("sourceAvailable", new MockSource(project, "web/other.dart"));

    target.build(new BuildEvent(project, null, MONITOR), MONITOR);

    assertEquals(1, target.getAllContexts().size());
    MockContext context = target.getContext(project);
    assertEquals(5, context.sources.size());

    expectCall("discard");

    target.clean(new CleanEvent(project, MONITOR), MONITOR);

    assertEquals(0, target.getAllContexts().size());
  }

  @Override
  protected void tearDown() throws Exception {
    if (expectedCalls.size() > 0) {
      fail("Expected:\n  " + expectedCalls.get(0));
    }
  }

  /**
   * Assert that a method call was expected
   * 
   * @param mthName the name of the method (not {@code null})
   * @param args zero or more arguments
   */
  private void assertCall(Call call) {
    if (expectedCalls.size() == 0) {
      fail("Did not expect:\n  " + call);
    }
    Call expected = expectedCalls.remove(0);
    if (!expected.equals(call)) {
      fail("Expected:\n  " + expected + "\nActual:\n  " + call);
    }
  }

  /**
   * Assert that a method call was expected
   * 
   * @param mthName the name of the method (not {@code null})
   * @param args zero or more arguments
   */
  private void assertCall(String mthName, Object... args) {
    assertCall(new Call(mthName, args));
  }

  /**
   * Add the specified call to the expected call list
   * 
   * @param call the expected call (not {@code null})
   */
  private void expectCall(Call call) {
    expectedCalls.add(call);
  }

  /**
   * Add the specified information to the expected call list
   * 
   * @param mthName the name of the method (not {@code null})
   * @param args zero or more arguments
   */
  private void expectCall(String mthName, Object... args) {
    expectCall(new Call(mthName, args));
  }
}
