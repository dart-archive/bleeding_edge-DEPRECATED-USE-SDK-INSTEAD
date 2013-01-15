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
package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.internal.analysis.model.ProjectImpl;
import com.google.dart.tools.core.internal.builder.MockContext;
import com.google.dart.tools.core.internal.builder.TestProjects;
import com.google.dart.tools.core.mock.MockContainer;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;

import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;

import java.io.File;

public class ProjectImplTest extends AbstractDartCoreTest {

  /**
   * Specialized {@link ProjectImpl} that returns a mock context for recording what analysis is
   * requested rather than a context that would actually analyze the source.
   */
  private final class Target extends ProjectImpl {

    Target(IProject resource) {
      super(resource);
    }

    @Override
    protected AnalysisContext createDefaultContext() {
      return new MockContext();
    }
  }

  private MockProject projectContainer;
  private MockFolder webContainer;
  private MockFolder subContainer;
  private MockFolder appContainer;
  private MockFolder appLibContainer;
  private MockFolder subAppContainer;
  private Target project;

  public void test_container1Deleted() {
    MockContext context1 = (MockContext) project.getContext(subContainer);
    MockContext context2 = (MockContext) project.getContext(appContainer);
    context1.assertDiscarded(false);
    context2.assertDiscarded(false);

    projectContainer.remove("web");
    project.discardContextsIn(webContainer);

    assertNotNull(project.getExistingContext(projectContainer));
    assertNotNull(project.getExistingContext(appContainer));
    assertNull(project.getExistingContext(webContainer));
    assertNull(project.getExistingContext(subContainer));
    context1.assertDiscarded(false);
    context2.assertDiscarded(false);

    projectContainer.remove("myapp");
    project.discardContextsIn(appContainer);

    assertNotNull(project.getExistingContext(projectContainer));
    assertNull(project.getExistingContext(appContainer));
    assertNull(project.getExistingContext(webContainer));
    assertNull(project.getExistingContext(subContainer));
    context1.assertDiscarded(false);
    context2.assertDiscarded(true);
  }

  public void test_container2Deleted() {
    MockContext context0 = (MockContext) project.getContext(projectContainer);
    MockContext context1 = (MockContext) project.getContext(subContainer);
    MockContext context2 = (MockContext) project.getContext(appContainer);
    context1.assertDiscarded(false);
    context2.assertDiscarded(false);

    project.discardContextsIn(projectContainer);

    assertNull(project.getExistingContext(projectContainer));
    assertNull(project.getExistingContext(appContainer));
    assertNull(project.getExistingContext(webContainer));
    assertNull(project.getExistingContext(subContainer));
    context0.assertDiscarded(true);
    context1.assertDiscarded(true);
    context2.assertDiscarded(true);
  }

  public void test_getContext() {
    MockContext context1 = (MockContext) project.getContext(projectContainer);
    assertNotNull(context1);
    assertSame(context1, project.getContext(webContainer));
    assertSame(context1, project.getContext(subContainer));
    context1.assertExtracted(null);

    MockContext context2 = (MockContext) project.getContext(appContainer);
    assertNotNull(context2);
    assertNotSame(context1, context2);
    context1.assertExtracted(appContainer);

    assertFactoryInitialized(projectContainer, context1);
    assertFactoryInitialized(appContainer, context2);
  }

  public void test_getResource() {
    assertSame(projectContainer, project.getResource());
  }

  public void test_pubspecAdded() {
    projectContainer.remove(PUBSPEC_FILE_NAME);
    appContainer.remove(PUBSPEC_FILE_NAME);
    MockContext context1 = (MockContext) project.getContext(projectContainer);
    MockContext context2 = (MockContext) project.getContext(appLibContainer);
    MockContext context3 = (MockContext) project.getContext(subAppContainer);
    assertSame(context1, context2);
    assertNotSame(context1, context3);
    context1.assertClearResolution(false);
    context1.assertExtracted(subAppContainer);
    context3.assertExtracted(null);

    projectContainer.addFile(PUBSPEC_FILE_NAME);
    project.pubspecAdded(projectContainer);
    context1.assertClearResolution(true);

    appContainer.addFile(PUBSPEC_FILE_NAME);
    project.pubspecAdded(appContainer);

    assertSame(context1, project.getExistingContext(projectContainer));
    assertSame(context3, project.getExistingContext(subAppContainer));
    context2 = (MockContext) project.getContext(appLibContainer);
    assertFactoryInitialized(appContainer, context2);
    assertSame(context2, project.getExistingContext(appContainer));
    assertNotSame(context2, context1);
    assertNotSame(context2, context3);
    context1.assertExtracted(appContainer);
    context2.assertExtracted(null);
    context3.assertExtracted(null);
  }

  public void test_pubspecRemoved() {
    MockContext context1 = (MockContext) project.getContext(projectContainer);
    MockContext context2 = (MockContext) project.getContext(appLibContainer);
    MockContext context3 = (MockContext) project.getContext(subAppContainer);
    assertNotSame(context1, context2);
    assertNotSame(context1, context3);
    assertNotSame(context2, context3);
    context1.assertClearResolution(false);
    context1.assertMergedContext(null);

    projectContainer.remove(PUBSPEC_FILE_NAME);
    project.pubspecRemoved(projectContainer);

    context1.assertClearResolution(true);
    context1.assertMergedContext(null);

    appContainer.remove(PUBSPEC_FILE_NAME);
    project.pubspecRemoved(appContainer);

    context1.assertMergedContext(context2);
    assertSame(context1, project.getContext(projectContainer));
    assertSame(context1, project.getContext(appContainer));
    assertSame(context1, project.getContext(appLibContainer));
    assertSame(context3, project.getContext(subAppContainer));
  }

  @Override
  protected void setUp() throws Exception {
    projectContainer = TestProjects.newPubProject3();
    webContainer = projectContainer.getMockFolder("web");
    subContainer = webContainer.getMockFolder("sub");
    appContainer = projectContainer.getMockFolder("myapp");
    appLibContainer = appContainer.getMockFolder("lib");
    subAppContainer = appContainer.getMockFolder("subApp");

    project = new Target(projectContainer);
  }

  private void assertFactoryInitialized(MockContainer container, AnalysisContext context) {
    SourceFactory factory = context.getSourceFactory();
    File file1 = container.getFile(new Path("doesNotExist1.dart")).getLocation().toFile();
    Source source1 = factory.forFile(file1);

    Source source2 = factory.resolveUri(source1, "doesNotExist2.dart");
    File file2 = new File(source2.getFullName());
    assertEquals(file1.getParent(), file2.getParent());

    Source source3 = factory.resolveUri(source1, "package:doesNotExist3/doesNotExist4.dart");
    File file3 = new File(source3.getFullName());
    assertEquals("doesNotExist4.dart", file3.getName());
    File parent3 = file3.getParentFile();
    assertEquals("doesNotExist3", parent3.getName());
    File packages = parent3.getParentFile();
    assertEquals("packages", packages.getName());
    assertEquals(file1.getParent(), packages.getParent());
  }
}
