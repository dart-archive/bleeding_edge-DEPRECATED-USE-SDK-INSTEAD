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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;

import java.util.List;

/**
 * 
 */
public abstract class ViewHistory {

  public abstract void addMenuEntries(MenuManager manager);

  /**
   * Configure the history drop down action. Clients typically want to set a tooltip and an image.
   * 
   * @param action the action
   */
  public abstract void configureHistoryDropDownAction(IAction action);

  /**
   * Configure the history List action. Clients typically want to set a text and an image.
   * 
   * @param action the action
   */
  public abstract void configureHistoryListAction(IAction action);

  /**
   * @return a history drop down action, ready for inclusion in a view toolbar
   */
  public final IAction createHistoryDropDownAction() {
    return new HistoryDropDownAction(this);
  }

  /**
   * @return action to clear history entries, or <code>null</code>
   */
  public abstract Action getClearAction();

  /**
   * @return the active entry from the history
   */
  public abstract Object getCurrentEntry();

  /**
   * @return An unmodifiable list of history entries, can be empty. The list is sorted by age,
   *         youngest first.
   */
  public abstract List<Object> getHistoryEntries();

  public abstract String getHistoryListDialogMessage();

  public abstract String getHistoryListDialogTitle();

  /**
   * @param element the element to render
   * @return the image descriptor for the given element, or <code>null</code>
   */
  public abstract ImageDescriptor getImageDescriptor(Object element);

  public abstract int getMaxEntries();

  public abstract String getMaxEntriesMessage();

  public abstract Shell getShell();

  /**
   * @param element the element to render
   * @return the label text for the given element
   */
  public abstract String getText(Object element);

  /**
   * @param entry the entry to activate, or <code>null</code> if none should be active
   */
  public abstract void setActiveEntry(Object entry);

  /**
   * @param remainingEntries all the remaining history entries, can be empty
   * @param activeEntry the entry to activate, or <code>null</code> if none should be active
   */
  public abstract void setHistoryEntries(List<Object> remainingEntries, Object activeEntry);

  public abstract void setMaxEntries(int maxEntries);

}
