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
package com.google.dart.tools.search2.internal.ui;

import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.internal.ui.SearchPluginImages;
import com.google.dart.tools.search.internal.ui.SearchPreferencePage;
import com.google.dart.tools.search.ui.IQueryListener;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.ISearchResultViewPart;
import com.google.dart.tools.search2.internal.ui.text.PositionTracker;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;

public class InternalSearchUI {

  private class InternalSearchJob extends Job {

    private SearchJobRecord fSearchJobRecord;

    public InternalSearchJob(SearchJobRecord sjr) {
      super(sjr.query.getLabel());

      fSearchJobRecord = sjr;
    }

    @Override
    public boolean belongsTo(Object family) {
      return family == InternalSearchUI.FAMILY_SEARCH;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      fSearchJobRecord.job = this;
      searchJobStarted(fSearchJobRecord);
      IStatus status = null;
      int origPriority = Thread.currentThread().getPriority();
      try {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      } catch (SecurityException e) {
      }
      try {
        status = fSearchJobRecord.query.run(monitor);
      } finally {
        try {
          Thread.currentThread().setPriority(origPriority);
        } catch (SecurityException e) {
        }
        searchJobFinished(fSearchJobRecord);
      }
      fSearchJobRecord.job = null;
      return status;
    }

  }

  private class SearchJobRecord {
    public ISearchQuery query;
    public Job job;
    public boolean isRunning;

    public SearchJobRecord(ISearchQuery job) {
      this.query = job;
      this.isRunning = false;
      this.job = null;
    }
  }

  //The shared instance.
  private static InternalSearchUI fgInstance;

  public static void shutdown() {
    InternalSearchUI instance = fgInstance;
    if (instance != null) {
      instance.doShutdown();
    }
  }

  // contains all running jobs
  private HashMap<ISearchQuery, SearchJobRecord> fSearchJobs;

  private QueryManager fSearchResultsManager;

  private PositionTracker fPositionTracker;

  private SearchViewManager fSearchViewManager;

  public static final Object FAMILY_SEARCH = new Object();

  /**
   * @return returns the shared instance.
   */
  public static InternalSearchUI getInstance() {
    if (fgInstance == null) {
      fgInstance = new InternalSearchUI();
    }
    return fgInstance;
  }

  /**
   * The constructor.
   */
  public InternalSearchUI() {
    fgInstance = this;
    fSearchJobs = new HashMap<ISearchQuery, SearchJobRecord>();
    fSearchResultsManager = new QueryManager();
    fPositionTracker = new PositionTracker();

    fSearchViewManager = new SearchViewManager(fSearchResultsManager);

    PlatformUI.getWorkbench().getProgressService().registerIconForFamily(
        SearchPluginImages.DESC_VIEW_SEARCHRES,
        FAMILY_SEARCH);
  }

  public void addQuery(ISearchQuery query) {
    if (query == null) {
      throw new IllegalArgumentException();
    }
    establishHistoryLimit();
    getSearchManager().addQuery(query);
  }

  public void addQueryListener(IQueryListener l) {
    getSearchManager().addQueryListener(l);
  }

  public void cancelSearch(ISearchQuery job) {
    SearchJobRecord rec = fSearchJobs.get(job);
    if (rec != null && rec.job != null) {
      rec.job.cancel();
    }
  }

  public PositionTracker getPositionTracker() {
    return fPositionTracker;
  }

  public ISearchQuery[] getQueries() {
    return getSearchManager().getQueries();
  }

  public QueryManager getSearchManager() {
    return fSearchResultsManager;
  }

  public ISearchResultViewPart getSearchView() {
    return getSearchViewManager().getActiveSearchView();
  }

  public SearchViewManager getSearchViewManager() {
    return fSearchViewManager;
  }

  public boolean isQueryRunning(ISearchQuery query) {
    SearchJobRecord sjr = fSearchJobs.get(query);
    return sjr != null && sjr.isRunning;
  }

  public void removeAllQueries() {
    for (Iterator<ISearchQuery> queries = fSearchJobs.keySet().iterator(); queries.hasNext();) {
      ISearchQuery query = queries.next();
      cancelSearch(query);
    }
    fSearchJobs.clear();
    getSearchManager().removeAll();
  }

  public void removeQuery(ISearchQuery query) {
    if (query == null) {
      throw new IllegalArgumentException();
    }
    cancelSearch(query);
    getSearchManager().removeQuery(query);
    fSearchJobs.remove(query);
  }

  public void removeQueryListener(IQueryListener l) {
    getSearchManager().removeQueryListener(l);
  }

