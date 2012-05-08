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
package com.google.dart.tools.search.internal.ui.text;

import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.ui.IQueryListener;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;

public class SearchResultUpdater implements IResourceChangeListener, IQueryListener {
  private AbstractTextSearchResult fResult;

  public SearchResultUpdater(AbstractTextSearchResult result) {
    fResult = result;
    NewSearchUI.addQueryListener(this);
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
  }

  public void resourceChanged(IResourceChangeEvent event) {
    IResourceDelta delta = event.getDelta();
    if (delta != null)
      handleDelta(delta);
  }

  private void handleDelta(IResourceDelta d) {
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
      SearchPlugin.log(e);
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
