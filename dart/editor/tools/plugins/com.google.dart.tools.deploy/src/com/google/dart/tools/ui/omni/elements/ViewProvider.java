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

import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Provider for view elements.
 */
public class ViewProvider extends OmniProposalProvider {

  private OmniElement[] cachedElements;
  private final Map<String, ViewElement> idToElement = new HashMap<String, ViewElement>();
  private final Collection<String> multiInstanceViewIds = new HashSet<String>(0);

  public void addOpenViews(Collection<ViewElement> elements) {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    IViewRegistry viewRegistry = PlatformUI.getWorkbench().getViewRegistry();
    IViewReference[] refs = page.getViewReferences();
    for (int i = 0; i < refs.length; i++) {
      IViewDescriptor viewDescriptor = viewRegistry.find(refs[i].getId());
      addElement(viewDescriptor, elements, refs[i].getSecondaryId(),
          refs[i].getContentDescription());
    }
  }

  @Override
  public OmniElement getElementForId(String id) {
    getElements(id);
    return idToElement.get(id);
  }

  @Override
  public OmniElement[] getElements(String pattern) {
    if (PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() == null) {
      cachedElements = null;
      return new OmniElement[0];
    }
    if (cachedElements == null) {
      IViewDescriptor[] views = PlatformUI.getWorkbench().getViewRegistry().getViews();
      Collection<ViewElement> elements = new HashSet<ViewElement>(views.length);
      for (int i = 0; i < views.length; i++) {
        if (!WorkbenchActivityHelper.filterItem(views[i])) {
          addElement(views[i], elements, null, null);
        }
      }

      addOpenViews(elements);

      markMultiInstance(elements);

      cachedElements = elements.toArray(new OmniElement[elements.size()]);

    }
    return cachedElements;
  }

  @Override
  public String getId() {
    return getClass().getName();
  }

  @Override
  public String getName() {
    return OmniBoxMessages.OmniBox_Views;
  }

  private void addElement(IViewDescriptor viewDesc, Collection<ViewElement> elements,
      String secondaryId, String desc) {
    ViewElement viewElement = new ViewElement(viewDesc, this);
    viewElement.setSecondaryId(secondaryId);
    viewElement.setContentDescription(desc);
    boolean added = elements.add(viewElement);
    if (added) {
      idToElement.put(viewElement.getId(), viewElement);
    } else {
      // *could* be multinstance
      multiInstanceViewIds.add(viewDesc.getId());
    }
  }

  private void markMultiInstance(Collection<ViewElement> elements) {
    for (Iterator<String> i = multiInstanceViewIds.iterator(); i.hasNext();) {
      String viewId = i.next();
      ViewElement firstInstance = null;
      for (Iterator<ViewElement> j = elements.iterator(); j.hasNext();) {
        ViewElement viewElement = j.next();
        if (viewElement.getPrimaryId().equals(viewId)) {
          if (firstInstance == null) {
            firstInstance = viewElement;
          } else {
            firstInstance.setMultiInstance(true);
            viewElement.setMultiInstance(true);
          }
        }
      }
    }
  }
}
