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
package com.google.dart.tools.search.internal.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.eclipse.ui.activities.WorkbenchActivityHelper;

import java.util.Iterator;
import java.util.List;

public class OpenSearchDialogPageAction implements IWorkbenchWindowPulldownDelegate2 {

  private static final class SearchPageAction extends Action {
    private final OpenSearchDialogAction fOpenSearchDialogAction;

    public SearchPageAction(IWorkbenchWindow workbenchWindow, SearchPageDescriptor pageDescriptor) {
      super();
      fOpenSearchDialogAction = new OpenSearchDialogAction(workbenchWindow, pageDescriptor.getId());
      init(pageDescriptor);
    }

    @Override
    public void run() {
      fOpenSearchDialogAction.run(this);
    }

    private void init(SearchPageDescriptor pageDesc) {
      setText(pageDesc.getLabel());
      setToolTipText(pageDesc.getLabel());
      ImageDescriptor imageDescriptor = pageDesc.getImage();
      if (imageDescriptor != null) {
        setImageDescriptor(imageDescriptor);
      }
    }

  }

  private IWorkbenchWindow fWorkbenchWindow;

  private OpenSearchDialogAction fOpenSearchDialogAction;

  @Override
  public void dispose() {
    if (fOpenSearchDialogAction != null) {
      fOpenSearchDialogAction.dispose();
    }
  }

  @Override
  public Menu getMenu(Control parent) {
    Menu menu = new Menu(parent);
    fillMenu(menu);
    return menu;
  }

  @Override
  public Menu getMenu(Menu parent) {
    Menu menu = new Menu(parent);
    fillMenu(menu);
    return menu;
  }

  @Override
  public void init(IWorkbenchWindow window) {
    fWorkbenchWindow = window;
  }

  @Override
  public void run(IAction action) {
    if (fOpenSearchDialogAction == null) {
      fOpenSearchDialogAction = new OpenSearchDialogAction();
    }
    fOpenSearchDialogAction.run(action);
  }

  @Override
  public void selectionChanged(IAction action, ISelection sel) {
    // Empty
  }

  private void addToMenu(Menu localMenu, IAction action, int accelerator) {
    StringBuffer label = new StringBuffer();
    if (accelerator >= 0 && accelerator < 10) {
      //add the numerical accelerator
      label.append('&');
      label.append(accelerator);
      label.append(' ');
    }
    label.append(action.getText());
    action.setText(label.toString());
    ActionContributionItem item = new ActionContributionItem(action);
    item.fill(localMenu, -1);
  }

  private void fillMenu(final Menu localMenu) {
    List<SearchPageDescriptor> pageDescriptors = SearchPlugin.getDefault().getSearchPageDescriptors();
    int accelerator = 1;
    for (Iterator<SearchPageDescriptor> iter = pageDescriptors.iterator(); iter.hasNext();) {
      SearchPageDescriptor desc = iter.next();
      if (!WorkbenchActivityHelper.filterItem(desc) && desc.isEnabled()) {
        SearchPageAction action = new SearchPageAction(fWorkbenchWindow, desc);
        addToMenu(localMenu, action, accelerator++);
      }
    }
    localMenu.addMenuListener(new MenuAdapter() {
      @Override
      public void menuHidden(MenuEvent e) {
        e.display.asyncExec(new Runnable() {
          @Override
          public void run() {
            localMenu.dispose();
          }
        });
      }
    });

  }
}
