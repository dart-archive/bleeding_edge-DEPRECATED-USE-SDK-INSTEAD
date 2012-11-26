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
package com.google.dart.tools.ui.internal.text.correction.proposals;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.tools.core.dom.rewrite.TrackedNodePosition;
import com.google.dart.tools.core.model.SourceRange;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.Image;

import java.util.List;
import java.util.Map;

/**
 * Helper for building Dart source with tracked positions.
 */
public class SourceBuilder {
  private final int offset;
  private final StringBuilder buffer = new StringBuilder();
  private final Map<String, List<TrackedNodePosition>> trackedPositions = Maps.newHashMap();
  private final Map<String, List<TrackedNodeProposal>> trackedProposals = Maps.newHashMap();
  private String currentPositionGroupId;
  private int currentPositionStart;
  private int endPosition = -1;

  public SourceBuilder(int offset) {
    this.offset = offset;
  }

  public SourceBuilder(SourceRange offsetRange) {
    this(offsetRange.getOffset());
  }

  public void addProposal(Image icon, String text) {
    List<TrackedNodeProposal> proposals = trackedProposals.get(currentPositionGroupId);
    if (proposals == null) {
      proposals = Lists.newArrayList();
      trackedProposals.put(currentPositionGroupId, proposals);
    }
    proposals.add(new TrackedNodeProposal(icon, text));
  }

  public SourceBuilder append(CharSequence s) {
    buffer.append(s);
    return this;
  }

  public void endPosition() {
    Assert.isNotNull(currentPositionGroupId);
    addPosition();
    currentPositionGroupId = null;
  }

  /**
   * @return the "end position" for the {@link LinkedCorrectionProposal}, may be <code>-1</code> if
   *         not set in this {@link SourceBuilder}.
   */
  public int getEndPosition() {
    if (endPosition == -1) {
      return -1;
    }
    return offset + endPosition;
  }

  /**
   * @return the offset at which this {@link SourceBuilder} should be applied in the original
   *         document.
   */
  public int getOffset() {
    return offset;
  }

  public Map<String, List<TrackedNodePosition>> getTrackedPositions() {
    return trackedPositions;
  }

  public Map<String, List<TrackedNodeProposal>> getTrackedProposals() {
    return trackedProposals;
  }

  /**
   * Marks current position as "end position" of the {@link LinkedCorrectionProposal}.
   */
  public void setEndPosition() {
    endPosition = buffer.length();
  }

  public void setProposals(String[] proposals) {
    List<TrackedNodeProposal> proposalList = Lists.newArrayList();
    for (String proposalText : proposals) {
      proposalList.add(new TrackedNodeProposal(null, proposalText));
    }
    trackedProposals.put(currentPositionGroupId, proposalList);
  }

  public void startPosition(String groupId) {
    Assert.isTrue(currentPositionGroupId == null);
    currentPositionGroupId = groupId;
    currentPositionStart = buffer.length();
  }

  @Override
  public String toString() {
    return buffer.toString();
  }

  /**
   * Adds {@link TrackedNodePosition} using current fields.
   */
  private void addPosition() {
    List<TrackedNodePosition> positions = trackedPositions.get(currentPositionGroupId);
    if (positions == null) {
      positions = Lists.newArrayList();
      trackedPositions.put(currentPositionGroupId, positions);
    }
    int start = offset + currentPositionStart;
    int end = offset + buffer.length();
    positions.add(TrackedPositions.forStartEnd(start, end));
  }
}
