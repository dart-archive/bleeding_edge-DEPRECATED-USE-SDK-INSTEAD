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
package com.google.dart.tools.ui.internal.text.correction.proposals;

import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.dom.rewrite.TrackedNodePosition;

import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.swt.graphics.Image;

/**
 * A proposal for quick fixes and quick assists that applies change and enters the linked mode when
 * the proposal is set up.
 */
public class LinkedCorrectionProposal_OLD extends CUCorrectionProposal_OLD {
  public LinkedCorrectionProposal_OLD(String name, Source source, TextChange change, int relevance,
      Image image) {
    super(name, source, change, relevance, image);
  }

  /**
   * Adds a linked position to be shown when the proposal is applied. All position with the same
   * group id are linked.
   * 
   * @param position The position to add.
   * @param isFirst If set, the proposal is jumped to first.
   * @param groupID The id of the group the proposal belongs to. All proposals in the same group are
   *          linked.
   */
  public void addLinkedPosition(TrackedNodePosition position, boolean isFirst, String groupID) {
    getLinkedProposalModel().getPositionGroup(groupID, true).addPosition(position, isFirst);
  }

  /**
   * Adds a linked position proposal to the group with the given id.
   * 
   * @param groupID The id of the group that should present the proposal
   * @param proposal The string to propose.
   * @param image The image to show for the position proposal or <code>null</code> if no image is
   *          desired.
   */
  public void addLinkedPositionProposal(String groupID, String proposal, Image image) {
    getLinkedProposalModel().getPositionGroup(groupID, true).addProposal(proposal, image, 10);
  }

  /**
   * Sets the end position of the linked mode to the end of the passed range.
   * 
   * @param position The position that describes the end position of the linked mode.
   */
  public void setEndPosition(TrackedNodePosition position) {
    getLinkedProposalModel().setEndPosition(position);
  }
}
