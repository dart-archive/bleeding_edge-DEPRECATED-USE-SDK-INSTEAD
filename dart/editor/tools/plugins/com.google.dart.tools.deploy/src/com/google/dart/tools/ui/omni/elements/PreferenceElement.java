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
package com.google.dart.tools.ui.omni.elements;

import com.google.dart.tools.ui.omni.OmniElement;

import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.dialogs.WorkbenchPreferenceDialog;

/**
 * Element for preference nodes.
 */
public class PreferenceElement extends OmniElement {

  private final IPreferenceNode preferenceNode;

  private final String prefix;

  /* package */PreferenceElement(IPreferenceNode preferenceNode, String prefix,
      PreferenceProvider preferenceProvider) {
    super(preferenceProvider);
    this.preferenceNode = preferenceNode;
    this.prefix = prefix;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final PreferenceElement other = (PreferenceElement) obj;
    if (preferenceNode == null) {
      if (other.preferenceNode != null) {
        return false;
      }
    } else if (!preferenceNode.equals(other.preferenceNode)) {
      return false;
    }
    return true;
  }

  @Override
  public void execute(String text) {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window != null) {
      WorkbenchPreferenceDialog dialog = WorkbenchPreferenceDialog.createDialogOn(
          window.getShell(), preferenceNode.getId());
      dialog.open();
    }
  }

  @Override
  public String getId() {
    return preferenceNode.getId();
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    Image image = preferenceNode.getLabelImage();
    if (image != null) {
      ImageDescriptor descriptor = ImageDescriptor.createFromImage(image);
      return descriptor;
    }
    return null;
  }

  @Override
  public String getLabel() {
    if (prefix != null && prefix.length() > 0) {
      return preferenceNode.getLabelText() + separator + prefix;
    }
    return preferenceNode.getLabelText();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((preferenceNode == null) ? 0 : preferenceNode.hashCode());
    return result;
  }
}
