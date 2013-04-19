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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.internal.correction.LinkedPositionProposal;
import com.google.dart.engine.utilities.source.SourceRange;

import java.util.List;
import java.util.Map;

/**
 * Proposal for single source file change.
 * <p>
 * TODO(scheglov) why do we have several SourceChange-s in CorrectionProposal?
 */
public class CorrectionProposal {
  private final CorrectionKind kind;
  private final List<SourceChange> changes = Lists.newArrayList();
  private Map<String, List<SourceRange>> linkedPositions = Maps.newHashMap();
  private Map<String, List<LinkedPositionProposal>> linkedPositionProposals = Maps.newHashMap();

  public CorrectionProposal(CorrectionKind kind) {
    this.kind = kind;
  }

  /**
   * Adds {@link SourceChange} to perform.
   */
  public void addChange(SourceChange change) {
    changes.add(change);
  }

  /**
   * @return the {@link SourceChange}s to perform.
   */
  public List<SourceChange> getChanges() {
    return changes;
  }

  /**
   * @return the {@link CorrectionKind} which contains presentation of this
   *         {@link CorrectionProposal}.
   */
  public CorrectionKind getKind() {
    return kind;
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
   * Sets {@link Map} of position IDs to their proposals.
   */
  public void setLinkedPositionProposals(
      Map<String, List<LinkedPositionProposal>> linkedPositionProposals) {
    this.linkedPositionProposals = linkedPositionProposals;
  }

  /**
   * Sets the {@link Map} or position IDs to their locations.
   */
  public void setLinkedPositions(Map<String, List<SourceRange>> linkedPositions) {
    this.linkedPositions = linkedPositions;
  }
}
