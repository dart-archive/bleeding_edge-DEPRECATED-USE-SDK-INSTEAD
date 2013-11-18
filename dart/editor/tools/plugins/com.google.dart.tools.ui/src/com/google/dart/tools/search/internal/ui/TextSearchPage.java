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
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.search.internal.ui.text.FileSearchPage;
import com.google.dart.tools.search.internal.ui.text.FileSearchQuery;
import com.google.dart.tools.search.internal.ui.text.FileSearchResult;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.text.FileTextSearchScope;
import com.google.dart.tools.search.ui.text.Match;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.resources.IFile;
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

import java.text.MessageFormat;

/**
 * @coverage dart.editor.ui.search
 */
public class TextSearchPage extends SearchPage {
  private static final String[] DEFAULT_FILE_EXTENSIONS = {"*"};

  private final SearchView searchView;
  private final String taskName;
  private final String searchText;
  private FileSearchPage fileSearchPage;
  private FileSearchResult searchResult;

  private IAction refreshAction = new Action() {
    {
      setToolTipText("Refresh the Current Search");
      DartPluginImages.setLocalImageDescriptors(this, "refresh.gif");
    }

    @Override
    public void run() {
      refresh();
    }
  };

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
  public long getLastQueryFinishTime() {
    return 0;
  }

  @Override
  public long getLastQueryStartTime() {
    return 0;
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
    toolBarManager.add(refreshAction);
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

  private boolean isInWrongPackageFolder(IFile file) {
    return DartCore.isInSelfLinkedPackageFolder(file) || DartCore.isInDuplicatePackageFolder(file);
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
            removeWrongPackageFolders();
          } catch (Throwable e) {
            DartToolsPlugin.log(e);
            return Status.CANCEL_STATUS;
          }
          // schedule UI update
          new UIJob("Displaying search results...") {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
              searchView.setContentDescription(MessageFormat.format(
                  "''{0}'' - {1} text occurrences in workspace",
                  searchText,
                  searchResult.getMatchCount()));
              if (fileSearchPage != null) {
                fileSearchPage.setInput(searchResult, fileSearchPage);
                fileSearchPage.setViewPart(searchView);
              }
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

  /**
   * Removes matches corresponding to the resources located in the "packages" sub-folder that
   * references the enclosing package. There are no reason to show them - they are always duplicates
   * and we don't want to edit "packages" resources anyway.
   */
  private void removeWrongPackageFolders() {
    Object[] elements = searchResult.getElements();
    for (Object element : elements) {
      // prepare file
      IFile file = searchResult.getFile(element);
      if (file == null) {
        continue;
      }
      // remove if in wrong folder
      if (isInWrongPackageFolder(file)) {
        Match[] matches = searchResult.getMatches(element);
        searchResult.removeMatches(matches);
        continue;
      }
    }
  }
}