  public boolean runSearchInBackground(ISearchQuery query, ISearchResultViewPart view) {
    if (isQueryRunning(query)) {
      return false;
    }

    // prepare view
    if (view == null) {
      getSearchViewManager().activateSearchView(true);
    } else {
      getSearchViewManager().activateSearchView(view);
    }

    addQuery(query);

    SearchJobRecord sjr = new SearchJobRecord(query);
    fSearchJobs.put(query, sjr);

    Job job = new InternalSearchJob(sjr);
    job.setPriority(Job.BUILD);
    job.setUser(true);

    IWorkbenchSiteProgressService service = getProgressService(view);
    if (service != null) {
      service.schedule(job, 0, true);
    } else {
      job.schedule();
    }

    return true;
  }

  public IStatus runSearchInForeground(IRunnableContext context, final ISearchQuery query,
      ISearchResultViewPart view) {
    if (isQueryRunning(query)) {
      return Status.CANCEL_STATUS;
    }

    // prepare view
    if (view == null) {
      getSearchViewManager().activateSearchView(true);
    } else {
      getSearchViewManager().activateSearchView(view);
    }

    addQuery(query);

    SearchJobRecord sjr = new SearchJobRecord(query);
    fSearchJobs.put(query, sjr);

    if (context == null) {
      context = new ProgressMonitorDialog(null);
    }

    return doRunSearchInForeground(sjr, context);
  }

  public void showSearchResult(SearchView searchView, ISearchResult result, boolean openInNew) {
    if (openInNew) {
      SearchView newPart = (SearchView) getSearchViewManager().activateSearchView(true);
      showSearchResult(newPart, result);

    } else {
      showSearchResult(searchView, result);
    }
  }

  private IStatus doRunSearchInForeground(final SearchJobRecord rec, IRunnableContext context) {
    try {
      context.run(true, true, new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
          searchJobStarted(rec);
          try {
            IStatus status = rec.query.run(monitor);
            if (status.matches(IStatus.CANCEL)) {
              throw new InterruptedException();
            }
            if (!status.isOK()) {
              throw new InvocationTargetException(new CoreException(status));
            }
          } catch (OperationCanceledException e) {
            throw new InterruptedException();
          } finally {
            searchJobFinished(rec);
          }
        }
      });
    } catch (InvocationTargetException e) {
      Throwable innerException = e.getTargetException();
      if (innerException instanceof CoreException) {
        return ((CoreException) innerException).getStatus();
      }
      return new Status(
          IStatus.ERROR,
          SearchPlugin.getID(),
          0,
          SearchMessages.InternalSearchUI_error_unexpected,
          innerException);
    } catch (InterruptedException e) {
      return Status.CANCEL_STATUS;
    }
    return Status.OK_STATUS;
  }

  private void doShutdown() {
    Iterator<SearchJobRecord> jobRecs = fSearchJobs.values().iterator();
    while (jobRecs.hasNext()) {
      SearchJobRecord element = jobRecs.next();
      if (element.job != null) {
        element.job.cancel();
      }
    }
    fPositionTracker.dispose();

    fSearchViewManager.dispose(fSearchResultsManager);

  }

  private void establishHistoryLimit() {
    int historyLimit = SearchPreferencePage.getHistoryLimit();
    QueryManager searchManager = getSearchManager();
    if (historyLimit >= searchManager.getSize()) {
      return;
    }
    int numberQueriesNotShown = 0;
    SearchViewManager searchViewManager = getSearchViewManager();
    ISearchQuery[] queries = searchManager.getQueries();
    for (int i = 0; i < queries.length; i++) {
      ISearchQuery query = queries[i];
      if (!searchViewManager.isShown(query)) {
        if (++numberQueriesNotShown >= historyLimit) {
          removeQuery(query);
        }
      }
    }
  }

  private IWorkbenchSiteProgressService getProgressService(ISearchResultViewPart view) {
    if (view != null) {
      IWorkbenchPartSite site = view.getSite();
      if (site != null) {
        return (IWorkbenchSiteProgressService) view.getSite().getAdapter(
            IWorkbenchSiteProgressService.class);
      }
    }
    return null;
  }

  private void searchJobFinished(SearchJobRecord record) {
    record.isRunning = false;
    fSearchJobs.remove(record);
    getSearchManager().queryFinished(record.query);
  }

  private void searchJobStarted(SearchJobRecord record) {
    record.isRunning = true;
    getSearchManager().queryStarting(record.query);
  }

  private void showSearchResult(SearchView searchView, ISearchResult result) {
    getSearchManager().touch(result.getQuery());
    searchView.showSearchResult(result);
  }

}
