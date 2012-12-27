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
package com.google.dart.tools.core.pub;

import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.internal.builder.TestProjects;
import com.google.dart.tools.core.mock.MockContainer;
import com.google.dart.tools.core.mock.MockDelta;

import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;
import static com.google.dart.tools.core.internal.builder.TestProjects.MONITOR;
import static com.google.dart.tools.core.internal.builder.TestProjects.newEmptyProject;
import static com.google.dart.tools.core.internal.builder.TestProjects.newPubProject1;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IProgressMonitor;

import static org.eclipse.core.resources.IResourceDelta.ADDED;
import static org.eclipse.core.resources.IResourceDelta.REMOVED;

import java.util.ArrayList;

public class PubBuildParticipantTest extends TestCase {

  /**
   * A specialized {@link PubBuildParticipant} that records in which directories pub is run rather
   * than actually executing pub.
   */
  private static class Target extends PubBuildParticipant {

    private ArrayList<IContainer> actual = new ArrayList<IContainer>();

    public void assertCalls(IContainer... expected) {
      if (expected.length == actual.size()) {
        boolean success = true;
        for (IContainer container : expected) {
          if (!actual.contains(container)) {
            success = false;
            break;
          }
        }
        if (success) {
          return;
        }
      }
      PrintStringWriter writer = new PrintStringWriter();
      writer.println("expected:");
      for (IContainer container : expected) {
        writer.println("  " + container);
      }
      writer.println("actual:");
      for (IContainer container : actual) {
        writer.println("  " + container);
      }
      fail(writer.toString().trim());
    }

    @Override
    protected void runPub(IContainer container, IProgressMonitor monitor) {
      assertNotNull(container);
      assertNotNull(monitor);
      actual.add(container);
    }
  }

  // Assert pub is not run
  public void test_build_full_emptyProject() throws Exception {
    Target target = new Target();
    MockContainer project = newEmptyProject();

    target.build(new BuildEvent(project, null, MONITOR), MONITOR);
    target.assertCalls();
  }

  // Assert pub is run on project containing pubspec.yaml
  public void test_build_full_pub() throws Exception {
    Target target = new Target();
    MockContainer project = newEmptyProject();
    project.addFile(PUBSPEC_FILE_NAME);

    target.build(new BuildEvent(project, null, MONITOR), MONITOR);
    target.assertCalls(project);
  }

  // Assert pub is not run on pubspec.yaml in folder under "packages" directory hierarchy
  // or in hidden ".svn" directory
  public void test_build_full_pubInPackages() throws Exception {
    Target target = new Target();
    MockContainer project = newPubProject1();

    target.build(new BuildEvent(project, null, MONITOR), MONITOR);
    target.assertCalls(project);
  }

  // Assert pub is run when pubspec.yaml is added
  public void test_build_incremental_pubAdded() throws Exception {
    Target target = new Target();
    MockContainer project = TestProjects.newPubProject1();

    MockDelta delta = new MockDelta(project);
    delta.add(PUBSPEC_FILE_NAME, ADDED);

    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls(project);
  }

  // Assert pub is run when pubspec.yaml has changed
  public void test_build_incremental_pubChanged() throws Exception {
    Target target = new Target();
    MockContainer project = TestProjects.newPubProject1();

    MockDelta delta = new MockDelta(project);
    delta.add(PUBSPEC_FILE_NAME);

    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls(project);
  }

  // Assert pub is not run on pubspec.yaml in file under "packages" directory hierarchy
  // or in hidden ".svn" directory
  public void test_build_incremental_pubInPackagesAdded() throws Exception {
    Target target = new Target();
    MockContainer project = TestProjects.newPubProject1();

    MockDelta delta = new MockDelta(project);
    delta.add(PACKAGES_DIRECTORY_NAME).add("foo").add(PUBSPEC_FILE_NAME);
    delta.add(".svn").add(PUBSPEC_FILE_NAME);

    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls();
  }

  // Assert pub is not run on pubspec.yaml in file under "packages" directory hierarchy
  // or in hidden ".svn" directory
  public void test_build_incremental_pubInPackagesChanged() throws Exception {
    Target target = new Target();
    MockContainer project = TestProjects.newPubProject1();

    MockDelta delta = new MockDelta(project);
    delta.add(PACKAGES_DIRECTORY_NAME).add("foo").add(PUBSPEC_FILE_NAME);
    delta.add(".svn").add(PUBSPEC_FILE_NAME);

    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls();
  }

  // Assert pub not is run when pubspec.yaml is removed
  public void test_build_incremental_pubRemoved() throws Exception {
    Target target = new Target();
    MockContainer project = TestProjects.newPubProject1();

    MockDelta delta = new MockDelta(project);
    delta.add(PUBSPEC_FILE_NAME, REMOVED);

    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls();
  }
}
