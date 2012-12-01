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

import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.html.HtmlBuildParticipant;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.pub.PubBuildParticipant;

import org.eclipse.core.resources.IProject;

public class BuildParticipantDeclarationTest extends AbstractDartCoreTest {

  public void test_participantsFor() {
    IProject project = new MockProject();
    BuildParticipant[] participants = BuildParticipantDeclaration.participantsFor(project);

    assertNotNull(participants);
    for (Object participant : participants) {
      assertNotNull(participant);
    }

    // At a minimum, the "pub" build participant should be defined
    assertTrue(participants.length > 0);

    // Assert contains known participants
    int pubIndex = indexOf(participants, PubBuildParticipant.class);
    int buildDartindex = indexOf(participants, BuildDartParticipant.class);
    int htmlIndex = indexOf(participants, HtmlBuildParticipant.class);

    assertTrue(pubIndex >= 0);
    assertTrue(buildDartindex >= 0);
    assertTrue(htmlIndex >= 0);

    assertTrue(pubIndex < buildDartindex);
    assertTrue(buildDartindex < htmlIndex);
  }

  private int indexOf(BuildParticipant[] participants, Class<?> bpClass) {
    for (int index = 0; index < participants.length; index++) {
      if (participants[index].getClass() == bpClass) {
        return index;
      }
    }
    return -1;
  }
}
