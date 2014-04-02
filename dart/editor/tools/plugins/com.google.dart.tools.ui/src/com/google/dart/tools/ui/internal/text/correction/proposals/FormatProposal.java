/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.text.correction.CorrectionMessages;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.graphics.Image;

/**
 * A quick assist proposal that starts the Format action.
 * 
 * @coverage dart.editor.ui.correction
 */
public class FormatProposal extends AbstractActionProposal {
  public FormatProposal(IAction action) {
    super(action, CorrectionMessages.FormatProposal_name);
  }

  @Override
  public String getAdditionalProposalInfo() {
    return CorrectionMessages.FormatProposal_additionalInfo;
  }

  @Override
  public Image getImage() {
    return DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE);
  }

  @Override
  public int getRelevance() {
    return 5;
  }
}
