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
package com.google.dart.tools.core.html;

import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.pub.PubBuildParticipantTest;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;

import static org.eclipse.core.resources.IResourceDelta.ADDED;
import static org.eclipse.core.resources.IResourceDelta.CHANGED;
import static org.eclipse.core.resources.IResourceDelta.REMOVED;

import java.util.ArrayList;

public class HtmlBuildParticipantTest extends TestCase {

  /**
   * A specialized {@link HtmlBuildParticipant} that records which files are processed rather than
   * actually processing the files.
   */
  private final class Target extends HtmlBuildParticipant {

    private ArrayList<IResource> actual = new ArrayList<IResource>();

    public void assertCalls(IResource... expected) {
      if (expected.length == actual.size()) {
        boolean success = true;
        for (IResource resource : expected) {
          if (!actual.contains(resource)) {
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
      for (IResource resource : actual) {
        writer.println("  " + resource);
      }
      fail(writer.toString().trim());
    }

    @Override
    protected void processHtml(IFile file) {
      assertNotNull(file);
      actual.add(file);
    }
  }

  private static final MockProject PROJECT = new MockProject(
      PubBuildParticipantTest.class.getSimpleName());
  private static final MockFolder WEB = PROJECT.addFolder("web");
  private static final MockFile HTML1 = WEB.addFile("index.html");
  private static final MockFolder PACKAGES = PROJECT.addFolder(DartCore.PACKAGES_DIRECTORY_NAME);
  private static final MockFolder PACKAGE_FOO = PACKAGES.addFolder("foo");
  private static final MockFile HTML2 = PACKAGE_FOO.addFile("other.html");
  private static final MockFolder SVN = PROJECT.addFolder(".svn");
  private static final MockFile HTML3 = SVN.addFile("team.html");

  private static final NullProgressMonitor MONITOR = new NullProgressMonitor();

  public void test_build_full() throws Exception {
    Target target = new Target();
    target.build(new BuildEvent(PROJECT, null, MONITOR), MONITOR);
    target.assertCalls(HTML1);
  }

  public void test_build_html1_added() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(WEB, CHANGED).add(HTML1, ADDED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls(HTML1);
  }

  public void test_build_html1_changed() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(WEB, CHANGED).add(HTML1, CHANGED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls(HTML1);
  }

  public void test_build_html1_removed() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(WEB, CHANGED).add(HTML1, REMOVED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls();
  }

  public void test_build_html2_changed() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(PACKAGES, CHANGED).add(PACKAGE_FOO, CHANGED).add(HTML2, CHANGED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls();
  }

  public void test_build_html3_changed() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(SVN, CHANGED).add(HTML3, CHANGED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls();
  }

  public void test_build_packages_added() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(PACKAGES, ADDED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls();
  }

  public void test_build_svn_added() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(SVN, ADDED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls();
  }

  public void test_build_web_added() throws Exception {
    Target target = new Target();
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(WEB, ADDED);
    target.build(new BuildEvent(PROJECT, delta, MONITOR), MONITOR);
    target.assertCalls(HTML1);
  }
}
