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
package com.google.dart.tools.ui.omni.elements;

import com.google.dart.tools.search.ui.actions.TextSearchAction;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class HeaderElement extends OmniElement {

  public HeaderElement(OmniProposalProvider provider) {
    super(provider);
  }

  @Override
  public void execute(String text) {
    new TextSearchAction(getShell(), text).run();
  }

  @Override
  public String getId() {
    return getProvider().getId();
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return null;
  }

  @Override
  public String getLabel() {
    return getProvider().getName();
  }

  private Shell getShell() {
    OmniProposalProvider proposalProvider = getProvider();
    if (proposalProvider instanceof IShellProvider) {
      return ((IShellProvider) proposalProvider).getShell();
    }
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
  }

}
