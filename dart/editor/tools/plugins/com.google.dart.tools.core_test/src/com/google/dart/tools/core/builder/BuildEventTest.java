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
package com.google.dart.tools.core.builder;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.pub.PubBuildParticipantTest;

import junit.framework.TestCase;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import static org.eclipse.core.resources.IResourceDelta.CHANGED;

import java.util.ArrayList;

public class BuildEventTest extends TestCase {

  /**
   * Collects the resources that were visited
   */
  private final class Visitor implements BuildVisitor {
    ArrayList<IResource> actual = new ArrayList<IResource>();

    public void assertNotVisited(IResource resource) {
      if (!actual.contains(resource)) {
        return;
      }
      fail(resource + " should not have been visited as part of\n  " + actual);
    }

    public void assertVisited(IResource resource) {
      if (actual.contains(resource)) {
        return;
      }
      fail("Expected " + resource + " to be visited, but instead\n  " + actual);
    }

    @Override
    public boolean visit(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
      actual.add(delta.getResource());
      return true;
    }

    @Override
    public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {
      actual.add(proxy.requestResource());
      return true;
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

  public void test_traverse_delta_html1() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(WEB, CHANGED).add(HTML1, CHANGED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    Visitor visitor = new Visitor();
    event.traverse(visitor, false);
    visitor.assertVisited(HTML1);
    visitor.assertNotVisited(HTML2);
    visitor.assertNotVisited(HTML3);
  }

  public void test_traverse_delta_html2() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(PACKAGES, CHANGED).add(PACKAGE_FOO, CHANGED).add(HTML2, CHANGED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    Visitor visitor = new Visitor();
    event.traverse(visitor, false);
    visitor.assertNotVisited(HTML1);
    visitor.assertNotVisited(HTML2);
    visitor.assertNotVisited(HTML3);
  }

  public void test_traverse_delta_html2_withPackages() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(PACKAGES, CHANGED).add(PACKAGE_FOO, CHANGED).add(HTML2, CHANGED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    Visitor visitor = new Visitor();
    event.traverse(visitor, true);
    visitor.assertNotVisited(HTML1);
    visitor.assertVisited(HTML2);
    visitor.assertNotVisited(HTML3);
  }

  public void test_traverse_delta_html3() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(SVN, CHANGED).add(HTML3, CHANGED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    Visitor visitor = new Visitor();
    event.traverse(visitor, false);
    visitor.assertNotVisited(HTML1);
    visitor.assertNotVisited(HTML2);
    visitor.assertNotVisited(HTML3);
  }

  public void test_traverse_noDelta() throws Exception {
    BuildEvent event = new BuildEvent(PROJECT, null, MONITOR);
    Visitor visitor = new Visitor();
    event.traverse(visitor, false);
    visitor.assertVisited(HTML1);
    visitor.assertNotVisited(HTML2);
    visitor.assertNotVisited(HTML3);
  }

  public void test_traverse_noDelta_withPackages() throws Exception {
    BuildEvent event = new BuildEvent(PROJECT, null, MONITOR);
    Visitor visitor = new Visitor();
    event.traverse(visitor, true);
    visitor.assertVisited(HTML1);
    visitor.assertVisited(HTML2);
    visitor.assertNotVisited(HTML3);
  }

  public void testGetProject() {
    ParticipantEvent event = new BuildEvent(PROJECT, null, MONITOR);
    assertSame(PROJECT, event.getProject());
  }
}
