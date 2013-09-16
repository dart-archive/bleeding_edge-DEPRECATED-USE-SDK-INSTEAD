/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search;

import com.google.dart.tools.search.ui.IQueryListener;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.wst.common.core.search.SearchEngine;
import org.eclipse.wst.common.core.search.pattern.QualifiedName;
import org.eclipse.wst.common.core.search.pattern.SearchPattern;
import org.eclipse.wst.common.core.search.scope.SearchScope;
import org.eclipse.wst.common.ui.internal.search.basecode.Messages;

public abstract class AbstractSearchQuery implements ISearchQuery {

  protected String fPattern;
  protected SearchScope fScope;
  protected SearchResult fResult;
  protected String fScopeDescription;

  public AbstractSearchQuery(String pattern, SearchScope scope, String scopeDescription) {
    super();
    fPattern = pattern;
    fScope = scope;
    fScopeDescription = scopeDescription;
  }

  public boolean canRerun() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean canRunInBackground() {
    return true;
  }

  public String getLabel() {
    return SearchMessages.FileSearchQuery_label;
  }

  public ISearchResult getSearchResult() {
    if (fResult == null) {
      fResult = new SearchResult(this);
      new SearchResultUpdater(fResult);
    }
    return fResult;
  }

  public String getResultLabel(int nMatches) {
    if (nMatches == 1) {
      if (fPattern.length() > 0) {
        Object[] args = {fPattern, fScopeDescription};
        return Messages.format(SearchMessages.FileSearchQuery_singularLabel, args);
      }
      Object[] args = {"", fScopeDescription};
      return Messages.format(SearchMessages.FileSearchQuery_singularLabel_fileNameSearch, args);
    }
    if (fPattern.length() > 0) {
      Object[] args = {fPattern, new Integer(nMatches), fScopeDescription}; //$NON-NLS-1$
      return Messages.format(SearchMessages.FileSearchQuery_pluralPattern, args);
    }
    Object[] args = {"", new Integer(nMatches), fScopeDescription}; //$NON-NLS-1$
    return Messages.format(SearchMessages.FileSearchQuery_pluralPattern_fileNameSearch, args);

  }

  public IStatus run(IProgressMonitor pm) throws OperationCanceledException {
    final SearchResult textResult = (SearchResult) getSearchResult();
    textResult.removeAll();
    SearchQueryResultCollector collector = new SearchQueryResultCollector(textResult);
    String searchString = fPattern;
    if (searchString.trim().equals(String.valueOf('*'))) {
      searchString = new String();
    }
    String message = SearchMessages.TextSearchEngine_statusMessage;
    MultiStatus status = new MultiStatus(NewSearchUI.PLUGIN_ID, IStatus.OK, message, null);

    SearchEngine searchEngine = new SearchEngine();

    QualifiedName typeName = QualifiedName.valueOf(searchString);

    try {
      SearchPattern pattern = createSearchPattern(typeName);
      searchEngine.search(pattern, collector, fScope, null, new NullProgressMonitor());
    } catch (CoreException e) {
      status.add(e.getStatus());
    }

    return status;

  }

  protected abstract SearchPattern createSearchPattern(QualifiedName typeName);

  public class SearchResultUpdater implements IResourceChangeListener, IQueryListener {
    SearchResult fResult;

    public SearchResultUpdater(SearchResult result) {
      fResult = result;
      NewSearchUI.addQueryListener(this);
      ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    public void resourceChanged(IResourceChangeEvent event) {
      IResourceDelta delta = event.getDelta();
      if (delta != null)
        handleDelta(delta);
    }

    protected void handleDelta(IResourceDelta d) {
      try {
        d.accept(new IResourceDeltaVisitor() {
          public boolean visit(IResourceDelta delta) throws CoreException {
            switch (delta.getKind()) {
              case IResourceDelta.ADDED:
                return false;
              case IResourceDelta.REMOVED:
                IResource res = delta.getResource();
                if (res instanceof IFile) {
                  Match[] matches = fResult.getMatches(res);
                  fResult.removeMatches(matches);
                }
                break;
              case IResourceDelta.CHANGED:
                // handle changed resource
                break;
            }
            return true;
          }
        });
      } catch (CoreException e) {
        e.printStackTrace();
      }
    }

    public void queryAdded(ISearchQuery query) {
      // don't care
    }

    public void queryRemoved(ISearchQuery query) {
      if (fResult.equals(query.getSearchResult())) {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        NewSearchUI.removeQueryListener(this);
      }
    }

    public void queryStarting(ISearchQuery query) {
      // don't care
    }

    public void queryFinished(ISearchQuery query) {
      // don't care
    }
  }
}
