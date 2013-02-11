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
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartLength;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.Map;

/**
 * Test for {@link SourceBuilder}.
 */
public class SourceBuilderTest extends AbstractDartTest {
  public void test_append() throws Exception {
    SourceBuilder builder = new SourceBuilder(42);
    builder.append("var ");
    builder.append("a");
    builder.append(" = ");
    builder.append("99");
    builder.append(";");
    assertEquals("var a = 99;", builder.toString());
  }

  public void test_endPosition() throws Exception {
    SourceBuilder builder = new SourceBuilder(42);
    // no end position
    assertEquals(-1, builder.getEndPosition());
    // set end position
    builder.append("var v");
    builder.setEndPosition();
    builder.append(" = 99;");
    // validate
    assertEquals(42 + "var v".length(), builder.getEndPosition());
  }

  public void test_new_offset() throws Exception {
    SourceBuilder builder = new SourceBuilder(42);
    assertEquals(42, builder.getOffset());
  }

  public void test_new_SourceRange() throws Exception {
    SourceBuilder builder = new SourceBuilder(rangeStartLength(42, 10));
    assertEquals(42, builder.getOffset());
  }

  public void test_position() throws Exception {
    SourceBuilder builder = new SourceBuilder(42);
    builder.append("var ");
    String KEY = "VAR_NAME";
    {
      builder.startPosition(KEY);
      builder.append("o");
      builder.addProposal(CorrectionImage.IMG_CORRECTION_CHANGE, "willBeReplaced");
      builder.setProposals(new String[] {"builder", "sourceBuilder"});
      builder.addProposal(CorrectionImage.IMG_CORRECTION_CHANGE, "sb");
      builder.endPosition();
    }
    builder.append(" = ");
    {
      builder.startPosition(KEY);
      builder.append("o");
      builder.endPosition();
    }
    // source
    assertEquals("var o = o", builder.toString());
    // position(s)
    {
      Map<String, List<SourceRange>> linkedPositions = builder.getLinkedPositions();
      assertThat(linkedPositions).hasSize(1);
      assertTrue(linkedPositions.containsKey(KEY));
      List<SourceRange> locations = linkedPositions.get(KEY);
      assertThat(locations).hasSize(2);
      assertEquals(rangeStartLength(42 + "var ".length(), 1), locations.get(0));
      assertEquals(rangeStartLength(42 + "var o = ".length(), 1), locations.get(1));
    }
    // proposals
    {
      Map<String, List<LinkedPositionProposal>> linkedProposals = builder.getLinkedProposals();
      assertThat(linkedProposals).hasSize(1);
      assertTrue(linkedProposals.containsKey(KEY));
      List<LinkedPositionProposal> proposals = linkedProposals.get(KEY);
      assertThat(proposals).hasSize(3);
      // 0
      assertSame(null, proposals.get(0).getIcon());
      assertEquals("builder", proposals.get(0).getText());
      // 1
      assertSame(null, proposals.get(1).getIcon());
      assertEquals("sourceBuilder", proposals.get(1).getText());
      // 2
      assertSame(CorrectionImage.IMG_CORRECTION_CHANGE, proposals.get(2).getIcon());
      assertEquals("sb", proposals.get(2).getText());
    }
  }
}
