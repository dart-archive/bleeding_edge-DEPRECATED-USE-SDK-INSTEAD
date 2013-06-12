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

import com.google.dart.engine.search.SearchMatch;
import com.google.dart.tools.search.internal.ui.text.FileSearchPage;
import com.google.dart.tools.search.internal.ui.text.FileSearchQuery;
import com.google.dart.tools.search.internal.ui.text.FileSearchResult;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.text.FileTextSearchScope;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.progress.UIJob;

public class TextSearchPage extends SearchPage {
  private static final String[] DEFAULT_FILE_EXTENSIONS = {"*"};
  private final SearchView searchView;
  private final String taskName;
  private final String searchText;
  private FileSearchPage fileSearchPage;
  private FileSearchResult searchResult;

  private IAction removeAction = new Action() {
    {
      setToolTipText("Remove Selected Matches");
      DartPluginImages.setLocalImageDescriptors(this, "search_rem.gif");
    }

    @Override
    public void run() {
      fileSearchPage.internalRemoveSelected();
    }
  };

  private IAction removeAllAction = new Action() {
    {
      setToolTipText("Remove All Matches");
      DartPluginImages.setLocalImageDescriptors(this, "search_remall.gif");
    }

    @Override
    public void run() {
      searchView.showPage(null);
    }
  };

  public TextSearchPage(SearchView searchView, String taskName, String searchText) {
    this.searchView = searchView;
    this.taskName = taskName;
    this.searchText = searchText;
    fileSearchPage = new FileSearchPage();
  }

  @Override
  public void createControl(Composite parent) {
    fileSearchPage.createControl(parent);
  }

  @Override
  public void dispose() {
    super.dispose();
    if (fileSearchPage != null) {
      fileSearchPage.dispose();
      fileSearchPage = null;
    }
  }

  @Override
  public Control getControl() {
    return fileSearchPage.getControl();
  }

  @Override
  public void init(IPageSite pageSite) {
    super.init(pageSite);
    fileSearchPage.init(pageSite);
  }

  @Override
  public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager,
      IStatusLineManager statusLineManager) {
    toolBarManager.add(new Separator());
    toolBarManager.add(removeAction);
    toolBarManager.add(removeAllAction);
    toolBarManager.add(new Separator());
  }

  @Override
  public void setFocus() {
    fileSearchPage.setFocus();
  }

  @Override
  public void show() {
    refresh();
  }

  private FileTextSearchScope createTextSearchScope() {
    return FileTextSearchScope.newWorkspaceScope(DEFAULT_FILE_EXTENSIONS, false);
  }

  private ISearchQuery newQuery() throws CoreException {
    return new FileSearchQuery(searchText, false, false, createTextSearchScope());
  }

  /**
   * Runs background {@link Job} to fetch {@link SearchMatch}s and then displays them in the
   * {@link #viewer}.
   */
  private void refresh() {
    try {
      new Job(taskName) {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          try {
            ISearchQuery searchQuery = newQuery();
            searchQuery.run(monitor);
            searchResult = (FileSearchResult) searchQuery.getSearchResult();
          } catch (Throwable e) {
            DartToolsPlugin.log(e);
            return Status.CANCEL_STATUS;
          }
          // schedule UI update
          new UIJob("Displaying search results...") {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
              fileSearchPage.setInput(searchResult, fileSearchPage);
              return Status.OK_STATUS;
            }
          }.schedule();
          // done
          return Status.OK_STATUS;
        }
      }.schedule();
    } catch (Throwable e) {
      ExceptionHandler.handle(e, "Search", "Exception during search.");
    }
  }
}
