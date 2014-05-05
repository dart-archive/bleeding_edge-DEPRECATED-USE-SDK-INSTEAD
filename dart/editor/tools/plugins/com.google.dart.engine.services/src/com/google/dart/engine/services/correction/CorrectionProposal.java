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

/**
 * Proposal for some change.
 */
public class CorrectionProposal {
  /**
   * An empty array of {@link CorrectionProposal}s.
   */
  public static final CorrectionProposal[] EMPTY_ARRAY = new CorrectionProposal[0];

  private final CorrectionKind kind;
  private final String name;

  public CorrectionProposal(CorrectionKind kind, Object... arguments) {
    this.kind = kind;
    this.name = String.format(kind.getMessage(), arguments);
  }

  /**
   * @return the {@link CorrectionKind} which contains presentation of this
   *         {@link CorrectionProposal}.
   */
  public final CorrectionKind getKind() {
    return kind;
  }

  /**
   * @return the name to display for user.
   */
  public final String getName() {
    return name;
  }
}
