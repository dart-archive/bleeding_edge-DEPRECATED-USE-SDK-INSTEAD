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

package com.google.dart.engine.services.internal.correction;

import com.google.dart.engine.services.correction.CorrectionImage;

/**
 * Test for {@link LinkedPositionProposal}.
 */
public class LinkedPositionProposalTest extends AbstractDartTest {
  public void test_access() throws Exception {
    LinkedPositionProposal proposal = new LinkedPositionProposal(
        CorrectionImage.IMG_CORRECTION_CHANGE,
        "my text");
    assertSame(CorrectionImage.IMG_CORRECTION_CHANGE, proposal.getIcon());
    assertEquals("my text", proposal.getText());
  }
}
