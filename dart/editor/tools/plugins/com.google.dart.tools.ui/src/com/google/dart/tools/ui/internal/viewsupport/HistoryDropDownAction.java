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
package com.google.dart.tools.ui.internal.viewsupport;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;

import java.util.List;

/* package */class HistoryDropDownAction extends Action {

  private class HistoryAction extends Action {
    private final Object fElement;

    public HistoryAction(Object element, int accelerator) {
      super("", AS_RADIO_BUTTON); //$NON-NLS-1$
      Assert.isNotNull(element);
      fElement = element;

      String label = fHistory.getText(element);
      if (accelerator < 10) {
        // add the numerical accelerator
        label = new StringBuffer().append('&').append(accelerator).append(' ').append(label).toString();
      }

      setText(label);
      setImageDescriptor(fHistory.getImageDescriptor(element));
    }

    @Override
    public void run() {
      fHistory.setActiveEntry(fElement);
    }
  }

  private class HistoryMenuCreator implements IMenuCreator {

    @Override
    public void dispose() {
      fHistory = null;

      if (fMenu != null) {
        fMenu.dispose();
        fMenu = null;
      }
    }

    @Override
    public Menu getMenu(Control parent) {
      if (fMenu != null) {
        fMenu.dispose();
      }
      final MenuManager manager = new MenuManager();
      manager.setRemoveAllWhenShown(true);
      manager.addMenuListener(new IMenuListener() {
        @Override
        public void menuAboutToShow(IMenuManager manager2) {
          List<Object> entries = fHistory.getHistoryEntries();
          boolean checkOthers = addEntryMenuItems(manager2, entries);

          manager2.add(new Separator());

          Action others = new HistoryListAction(fHistory);
          others.setChecked(checkOthers);
          manager2.add(others);

          Action clearAction = fHistory.getClearAction();
          if (clearAction != null) {
            manager2.add(clearAction);
          }

          manager2.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

          fHistory.addMenuEntries(manager);
        }

        private boolean addEntryMenuItems(IMenuManager manager2, List<Object> entries) {
          if (entries.isEmpty()) {
            return false;
          }

          boolean checkOthers = true;
          int min = Math.min(entries.size(), RESULTS_IN_DROP_DOWN);
          for (int i = 0; i < min; i++) {
            Object entry = entries.get(i);
            HistoryAction action = new HistoryAction(entry, i + 1);
            boolean check = entry.equals(fHistory.getCurrentEntry());
            action.setChecked(check);
            if (check) {
              checkOthers = false;
            }
            manager2.add(action);
          }
          return checkOthers;
        }
      });

      fMenu = manager.createContextMenu(parent);

      // workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=129973
      final Display display = parent.getDisplay();
      fMenu.addMenuListener(new MenuAdapter() {
        @Override
        public void menuHidden(final MenuEvent e) {
          display.asyncExec(new Runnable() {
            @Override
            public void run() {
              manager.removeAll();
              if (fMenu != null) {
                fMenu.dispose();
                fMenu = null;
              }
            }
          });
        }
      });
      return fMenu;
    }

    @Override
    public Menu getMenu(Menu parent) {
      return null;
    }
  }

  public static final int RESULTS_IN_DROP_DOWN = 10;

  private ViewHistory fHistory;
  private Menu fMenu;

  public HistoryDropDownAction(ViewHistory history) {
    fHistory = history;
    fMenu = null;
    setMenuCreator(new HistoryMenuCreator());
    fHistory.configureHistoryDropDownAction(this);
  }

  @Override
  public void run() {
    new HistoryListAction(fHistory).run();
  }
}
