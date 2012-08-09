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
package com.google.dart.tools.internal.corext.fix;

import com.google.dart.tools.core.dom.rewrite.TrackedNodePosition;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LinkedProposalPositionGroup {

  /**
   * {@link LinkedProposalPositionGroup.PositionInformation} describes a position inside a position
   * group. The information provided must be accurate after the document change to the proposal has
   * been performed, but doesn't need to reflect the changed done by the linking mode.
   */
  public static abstract class PositionInformation {
    public abstract int getLength();

    public abstract int getOffset();

    public abstract int getSequenceRank();
  }

  public static class Proposal {

    private final String fDisplayString;
    private Image fImage;
    private final int fRelevance;

    public Proposal(String displayString, Image image, int relevance) {
      fDisplayString = displayString;
      fImage = image;
      fRelevance = relevance;
    }

    public TextEdit computeEdits(int offset, LinkedPosition position, char trigger, int stateMask,
        LinkedModeModel model) throws CoreException {
      return new ReplaceEdit(position.getOffset(), position.getLength(), fDisplayString);
    }

    public String getAdditionalProposalInfo() {
      return null;
    }

    public String getDisplayString() {
      return fDisplayString;
    }

    public Image getImage() {
      return fImage;
    }

    public int getRelevance() {
      return fRelevance;
    }

    public void setImage(Image image) {
      fImage = image;
    }
  }

  /**
   * A position for the start of the given tracked node position.
   */
  public static class StartPositionInformation extends PositionInformation {

    private final TrackedNodePosition fPos;

    /**
     * A position for the start of the given tracked node position.
     * 
     * @param pos the position
     */
    public StartPositionInformation(TrackedNodePosition pos) {
      fPos = pos;
    }

    @Override
    public int getLength() {
      return 0;
    }

    @Override
    public int getOffset() {
      return fPos.getStartPosition();
    }

    @Override
    public int getSequenceRank() {
      return 0;
    }
  }

  /**
   * A position that contains all of the given tracked node positions.
   */
  public static class TrackedNodesPosition extends PositionInformation {

    private final Collection<TrackedNodePosition> fPos;

    /**
     * A position that contains all of the given tracked node positions.
     * 
     * @param pos the positions
     */
    public TrackedNodesPosition(Collection<TrackedNodePosition> pos) {
      fPos = pos;
    }

    @Override
    public int getLength() {
      int minStart = Integer.MAX_VALUE;
      int maxEnd = 0;
      for (TrackedNodePosition node : fPos) {
        minStart = Math.min(minStart, node.getStartPosition());
        maxEnd = Math.max(maxEnd, node.getStartPosition() + node.getLength());
      }
      return minStart == Integer.MAX_VALUE ? 0 : maxEnd - getOffset();
    }

    @Override
    public int getOffset() {
      int minStart = Integer.MAX_VALUE;
      for (TrackedNodePosition node : fPos) {
        minStart = Math.min(minStart, node.getStartPosition());
      }
      return minStart == Integer.MAX_VALUE ? -1 : minStart;
    }

    @Override
    public int getSequenceRank() {
      return 0;
    }
  }

  private static class TrackedNodePosition2 extends PositionInformation {

    private final TrackedNodePosition fPos;
    private final boolean fIsFirst;

    public TrackedNodePosition2(TrackedNodePosition pos, boolean isFirst) {
      fPos = pos;
      fIsFirst = isFirst;
    }

    @Override
    public int getLength() {
      return fPos.getLength();
    }

    @Override
    public int getOffset() {
      return fPos.getStartPosition();
    }

    @Override
    public int getSequenceRank() {
      return fIsFirst ? 0 : 1;
    }
  }

  public static PositionInformation createPositionInformation(TrackedNodePosition pos,
      boolean isFirst) {
    return new TrackedNodePosition2(pos, isFirst);
  }

  private final String fGroupId;
  private final List<PositionInformation> fPositions;
  private final List<Proposal> fProposals;

  public LinkedProposalPositionGroup(String groupID) {
    fGroupId = groupID;
    fPositions = new ArrayList<PositionInformation>();
    fProposals = new ArrayList<Proposal>();
  }

  public void addPosition(PositionInformation position) {
    fPositions.add(position);
  }

  public void addPosition(TrackedNodePosition position, boolean isFirst) {
    addPosition(createPositionInformation(position, isFirst));
  }

  public void addProposal(Proposal proposal) {
    fProposals.add(proposal);
  }

  public void addProposal(String displayString, Image image, int relevance) {
    addProposal(new Proposal(displayString, image, relevance));
  }

  public String getGroupId() {
    return fGroupId;
  }

  public PositionInformation[] getPositions() {
    return fPositions.toArray(new PositionInformation[fPositions.size()]);
  }

  public Proposal[] getProposals() {
    return fProposals.toArray(new Proposal[fProposals.size()]);
  }

}
