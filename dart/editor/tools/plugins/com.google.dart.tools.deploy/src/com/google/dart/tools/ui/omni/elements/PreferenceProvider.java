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

import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.WorkbenchActivityHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider for preference elements.
 */
public class PreferenceProvider extends OmniProposalProvider {

  private OmniElement[] cachedElements;
  private final Map<String, PreferenceElement> idToElement = new HashMap<String, PreferenceElement>();

  @Override
  public OmniElement getElementForId(String id) {
    getElements(id);
    return idToElement.get(id);
  }

  @Override
  public OmniElement[] getElements(String pattern) {
    if (cachedElements == null) {
      List<PreferenceElement> list = new ArrayList<PreferenceElement>();
      collectElements("", PlatformUI.getWorkbench().getPreferenceManager().getRootSubNodes(), list); //$NON-NLS-1$
      cachedElements = new PreferenceElement[list.size()];
      for (int i = 0; i < list.size(); i++) {
        PreferenceElement preferenceElement = list.get(i);
        cachedElements[i] = preferenceElement;
        idToElement.put(preferenceElement.getId(), preferenceElement);
      }
    }
    return cachedElements;
  }

  @Override
  public String getId() {
    return getClass().getName();
  }

  @Override
  public String getName() {
    return OmniBoxMessages.OmniBox_Preferences;
  }

  private void collectElements(String prefix, IPreferenceNode[] subNodes,
      List<PreferenceElement> result) {
    for (int i = 0; i < subNodes.length; i++) {
      if (!WorkbenchActivityHelper.filterItem(subNodes[i])) {
        PreferenceElement preferenceElement = new PreferenceElement(subNodes[i], prefix, this);
        result.add(preferenceElement);
        String nestedPrefix = prefix.length() == 0 ? subNodes[i].getLabelText()
            : (prefix + "/" + subNodes[i].getLabelText()); //$NON-NLS-1$
        collectElements(nestedPrefix, subNodes[i].getSubNodes(), result);
      }
    }
  }
}
