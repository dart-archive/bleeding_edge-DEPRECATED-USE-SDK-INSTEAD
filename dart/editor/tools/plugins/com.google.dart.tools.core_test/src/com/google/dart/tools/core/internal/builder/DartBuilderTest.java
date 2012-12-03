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

import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.builder.ParticipantEvent;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.pub.PubBuildParticipantTest;
import com.google.dart.tools.core.test.util.DartCoreTestLog;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import java.util.ArrayList;

public class DartBuilderTest extends AbstractDartCoreTest {

  /**
   * Specialized {@link BuildParticipant} that asserts specific methods are called
   */
  private class MockParticipant implements BuildParticipant {

    private final ArrayList<Object[]> expected = new ArrayList<Object[]>();
    private int index = 0;

    @Override
    public void build(BuildEvent event, IProgressMonitor monitor) throws CoreException {
      validateCall("build", event, monitor);
      // TODO (danrubel): validate event has appropriate delta
    }

    @Override
    public void clean(CleanEvent event, IProgressMonitor monitor) throws CoreException {
      validateCall("clean", event, monitor);
    }

    void assertComplete() {
      int delta = expected.size() - index;
      if (delta == 0) {
        return;
      }
      PrintStringWriter msg = new PrintStringWriter();
      msg.print("Expected ");
      msg.print(delta);
      msg.print(" additional call(s):");
      for (int i = index; i < expected.size(); i++) {
        msg.println();
        msg.print("  ");
        Object[] details = expected.get(i);
        for (Object each : details) {
          msg.print(each);
          msg.print(", ");
        }
      }
      fail(msg.toString());
    }

    void expect(String methodName, IProject project) {
      expected.add(new Object[] {methodName, project});
    }

    private Object[] validateCall(String mthName, ParticipantEvent event, IProgressMonitor monitor) {
      if (index >= expected.size()) {
        fail("Unexpected call to " + mthName);
      }
      Object[] details = expected.get(index++);
      assertEquals("Expected call to method", details[0], mthName);
      assertNotNull(event.getProject());
      assertSame(details[1], event.getProject());
      assertNotNull(monitor);
      return details;
    }
  }

  private static final MockProject PROJECT = new MockProject(
      PubBuildParticipantTest.class.getSimpleName());
  private MockParticipant participant;

  public void test_build_exception() throws Exception {
    participant = new MockParticipant() {
      @Override
      public void build(BuildEvent event, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("test exception");
      }
    };
    new DartBuilder(participant).build(PROJECT, IncrementalProjectBuilder.FULL_BUILD, null, null);
    DartCoreTestLog.getLog().assertEntries(IStatus.ERROR);
  }

  public void test_build_full() throws Exception {
    participant.expect("build", PROJECT);
    new DartBuilder(participant).build(PROJECT, IncrementalProjectBuilder.FULL_BUILD, null, null);
  }

  public void test_clean() throws Exception {
    participant.expect("clean", PROJECT);
    new DartBuilder(participant).clean(PROJECT, null);
  }

  public void test_clean_exception() throws Exception {
    participant = new MockParticipant() {
      @Override
      public void clean(CleanEvent event, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("test exception");
      }
    };
    new DartBuilder(participant).clean(PROJECT, null);
    DartCoreTestLog.getLog().assertEntries(IStatus.ERROR);
  }

  @Override
  protected void setUp() {
    participant = new MockParticipant();
  }

  @Override
  protected void tearDown() {
    participant.assertComplete();
  }
}
