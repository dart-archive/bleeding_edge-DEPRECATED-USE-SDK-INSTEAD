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

import java.util.List;

/**
 * Proposal for single source file change.
 */
public class CorrectionProposal {
  private final CorrectionImage image;
  private final String name;
  private final int relevance;
  private final List<SourceChange> changes = Lists.newArrayList();

  public CorrectionProposal(CorrectionImage image, String name, int relevance) {
    this.image = image;
    this.name = name;
    this.relevance = relevance;
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
   * @return the image to be displayed in the list of correction proposals.
   */
  public CorrectionImage getImage() {
    return image;
  }

  /**
   * @return the string to be displayed in the list of correction proposals.
   */
  public String getName() {
    return name;
  }

  /**
   * @return the relevance of this proposal - greater value, higher in the list of proposals.
   */
  public int getRelevance() {
    return relevance;
  }
}
