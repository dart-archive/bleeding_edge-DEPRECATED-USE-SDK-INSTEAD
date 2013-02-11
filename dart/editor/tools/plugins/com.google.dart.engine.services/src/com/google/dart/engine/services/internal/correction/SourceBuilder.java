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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.engine.services.correction.CorrectionImage;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;

import java.util.List;
import java.util.Map;

/**
 * Helper for building Dart source with tracked positions.
 */
public class SourceBuilder {
  private final int offset;
  private final StringBuilder buffer = new StringBuilder();
  private final Map<String, List<SourceRange>> linkedPositions = Maps.newHashMap();
  private final Map<String, List<LinkedPositionProposal>> linkedProposals = Maps.newHashMap();
  private String currentPositionGroupId;
  private int currentPositionStart;
  private int endPosition = -1;

  public SourceBuilder(int offset) {
    this.offset = offset;
  }

  public SourceBuilder(SourceRange offsetRange) {
    this(offsetRange.getOffset());
  }

  /**
   * Adds proposal for the current position, may be called after {@link #startPosition(String)}.
   */
  public void addProposal(CorrectionImage icon, String text) {
    List<LinkedPositionProposal> proposals = linkedProposals.get(currentPositionGroupId);
    if (proposals == null) {
      proposals = Lists.newArrayList();
      linkedProposals.put(currentPositionGroupId, proposals);
    }
    proposals.add(new LinkedPositionProposal(icon, text));
  }

  /**
   * Appends source to the buffer.
   */
  public SourceBuilder append(CharSequence s) {
    buffer.append(s);
    return this;
  }

  /**
   * Ends position started using {@link #startPosition(String)}.
   */
  public void endPosition() {
    assert currentPositionGroupId != null;
    addPosition();
    currentPositionGroupId = null;
  }

  /**
   * @return the "end position" for the {@link CorrectionProposal}, may be <code>-1</code> if not
   *         set in this {@link SourceBuilder}.
   */
  public int getEndPosition() {
    if (endPosition == -1) {
      return -1;
    }
    return offset + endPosition;
  }

  /**
   * @return the {@link Map} or position IDs to their locations.
   */
  public Map<String, List<SourceRange>> getLinkedPositions() {
    return linkedPositions;
  }

  /**
   * @return the {@link Map} of position IDs to their proposals.
   */
  public Map<String, List<LinkedPositionProposal>> getLinkedProposals() {
    return linkedProposals;
  }

  /**
   * @return the offset at which this {@link SourceBuilder} should be applied in the original
   *         document.
   */
  public int getOffset() {
    return offset;
  }

  /**
   * Marks current position as "end position" of the {@link CorrectionProposal}.
   */
  public void setEndPosition() {
    endPosition = buffer.length();
  }

  /**
   * Sets text-only proposals for the current position.
   */
  public void setProposals(String[] proposals) {
    List<LinkedPositionProposal> proposalList = Lists.newArrayList();
    for (String proposalText : proposals) {
      proposalList.add(new LinkedPositionProposal(null, proposalText));
    }
    linkedProposals.put(currentPositionGroupId, proposalList);
  }

  /**
   * Starts linked position with given ID.
   */
  public void startPosition(String groupId) {
    assert currentPositionGroupId == null;
    currentPositionGroupId = groupId;
    currentPositionStart = buffer.length();
  }

  @Override
  public String toString() {
    return buffer.toString();
  }

  /**
   * Adds position location {@link SourceRange} using current fields.
   */
  private void addPosition() {
    List<SourceRange> locations = linkedPositions.get(currentPositionGroupId);
    if (locations == null) {
      locations = Lists.newArrayList();
      linkedPositions.put(currentPositionGroupId, locations);
    }
    int start = offset + currentPositionStart;
    int end = offset + buffer.length();
    locations.add(rangeStartEnd(start, end));
  }
}
