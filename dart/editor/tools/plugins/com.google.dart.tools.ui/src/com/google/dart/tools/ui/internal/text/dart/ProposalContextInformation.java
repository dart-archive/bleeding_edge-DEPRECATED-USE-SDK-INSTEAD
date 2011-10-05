/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.text.dart.CompletionProposalLabelProvider;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.swt.graphics.Image;

/**
 * Implementation of the <code>IContextInformation</code> interface.
 */
public final class ProposalContextInformation implements IContextInformation,
    IContextInformationExtension {

  private final String fContextDisplayString;
  private final String fInformationDisplayString;
  private final Image fImage;
  private int fPosition;

  /**
   * Creates a new context information.
   */
  public ProposalContextInformation(CompletionProposal proposal) {
    // don't cache the core proposal because the ContentAssistant might
    // hang on to the context info.
    CompletionProposalLabelProvider labelProvider = new CompletionProposalLabelProvider();
    fInformationDisplayString = labelProvider.createParameterList(proposal);
    ImageDescriptor descriptor = labelProvider.createImageDescriptor(proposal);
    if (descriptor != null) {
      fImage = DartToolsPlugin.getImageDescriptorRegistry().get(descriptor);
    } else {
      fImage = null;
    }
    if (proposal.getCompletion().length == 0) {
      fPosition = proposal.getCompletionLocation() + 1;
    } else {
      fPosition = -1;
    }
    fContextDisplayString = labelProvider.createLabel(proposal);
  }

  /*
   * @see IContextInformation#equals
   */
  @Override
  public boolean equals(Object object) {
    if (object instanceof IContextInformation) {
      IContextInformation contextInformation = (IContextInformation) object;
      boolean equals = getInformationDisplayString().equalsIgnoreCase(
          contextInformation.getInformationDisplayString());
      if (getContextDisplayString() != null) {
        equals = equals
            && getContextDisplayString().equalsIgnoreCase(
                contextInformation.getContextDisplayString());
      }
      return equals;
    }
    return false;
  }

  /*
   * @see IContextInformation#getContextDisplayString()
   */
  @Override
  public String getContextDisplayString() {
    return fContextDisplayString;
  }

  /*
   * @see IContextInformationExtension#getContextInformationPosition()
   */
  @Override
  public int getContextInformationPosition() {
    return fPosition;
  }

  /*
   * @see IContextInformation#getImage()
   */
  @Override
  public Image getImage() {
    return fImage;
  }

  /*
   * @see IContextInformation#getInformationDisplayString()
   */
  @Override
  public String getInformationDisplayString() {
    return fInformationDisplayString;
  }

  /**
   * Sets the context information position.
   * 
   * @param position the new position, or -1 for unknown.
   */
  public void setContextInformationPosition(int position) {
    fPosition = position;
  }
}
