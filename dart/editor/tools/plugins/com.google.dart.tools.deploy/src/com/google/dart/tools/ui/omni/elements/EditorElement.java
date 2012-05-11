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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * Element for editors.
 */
public class EditorElement extends OmniElement {

  private static final String DIRTY_MARK = "*"; //$NON-NLS-1$

  private final IEditorReference editorReference;

  /* package */EditorElement(IEditorReference editorReference, EditorProvider editorProvider) {
    super(editorProvider);
    this.editorReference = editorReference;
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
    final EditorElement other = (EditorElement) obj;
    if (editorReference == null) {
      if (other.editorReference != null) {
        return false;
      }
    } else if (!editorReference.equals(other.editorReference)) {
      return false;
    }
    return true;
  }

  @Override
  public void execute(String text) {
    IWorkbenchPart part = editorReference.getPart(true);
    if (part != null) {
      IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      if (activePage != null) {
        activePage.activate(part);
      }
    }
  }

  @Override
  public String getId() {
    return editorReference.getId() + editorReference.getTitleToolTip();
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return ImageDescriptor.createFromImage(editorReference.getTitleImage());
  }

  @Override
  public String getLabel() {
    boolean dirty = editorReference.isDirty();
    return (dirty ? DIRTY_MARK : "") + editorReference.getTitle(); // + separator + editorReference.getTitleToolTip(); //$NON-NLS-1$
  }

  @Override
  public String getSortLabel() {
    return editorReference.getTitle();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((editorReference == null) ? 0 : editorReference.hashCode());
    return result;
  }
}
