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

import com.google.dart.tools.search.ui.IQueryListener;
import com.google.dart.tools.search.ui.ISearchQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class QueryManager {
  private List<ISearchQuery> fQueries;
  private List<IQueryListener> fListeners;

  public QueryManager() {
    super();
    // an ArrayList should be plenty fast enough (few searches).
    fListeners = new ArrayList<IQueryListener>();
    fQueries = new LinkedList<ISearchQuery>();
  }

  public boolean hasQueries() {
    synchronized (this) {
      return !fQueries.isEmpty();
    }
  }

  public int getSize() {
    synchronized (this) {
      return fQueries.size();
    }
  }

  /**
   * Returns the queries in LRU order. Smaller index means more recently used.
   * 
   * @return all queries
   */
  public ISearchQuery[] getQueries() {
    synchronized (this) {
      return fQueries.toArray(new ISearchQuery[fQueries.size()]);
    }
  }

  public void removeQuery(ISearchQuery query) {
    synchronized (this) {
      fQueries.remove(query);
    }
    fireRemoved(query);
  }

  public void addQuery(ISearchQuery query) {
    synchronized (this) {
      if (fQueries.contains(query))
        return;
      fQueries.add(0, query);
    }
    fireAdded(query);
  }

  public void addQueryListener(IQueryListener l) {
    synchronized (fListeners) {
      fListeners.add(l);
    }
  }

  public void removeQueryListener(IQueryListener l) {
    synchronized (fListeners) {
      fListeners.remove(l);
    }
  }

  public void fireAdded(ISearchQuery query) {
    Set<IQueryListener> copiedListeners = new HashSet<IQueryListener>();
    synchronized (fListeners) {
      copiedListeners.addAll(fListeners);
    }
    Iterator<IQueryListener> listeners = copiedListeners.iterator();
    while (listeners.hasNext()) {
      IQueryListener l = listeners.next();
      l.queryAdded(query);
    }
  }

  public void fireRemoved(ISearchQuery query) {
    Set<IQueryListener> copiedListeners = new HashSet<IQueryListener>();
    synchronized (fListeners) {
      copiedListeners.addAll(fListeners);
    }
    Iterator<IQueryListener> listeners = copiedListeners.iterator();
    while (listeners.hasNext()) {
      IQueryListener l = listeners.next();
      l.queryRemoved(query);
    }
  }

  public void fireStarting(ISearchQuery query) {
    Set<IQueryListener> copiedListeners = new HashSet<IQueryListener>();
    synchronized (fListeners) {
      copiedListeners.addAll(fListeners);
    }
    Iterator<IQueryListener> listeners = copiedListeners.iterator();
    while (listeners.hasNext()) {
      IQueryListener l = listeners.next();
      l.queryStarting(query);
    }
  }

  public void fireFinished(ISearchQuery query) {
    Set<IQueryListener> copiedListeners = new HashSet<IQueryListener>();
    synchronized (fListeners) {
      copiedListeners.addAll(fListeners);
    }
    Iterator<IQueryListener> listeners = copiedListeners.iterator();
    while (listeners.hasNext()) {
      IQueryListener l = listeners.next();
      l.queryFinished(query);
    }
  }

  public void removeAll() {
    synchronized (this) {
      List<ISearchQuery> old = fQueries;
      fQueries = new LinkedList<ISearchQuery>();
      Iterator<ISearchQuery> iter = old.iterator();
      while (iter.hasNext()) {
        ISearchQuery element = iter.next();
        fireRemoved(element);
      }
    }
  }

  public void queryFinished(ISearchQuery query) {
    fireFinished(query);
  }

  public void queryStarting(ISearchQuery query) {
    fireStarting(query);
  }

  public void touch(ISearchQuery query) {
    synchronized (this) {
      if (fQueries.contains(query)) {
        fQueries.remove(query);
        fQueries.add(0, query);
      }
    }
  }

}
