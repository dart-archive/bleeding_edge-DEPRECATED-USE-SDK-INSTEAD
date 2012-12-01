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
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.pub.PubBuildParticipantTest;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import static org.eclipse.core.resources.IResourceDelta.ADDED;
import static org.eclipse.core.resources.IResourceDelta.CHANGED;

import java.util.HashMap;
import java.util.List;

public class BuildDartParticipantTest extends TestCase {

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

  private static final MockProject PROJECT = new MockProject(
      PubBuildParticipantTest.class.getSimpleName());
  private static final MockFile BUILDER0 = PROJECT.addFile(DartCore.BUILD_DART_FILE_NAME);
  private static final MockFile DART0 = PROJECT.addFile("some.dart");
  private static final MockFile DART01 = PROJECT.addFile("some1.dart");
  private static final MockFolder MYAPP = PROJECT.addFolder("myapp");
  private static final MockFile BUILDER1 = MYAPP.addFile(DartCore.BUILD_DART_FILE_NAME);
  private static final MockFile DART1 = MYAPP.addFile("other.dart");
  private static final MockFolder SVN = PROJECT.addFolder(".svn");
  private static final MockFile DART2 = SVN.addFile("foo.dart");
  private static final MockFolder PACKAGES = PROJECT.addFolder(DartCore.PACKAGES_DIRECTORY_NAME);
  private static final MockFolder SOME_PACKAGE = PACKAGES.addFolder("pkg1");
  private static final MockFile DART3 = SOME_PACKAGE.addFile("bar.dart");
  private static final MockFolder SOME_FOLDER = PROJECT.addFolder("some_folder");
  private static final MockFile DART4 = SOME_FOLDER.addFile("bar.dart");
  static {
    MYAPP.addFile(DartCore.PUBSPEC_FILE_NAME);
    SOME_FOLDER.addFile(DartCore.BUILD_DART_FILE_NAME);
    SVN.addFile(DartCore.PUBSPEC_FILE_NAME);
    SVN.addFile(DartCore.BUILD_DART_FILE_NAME);
    SOME_PACKAGE.addFile(DartCore.PUBSPEC_FILE_NAME);
    SOME_PACKAGE.addFile(DartCore.BUILD_DART_FILE_NAME);
  }

  private static final NullProgressMonitor MONITOR = new NullProgressMonitor();

  public void test_build_clean() throws Exception {
    Target target = new Target();
    target.clean(new CleanEvent(PROJECT, MONITOR), MONITOR);
    target.assertCalls(BUILDER0, BUILDER1);
    target.assertCall(BUILDER0, "--clean");
    target.assertCall(BUILDER1, "--clean");
  }

  public void test_build_dart0_added() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(DART0, ADDED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls(BUILDER0);
    target.assertCall(BUILDER0, "--changed=" + DART0.getName());
  }

  public void test_build_dart0_changed() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(DART0, CHANGED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls(BUILDER0);
    target.assertCall(BUILDER0, "--changed=" + DART0.getName());
  }

  public void test_build_dart01_changed() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(DART0, CHANGED);
    delta.add(DART01, CHANGED);
    delta.add(SOME_FOLDER, CHANGED).add(DART4, CHANGED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls(BUILDER0);
    target.assertCall(
        BUILDER0,
        "--changed=" + DART0.getName(),
        "--changed=" + DART01.getName(),
        "--changed=" + DART4.getFullPath().removeFirstSegments(1).toOSString());
  }

  public void test_build_dart1_added() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(MYAPP, CHANGED).add(DART1, ADDED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls(BUILDER0, BUILDER1);
    target.assertCall(BUILDER0, "--changed="
        + DART1.getFullPath().removeFirstSegments(1).toOSString());
    target.assertCall(BUILDER1, "--changed=" + DART1.getName());
  }

  public void test_build_dart1_changed() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(MYAPP, CHANGED).add(DART1, CHANGED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls(BUILDER0, BUILDER1);
    target.assertCall(BUILDER0, "--changed="
        + DART1.getFullPath().removeFirstSegments(1).toOSString());
    target.assertCall(BUILDER1, "--changed=" + DART1.getName());
  }

  public void test_build_dart2_changed() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(SVN, CHANGED).add(DART2, CHANGED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls();
  }

  public void test_build_dart3_changed() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(PACKAGES, CHANGED).add(SOME_PACKAGE, CHANGED).add(DART3, CHANGED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls();
  }

  public void test_build_dart4_changed() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(SOME_FOLDER, CHANGED).add(DART4, CHANGED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls(BUILDER0);
    target.assertCall(BUILDER0, "--changed="
        + DART4.getFullPath().removeFirstSegments(1).toOSString());
  }

  public void test_build_full() throws Exception {
    Target target = new Target();
    target.build(new BuildEvent(PROJECT, null, MONITOR), MONITOR);

    // Should invoke BUILDER0 even though it does not have a pubspec siblng
    // but because it is in the project root... legacy
    // And invoke BUILDER1 because it is in an application directory (directory containing pubspec)
    target.assertCalls(BUILDER0, BUILDER1);
    target.assertCall(BUILDER0);
    target.assertCall(BUILDER1);
  }
}
