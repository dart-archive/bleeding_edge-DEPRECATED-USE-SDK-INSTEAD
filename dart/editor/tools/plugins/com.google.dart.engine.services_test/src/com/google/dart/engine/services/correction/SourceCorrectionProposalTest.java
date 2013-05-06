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

package com.google.dart.engine.services.correction;

import com.google.common.collect.Maps;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.utilities.source.SourceRange;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

public class SourceCorrectionProposalTest extends TestCase {
  private final SourceChange change = mock(SourceChange.class);

  public void test_access() throws Exception {
    SourceCorrectionProposal proposal = new SourceCorrectionProposal(
        change,
        CorrectionKind.QA_ADD_TYPE_ANNOTATION);
    assertSame(CorrectionKind.QA_ADD_TYPE_ANNOTATION, proposal.getKind());
    assertSame(change, proposal.getChange());
  }

  public void test_linkedPositions() throws Exception {
    SourceCorrectionProposal proposal = new SourceCorrectionProposal(
        change,
        CorrectionKind.QA_ADD_TYPE_ANNOTATION);
    Map<String, List<SourceRange>> linkedPositions = Maps.newHashMap();
    Map<String, List<LinkedPositionProposal>> linkedPositions2 = Maps.newHashMap();
    proposal.setLinkedPositions(linkedPositions);
    proposal.setLinkedPositionProposals(linkedPositions2);
    assertEquals(linkedPositions, proposal.getLinkedPositions());
    assertEquals(linkedPositions2, proposal.getLinkedPositionProposals());
  }
}
