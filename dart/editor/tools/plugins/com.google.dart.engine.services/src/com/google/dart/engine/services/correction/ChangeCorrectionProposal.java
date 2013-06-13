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

import com.google.dart.engine.services.change.Change;

/**
 * {@link CorrectionProposal} with some {@link Change}.
 */
public class ChangeCorrectionProposal extends CorrectionProposal {
  private final Change change;

  public ChangeCorrectionProposal(Change change, CorrectionKind kind, Object... arguments) {
    super(kind, arguments);
    this.change = change;
  }

  /**
   * @return the {@link Change} to perform.
   */
  public Change getChange() {
    return change;
  }
}
