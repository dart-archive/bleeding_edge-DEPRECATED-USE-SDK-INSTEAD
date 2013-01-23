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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.mock.MockContainer;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.test.AbstractDartCoreTest;

import static com.google.dart.tools.core.DartCore.BUILD_DART_FILE_NAME;
import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;
import static com.google.dart.tools.core.internal.builder.TestProjects.MONITOR;
import static com.google.dart.tools.core.internal.builder.TestProjects.newPubProject2;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import static org.eclipse.core.resources.IResourceDelta.ADDED;

import java.util.HashMap;
import java.util.List;

public class BuildDartParticipantTest extends AbstractDartCoreTest {

  /**
   * A specialized {@link BuildDartParticipant} that records which build files were supposed to be
   * executed rather than executing those build files.
   */
  private final class Target extends BuildDartParticipant {

    private HashMap<IFile, List<String>> calls = new HashMap<IFile, List<String>>();

    public void assertCall(IFile builderFile, String... expected) {
      List<String> actual = calls.get(builderFile);
      if (actual == null) {
        fail("Expected call to " + builderFile);
      }
      if (expected.length == actual.size()) {
        boolean success = true;
        for (int i = 0; i < expected.length; i++) {
          if (!expected[i].equals(actual.get(i))) {
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
      for (String arg : expected) {
        writer.println("  " + arg);
      }
      writer.println("actual:");
      for (String arg : actual) {
        writer.println("  " + arg);
      }
      fail(writer.toString().trim());
    }

    public void assertCalls(IResource... expected) {
      if (expected.length == calls.size()) {
        boolean success = true;
        for (IResource resource : expected) {
          if (calls.get(resource) == null) {
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
      for (IResource resource : expected) {
        writer.println("  " + resource);
      }
      writer.println("actual:");
      for (IResource resource : calls.keySet()) {
        writer.println("  " + resource);
      }
      fail(writer.toString().trim());
    }

    @Override
    protected void runBuildDart(IFile builderFile, List<String> buildArgs, IProgressMonitor monitor)
        throws CoreException {
      assertNotNull(builderFile);
      assertNotNull(buildArgs);
      for (String arg : buildArgs) {
        assertNotNull(arg);
      }
      assertNotNull(monitor);
      if (calls.put(builderFile, buildArgs) != null) {
        fail("Should not be called more than once: " + builderFile);
      }
    }
  }

  public void test_build_clean() throws Exception {
    Target target = new Target();
    MockContainer project = newPubProject2();
    MockFile builder0 = project.getMockFile(BUILD_DART_FILE_NAME);
    MockFile builder1 = project.getMockFile("myapp/" + BUILD_DART_FILE_NAME);

    target.clean(new CleanEvent(project, MONITOR), MONITOR);
    target.assertCalls(builder0, builder1);
    target.assertCall(builder0, "--clean");
    target.assertCall(builder1, "--clean");
  }

  public void test_build_dart0_added() throws Exception {
    Target target = new Target();
    MockContainer project = newPubProject2();
    MockFile builder0 = project.getMockFile(BUILD_DART_FILE_NAME);
    MockDelta delta = new MockDelta(project);
    MockFile dart = project.getMockFile("some.dart");

    delta.add(dart, ADDED);
    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls(builder0);
    target.assertCall(builder0, "--changed=" + dart.getName());
  }

  public void test_build_dart0_changed() throws Exception {
    Target target = new Target();
    MockContainer project = newPubProject2();
    MockFile builder0 = project.getMockFile(BUILD_DART_FILE_NAME);
    MockDelta delta = new MockDelta(project);
    MockFile dart = project.getMockFile("some.dart");

    delta.add(dart);
    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls(builder0);
    target.assertCall(builder0, "--changed=" + dart.getName());
  }

  public void test_build_dart01_changed() throws Exception {
    Target target = new Target();
    MockContainer project = newPubProject2();
    MockFile builder0 = project.getMockFile(BUILD_DART_FILE_NAME);
    //MockFile builder1 = project.getMockFile("myapp/" + BUILD_DART_FILE_NAME);
    MockDelta delta = new MockDelta(project);
    MockFile dart0 = project.getMockFile("some.dart");
    MockFile dart1 = project.getMockFile("some1.dart");
    MockFile dart2 = project.getMockFile("myapp/other.dart");

    delta.add(dart0);
    delta.add(dart1);
    delta.add("myapp").add("other.dart");
    delta.add("packages").add("pkg1").add("some_folder").add("bar.dart");
    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);

    // Ensure that build includes nested resources but excludes file nested under "packages"
    target.assertCalls(builder0);
    target.assertCall(
        builder0,
        "--changed=" + dart0.getName(),
        "--changed=" + dart1.getName(),
        "--changed=" + dart2.getFullPath().removeFirstSegments(1).toOSString());
    //target.assertCall(builder1, "--changed=" + dart2.getName());
  }

  public void test_build_dart1_added() throws Exception {
    Target target = new Target();
    MockContainer project = newPubProject2();
    MockFile builder0 = project.getMockFile(BUILD_DART_FILE_NAME);
    //MockFile builder1 = project.getMockFile("myapp/" + BUILD_DART_FILE_NAME);
    MockFile dart = project.getMockFile("myapp/other.dart");
    MockDelta delta = new MockDelta(project);

    delta.add("myapp").add("other.dart", ADDED);
    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls(builder0);
    target.assertCall(builder0, "--changed="
        + dart.getFullPath().removeFirstSegments(1).toOSString());
    //target.assertCall(builder1, "--changed=" + dart.getName());
  }

  public void test_build_dart1_changed() throws Exception {
    Target target = new Target();
    MockContainer project = newPubProject2();
    MockFile builder0 = project.getMockFile(BUILD_DART_FILE_NAME);
    //MockFile builder1 = project.getMockFile("myapp/" + BUILD_DART_FILE_NAME);
    MockFile dart = project.getMockFile("myapp/other.dart");
    MockDelta delta = new MockDelta(project);

    delta.add("myapp").add(dart);
    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls(builder0);
    target.assertCall(builder0, "--changed="
        + dart.getFullPath().removeFirstSegments(1).toOSString());
    //target.assertCall(builder1, "--changed=" + dart.getName());
  }

  public void test_build_dart2_changed() throws Exception {
    Target target = new Target();
    MockContainer project = newPubProject2();
    MockDelta delta = new MockDelta(project);

    delta.add(".svn").add("foo.dart");
    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls();
  }

  public void test_build_dart3_changed() throws Exception {
    Target target = new Target();
    MockContainer project = newPubProject2();
    MockDelta delta = new MockDelta(project);

    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1").add("bar.dart");
    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);
    target.assertCalls();
  }

  public void test_build_dart4_changed() throws Exception {
    Target target = new Target();
    MockContainer project = newPubProject2();
    project.remove("myapp/" + BUILD_DART_FILE_NAME);
    MockFile builder0 = project.getMockFile(BUILD_DART_FILE_NAME);
    MockFile dart = project.getMockFile("myapp/other.dart");

    MockDelta delta = new MockDelta(project);
    delta.add("myapp").add("other.dart");
    delta.add("packages").add("pkg1").add("some_folder").add("bar.dart");
    target.build(new BuildEvent(project, delta, MONITOR), MONITOR);

    // Ensure changed includes proper path to nested file but not files nested under "packages"
    target.assertCalls(builder0);
    target.assertCall(builder0, "--changed="
        + dart.getFullPath().removeFirstSegments(1).toOSString());
  }

  public void test_build_full1() throws Exception {
    Target target = new Target();
    MockContainer project = newPubProject2();
    MockFile builder0 = project.getMockFile(BUILD_DART_FILE_NAME);

    target.build(new BuildEvent(project, null, MONITOR), MONITOR);

    target.assertCalls(builder0);
    target.assertCall(builder0, "--full");
  }

  public void test_build_full2() throws Exception {
    Target target = new Target();
    MockContainer project = newPubProject2();
    project.remove(PUBSPEC_FILE_NAME);
    MockFile builder0 = project.getMockFile(BUILD_DART_FILE_NAME);

    target.build(new BuildEvent(project, null, MONITOR), MONITOR);

    // Should invoke builder0 even though it does not have a pubspec siblng
    // but because it is in the project root... legacy
    // And invoke builder1 because it is in an application directory (directory containing pubspec)
    target.assertCalls(builder0);
    target.assertCall(builder0, "--full");
  }
}
