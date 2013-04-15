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

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.util.GridLayoutFactory;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 * {@link ViewPart} for displaying search results.
 */
public class SearchView extends ViewPart {
  public static final String ID = "com.google.dart.tools.SearchView";
  public static final String SEARCH_MARKER = "com.google.dart.tools.search.searchmarker";

  static void updateColors(Control control) {
    SWTUtil.setColors(control, DartToolsPlugin.getDefault().getCombinedPreferenceStore());
  }

  private IActionBars actionBars;
  private PageBook pageBook;
  private SubActionBars pageActionBars;

  private SearchPage page;
  private Composite emptyComposite;

  @Override
  public void createPartControl(Composite parent) {
    actionBars = getViewSite().getActionBars();
    pageBook = new PageBook(parent, SWT.NONE);
    // empty page
    {
      emptyComposite = new Composite(pageBook, SWT.NONE);
      GridLayoutFactory.create(emptyComposite);
      {
        Label emptyLabel = new Label(emptyComposite, SWT.WRAP);
        emptyLabel.setText("No search results available.");
      }
      updateColors(emptyComposite);
    }
    // show empty page
    pageBook.showPage(emptyComposite);
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
      page.dispose();
      pageActionBars.dispose();
    }
    // activate new page
    page = newPage;
    if (page != null) {
      // show page Control
      page.createControl(pageBook);
      pageBook.showPage(page.getControl());
      // show page actions
      pageActionBars = new SubActionBars(actionBars);
      pageActionBars.activate();
      page.setActionBars(pageActionBars);
      pageActionBars.updateActionBars();
      // notify page
      page.show();
    } else {
      pageBook.showPage(emptyComposite);
      actionBars.updateActionBars();
    }
  }
}
