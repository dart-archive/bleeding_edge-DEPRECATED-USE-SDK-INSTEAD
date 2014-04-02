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
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;

import java.util.List;
import java.util.Map;

/**
 * {@link CorrectionProposal} with single {@link Source} change.
 */
public class SourceCorrectionProposal extends CorrectionProposal {
  private final SourceChange change;
  private Map<String, List<SourceRange>> linkedPositions = Maps.newHashMap();
  private Map<String, List<LinkedPositionProposal>> linkedPositionProposals = Maps.newHashMap();
  private SourceRange endRange;

  public SourceCorrectionProposal(SourceChange change, CorrectionKind kind, Object... arguments) {
    super(kind, arguments);
    this.change = change;
  }

  /**
   * @return the {@link SourceChange} to perform.
   */
  public SourceChange getChange() {
    return change;
  }

  /**
   * @return the {@link SourceRange} to select after applying this change, maybe {@code null}.
   */
  public SourceRange getEndRange() {
    return endRange;
  }

  /**
   * @return the {@link Map} or position IDs to their proposals.
   */
  public Map<String, List<LinkedPositionProposal>> getLinkedPositionProposals() {
    return linkedPositionProposals;
  }

  /**
   * @return the {@link Map} of position IDs to their locations.
   */
  public Map<String, List<SourceRange>> getLinkedPositions() {
    return linkedPositions;
  }

  /**
   * Sets {@link SourceRange} to select after applying this change, maybe {@code null}.
   */
  public void setEndRange(SourceRange endRange) {
    this.endRange = endRange;
  }

  /**
   * Sets {@link Map} of position IDs to their proposals.
   */
  public void setLinkedPositionProposals(
      Map<String, List<LinkedPositionProposal>> linkedPositionProposals) {
    this.linkedPositionProposals = Maps.newHashMap(linkedPositionProposals);
  }

  /**
   * Sets the {@link Map} or position IDs to their locations.
   */
  public void setLinkedPositions(Map<String, List<SourceRange>> linkedPositions) {
    this.linkedPositions = Maps.newHashMap(linkedPositions);
  }
}
