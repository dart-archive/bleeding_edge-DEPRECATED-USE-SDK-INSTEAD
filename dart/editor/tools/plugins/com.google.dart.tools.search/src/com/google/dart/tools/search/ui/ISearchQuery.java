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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Represents a particular search query (in a Java example, a query might be
 * "find all occurrences of 'foo' in workspace"). When its run method is called, the query places
 * any results it finds in the <code>ISearchResult</code> that can be accessed via
 * getSearchResult(). Note that <code>getSearchResult</code> may be called at any time, even before
 * the <code>run()</code> method has been called. An empty search result should be returned in that
 * case.
 * <p>
 * Clients may implement this interface.
 * </p>
 */
public interface ISearchQuery {
  /**
   * This is the method that actually does the work, i.e. finds the results of the search query.
   * 
   * @param monitor the progress monitor to be used
   * @return the status after completion of the search job.
   * @throws OperationCanceledException Thrown when the search query has been canceled.
   */
  IStatus run(IProgressMonitor monitor) throws OperationCanceledException;

  /**
   * Returns a user readable label for this query. This will be used, for example to set the
   * <code>Job</code> name if this query is executed in the background. Note that progress
   * notification (for example, the number of matches found) should be done via the progress monitor
   * passed into the <code>run(IProgressMonitor)</code> method
   * 
   * @return the user readable label of this query
   */
  String getLabel();

  /**
   * Returns whether the query can be run more than once. Some queries may depend on transient
   * information and return <code>false</code>.
   * 
   * @return whether this query can be run more than once
   */
  boolean canRerun();

  /**
   * Returns whether this query can be run in the background. Note that queries must do proper
   * locking when they are run in the background (e.g. get the appropriate workspace locks).
   * 
   * @return whether this query can be run in the background
   */
  boolean canRunInBackground();

  /**
   * Returns the search result associated with this query. This method can be called before run is
   * called.
   * 
   * @return this query's search result
   */
  ISearchResult getSearchResult();
}
