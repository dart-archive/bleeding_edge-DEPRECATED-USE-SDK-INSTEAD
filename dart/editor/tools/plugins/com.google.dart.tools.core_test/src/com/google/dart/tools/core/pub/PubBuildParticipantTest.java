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
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

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

  private static final NullProgressMonitor MONITOR = new NullProgressMonitor();

  // Assert pub is not run
  public void test_build_full_emptyProject() throws Exception {
    Target target = new Target();
    MockProject project = new MockProject(PubBuildParticipantTest.class.getSimpleName());

    target.build(new BuildEvent(project, null, MONITOR), MONITOR);
    target.assertCalls();
  }

  // Assert pub is run on container containing pubspec.yaml
  public void test_build_full_pub() throws Exception {
    Target target = new Target();
    MockProject project = new MockProject(PubBuildParticipantTest.class.getSimpleName());
    project.addFile(DartCore.PUBSPEC_FILE_NAME);

    target.build(new BuildEvent(project, null, MONITOR), MONITOR);
    target.assertCalls(project);
  }

  // Assert pub is not run on pubspec.yaml in folder under "packages" directory hierarchy
  public void test_build_full_pubInPackages() throws Exception {
    Target target = new Target();
    MockProject project = new MockProject(PubBuildParticipantTest.class.getSimpleName());
    project.addFile(DartCore.PUBSPEC_FILE_NAME);
    project.addFolder(DartCore.PACKAGES_DIRECTORY_NAME).addFolder("foo").addFile(
        DartCore.PUBSPEC_FILE_NAME);

    target.build(new BuildEvent(project, null, MONITOR), MONITOR);
    target.assertCalls(project);
  }

  // Assert pub is run when pubspec.yaml is added
  public void test_build_incremental_pubAdded() throws Exception {
    Target target = new Target();
    MockProject project = new MockProject(PubBuildParticipantTest.class.getSimpleName());
    MockFile pubspec = project.addFile(DartCore.PUBSPEC_FILE_NAME);
    project.addFolder(DartCore.PACKAGES_DIRECTORY_NAME).addFolder("foo").addFile(
        DartCore.PUBSPEC_FILE_NAME);

    MockDelta delta = new MockDelta(project, IResourceDelta.CHANGED);
    delta.add(pubspec, IResourceDelta.ADDED);

    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls(project);
  }

  // Assert pub is run when pubspec.yaml has changed
  public void test_build_incremental_pubChanged() throws Exception {
    Target target = new Target();
    MockProject project = new MockProject(PubBuildParticipantTest.class.getSimpleName());
    MockFile pubspec = project.addFile(DartCore.PUBSPEC_FILE_NAME);
    project.addFolder(DartCore.PACKAGES_DIRECTORY_NAME).addFolder("foo").addFile(
        DartCore.PUBSPEC_FILE_NAME);

    MockDelta delta = new MockDelta(project, IResourceDelta.CHANGED);
    delta.add(pubspec, IResourceDelta.CHANGED);

    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls(project);
  }

  // Assert pub is not run on pubspec.yaml in file under "packages" directory hierarchy
  public void test_build_incremental_pubInPackagesAdded() throws Exception {
    Target target = new Target();
    MockProject project = new MockProject(PubBuildParticipantTest.class.getSimpleName());
    project.addFile(DartCore.PUBSPEC_FILE_NAME);
    MockFolder packages = project.addFolder(DartCore.PACKAGES_DIRECTORY_NAME);
    MockFolder folder = packages.addFolder("foo");
    MockFile pubspec = folder.addFile(DartCore.PUBSPEC_FILE_NAME);

    MockDelta delta = new MockDelta(project, IResourceDelta.CHANGED);
    delta.add(packages, IResourceDelta.CHANGED).add(folder, IResourceDelta.CHANGED).add(
        pubspec,
        IResourceDelta.ADDED);

    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls();
  }

  // Assert pub is not run on pubspec.yaml in file under "packages" directory hierarchy
  public void test_build_incremental_pubInPackagesChanged() throws Exception {
    Target target = new Target();
    MockProject project = new MockProject(PubBuildParticipantTest.class.getSimpleName());
    project.addFile(DartCore.PUBSPEC_FILE_NAME);
    MockFolder packages = project.addFolder(DartCore.PACKAGES_DIRECTORY_NAME);
    MockFolder folder = packages.addFolder("foo");
    MockFile pubspec = folder.addFile(DartCore.PUBSPEC_FILE_NAME);

    MockDelta delta = new MockDelta(project, IResourceDelta.CHANGED);
    delta.add(packages, IResourceDelta.CHANGED).add(folder, IResourceDelta.CHANGED).add(
        pubspec,
        IResourceDelta.CHANGED);

    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls();
  }

  // Assert pub not is run when pubspec.yaml is removed
  public void test_build_incremental_pubRemoved() throws Exception {
    Target target = new Target();
    MockProject project = new MockProject(PubBuildParticipantTest.class.getSimpleName());
    MockFile pubspec = project.addFile(DartCore.PUBSPEC_FILE_NAME);
    project.addFolder(DartCore.PACKAGES_DIRECTORY_NAME).addFolder("foo").addFile(
        DartCore.PUBSPEC_FILE_NAME);

    MockDelta delta = new MockDelta(project, IResourceDelta.CHANGED);
    delta.add(pubspec, IResourceDelta.REMOVED);

    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls();
  }
}
