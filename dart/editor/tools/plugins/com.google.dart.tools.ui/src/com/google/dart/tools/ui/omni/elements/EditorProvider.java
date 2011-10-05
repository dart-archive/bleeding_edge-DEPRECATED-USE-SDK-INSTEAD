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

import com.google.dart.tools.ui.omni.OmniBoxMessages;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import java.util.HashMap;
import java.util.Map;

/**
 * Provider for editor elements.
 */
public class EditorProvider extends OmniProposalProvider {

  private Map<String, EditorElement> idToElement;

  @Override
  public OmniElement getElementForId(String id) {
    getElements(id);
    return idToElement.get(id);
  }

  @Override
  public OmniElement[] getElements(String pattern) {
    if (idToElement == null) {
      idToElement = new HashMap<String, EditorElement>();
      IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      if (activePage == null) {
        return new OmniElement[0];
      }
      IEditorReference[] editors = activePage.getEditorReferences();
      for (int i = 0; i < editors.length; i++) {
        EditorElement editorElement = new EditorElement(editors[i], this);
        idToElement.put(editorElement.getId(), editorElement);
      }
    }
    return idToElement.values().toArray(new OmniElement[idToElement.values().size()]);
  }

  @Override
  public String getId() {
    return getClass().getName();
  }

  @Override
  public String getName() {
    return OmniBoxMessages.OmniBox_Editors;
  }
}
