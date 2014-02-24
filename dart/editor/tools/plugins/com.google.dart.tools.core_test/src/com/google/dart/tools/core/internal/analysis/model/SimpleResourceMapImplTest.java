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
package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.FileUtilities2;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.MockContext;
import com.google.dart.tools.core.internal.builder.TestProjects;
import com.google.dart.tools.core.mock.MockContainer;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Path;

import java.io.File;

public class SimpleResourceMapImplTest extends TestCase {

  protected MockWorkspaceRoot rootContainer;
  protected MockProject projectContainer;
  protected File projectDir;
  protected MockContext context;
  protected MockContainer pubContainer;

  public void test_getContext() {
    SimpleResourceMapImpl map = newTarget();
    assertSame(context, map.getContext());
    context.assertNoCalls();
  }

  public void test_getResource() {
    SimpleResourceMapImpl map = newTarget();
    assertSame(pubContainer, map.getResource());
    context.assertNoCalls();
  }

  public void test_getResource_fromSourceInWorkspace() {
    SimpleResourceMapImpl map = newTarget();

    MockContainer myappRes = pubContainer;
    File myappDir = new File(projectDir, myappRes.getName());
    MockFile res = myappRes.getMockFile("other.dart");
    File file = new File(myappDir, res.getName());
    Source source = new FileBasedSource(file);
    assertSame(res, map.getResource(source));
  }

  public void test_getSource_fromUserResource() {
    SimpleResourceMapImpl map = newTarget();

    File myappDir = new File(projectDir, pubContainer.getName());
    MockFile res = pubContainer.getMockFile("other.dart");
    File file = new File(myappDir, res.getName());
    Source source = new FileBasedSource(file);
    assertEquals(source, map.getSource(res));
  }

  protected SimpleResourceMapImpl newTarget() {
    return new SimpleResourceMapImpl(pubContainer, context);
  }

  @Override
  protected void setUp() throws Exception {
    rootContainer = new MockWorkspaceRoot();
    projectContainer = TestProjects.newPubProject3(rootContainer);
    projectContainer.remove(DartCore.PUBSPEC_FILE_NAME);
    projectDir = FileUtilities2.createTempDir(projectContainer.getName());
    projectContainer.setLocation(new Path(projectDir.getAbsolutePath()));
    context = new MockContext();
    context.setSourceFactory(new SourceFactory());
    pubContainer = projectContainer.getMockFolder("myapp");
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtilities2.deleteTempDir();
  }
}
