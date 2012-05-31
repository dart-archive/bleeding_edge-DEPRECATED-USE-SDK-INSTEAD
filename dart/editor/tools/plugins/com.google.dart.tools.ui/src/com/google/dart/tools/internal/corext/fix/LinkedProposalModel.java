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
import com.google.dart.tools.internal.corext.fix.LinkedProposalPositionGroup.PositionInformation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LinkedProposalModel {

  private Map<String, LinkedProposalPositionGroup> fPositionGroups;
  private PositionInformation fEndPosition;

  public void addPositionGroup(LinkedProposalPositionGroup positionGroup) {
    if (positionGroup == null) {
      throw new IllegalArgumentException("positionGroup must not be null"); //$NON-NLS-1$
    }

    if (fPositionGroups == null) {
      fPositionGroups = new HashMap<String, LinkedProposalPositionGroup>();
    }
    fPositionGroups.put(positionGroup.getGroupId(), positionGroup);
  }

  public void clear() {
    fPositionGroups = null;
    fEndPosition = null;
  }

  public PositionInformation getEndPosition() {
    return fEndPosition;
  }

  public LinkedProposalPositionGroup getPositionGroup(String groupId, boolean createIfNotExisting) {
    LinkedProposalPositionGroup group = fPositionGroups != null
        ? fPositionGroups.get(groupId)
        : null;
    if (createIfNotExisting && group == null) {
      group = new LinkedProposalPositionGroup(groupId);
      addPositionGroup(group);
    }
    return group;
  }

  public Iterator<LinkedProposalPositionGroup> getPositionGroupIterator() {
    if (fPositionGroups == null) {
      return new Iterator<LinkedProposalPositionGroup>() {
        @Override
        public boolean hasNext() {
          return false;
        }

        @Override
        public LinkedProposalPositionGroup next() {
          return null;
        }

        @Override
        public void remove() {
        }
      };
    }
    return fPositionGroups.values().iterator();
  }

  public boolean hasLinkedPositions() {
    return fPositionGroups != null && !fPositionGroups.isEmpty();
  }

  /**
   * Sets the end position of the linked mode to the end of the passed range.
   * 
   * @param position The position that describes the end position of the linked mode.
   */
  public void setEndPosition(PositionInformation position) {
    fEndPosition = position;
  }

  public void setEndPosition(TrackedNodePosition position) {
    setEndPosition(LinkedProposalPositionGroup.createPositionInformation(position, false));
  }

}
