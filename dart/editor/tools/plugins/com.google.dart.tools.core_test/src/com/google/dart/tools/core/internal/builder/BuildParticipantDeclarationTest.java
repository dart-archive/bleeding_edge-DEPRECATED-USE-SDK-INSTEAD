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
import com.google.dart.tools.core.builder.DartBuildParticipant;

import org.eclipse.core.resources.IProject;

public class BuildParticipantDeclarationTest extends AbstractDartCoreTest {

  public void test_participantsFor() {
    IProject project = new MockProject();
    DartBuildParticipant[] participants = BuildParticipantDeclaration.participantsFor(project);

    assertNotNull(participants);
    for (Object participant : participants) {
      assertNotNull(participant);
    }

    // At a minimum, the "pub" build participant should be defined
    //assertTrue(participants.length > 0);

    // TODO (danrubel): assert participants are prioritized
    // pub before build.dart before analysis
  }
}
