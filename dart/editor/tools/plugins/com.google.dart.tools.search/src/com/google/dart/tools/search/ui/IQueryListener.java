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
package com.google.dart.tools.search.ui;

/**
 * <p>
 * A listener for changes to the set of search queries. Queries are added by running them via
 * {@link com.google.dart.tools.search.ui.NewSearchUI#runQueryInBackground(ISearchQuery)
 * NewSearchUI#runQueryInBackground(ISearchQuery)} or
 * {@link com.google.dart.tools.search.ui.NewSearchUI#runQueryInForeground(org.eclipse.jface.operation.IRunnableContext,ISearchQuery)
 * NewSearchUI#runQueryInForeground(IRunnableContext,ISearchQuery)}
 * </p>
 * <p>
 * The search UI determines when queries are rerun, stopped or deleted (and will notify interested
 * parties via this interface). Listeners can be added and removed in the
 * {@link com.google.dart.tools.search.ui.NewSearchUI NewSearchUI} class.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 */
public interface IQueryListener {
  /**
   * Called when an query has been added to the system.
   * 
   * @param query the query that has been added
   */

  void queryAdded(ISearchQuery query);

  /**
   * Called when a query has been removed.
   * 
   * @param query the query that has been removed
   */
  void queryRemoved(ISearchQuery query);

  /**
   * Called before an <code>ISearchQuery</code> is starting.
   * 
   * @param query the query about to start
   */
  void queryStarting(ISearchQuery query);

  /**
   * Called after an <code>ISearchQuery</code> has finished.
   * 
   * @param query the query that has finished
   */
  void queryFinished(ISearchQuery query);
}
