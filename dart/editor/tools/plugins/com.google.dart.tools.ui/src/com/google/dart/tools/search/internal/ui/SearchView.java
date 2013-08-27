/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.util.GridLayoutFactory;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageSite;
import org.eclipse.ui.part.ViewPart;

/**
 * {@link ViewPart} for displaying search results.
 * 
 * @coverage dart.editor.ui.search
 */
public class SearchView extends ViewPart {
  public static final String ID = "com.google.dart.tools.SearchView";
  public static final String SEARCH_MARKER = "com.google.dart.tools.search.searchmarker";

  private IActionBars actionBars;
  private PageBook pageBook;
  private SubActionBars pageActionBars;

  private Composite emptyComposite;
  private Label emptyLabel;

  private SearchPage page;

  private IPreferenceStore preferences;
  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      updateColors();
    }
  };

  @Override
  public void createPartControl(Composite parent) {
    actionBars = getViewSite().getActionBars();
    pageBook = new PageBook(parent, SWT.NONE);
    // empty page
    {
      emptyComposite = new Composite(pageBook, SWT.NONE);
      GridLayoutFactory.create(emptyComposite);
      {
        emptyLabel = new Label(emptyComposite, SWT.WRAP);
        emptyLabel.setText("No search results available.");
        SWTUtil.bindJFaceResourcesFontToControl(emptyComposite);
      }
    }
    // show empty page
    pageBook.showPage(emptyComposite);
    // update colors
    preferences = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
    preferences.addPropertyChangeListener(propertyChangeListener);
    updateColors();
  }

  @Override
  public void dispose() {
    preferences.removePropertyChangeListener(propertyChangeListener);
    showPage(null);
    super.dispose();
  }

  /**
   * @return the time when query was last time finished.
   */
  @VisibleForTesting
  public long getLastQueryFinishTime() {
    return page.getLastQueryFinishTime();
  }

  /**
   * @return the time when query was last time started.
   */
  @VisibleForTesting
  public long getLastQueryStartTime() {
    return page.getLastQueryStartTime();
  }

  @Override
  public void setFocus() {
    pageBook.setFocus();
  }

  /**
   * Shows given {@link SearchPage}.
   * 
   * @param newPage the {@link SearchPage} to show. May be <code>null</code> to show empty page.
   */
  public void showPage(SearchPage newPage) {
    // dispose previous page
    if (page != null) {
      setContentDescription("");
      page.dispose();
      pageActionBars.dispose();
    }
    // activate new page
    page = newPage;
    if (page != null) {
      // set IPageSite
      pageActionBars = new SubActionBars(actionBars);
      pageActionBars.activate();
      IPageSite pageSite = new PageSite(getViewSite()) {
        @Override
        public IActionBars getActionBars() {
          return pageActionBars;
        }
      };
      page.init(pageSite);
      // show page Control
      page.createControl(pageBook);
      pageBook.showPage(page.getControl());
      // show page actions
      page.setActionBars(pageActionBars);
      pageActionBars.updateActionBars();
      // notify page
      page.show();
    } else {
      pageBook.showPage(emptyComposite);
      actionBars.updateActionBars();
    }
  }

  @Override
  protected void setContentDescription(String description) {
    super.setContentDescription(description);
  }

  private void updateColors() {
    if (emptyComposite.isDisposed()) {
      return;
    }
    SWTUtil.setColors(emptyComposite, preferences);
    SWTUtil.setColors(emptyLabel, preferences);
  }
}
