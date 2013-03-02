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
package com.google.dart.tools.core.internal.search.listener;

import com.google.dart.tools.core.search.SearchListener;
import com.google.dart.tools.core.search.SearchMatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class GatheringSearchListener implements SearchListener {
  /**
   * A list containing the matches that have been found so far.
   */
  private final List<SearchMatch> matches = new ArrayList<SearchMatch>();

  private CountDownLatch latch = new CountDownLatch(1);

  /**
   * A flag indicating whether the search is complete.
   */
  private boolean isComplete = false;

  /**
   * Block until the search has finished. This method returns immediately if the serch is already
   * complete.
   * 
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  public void blockUntilComplete() throws InterruptedException {
    latch.await();
  }

  public List<SearchMatch> getMatches() {
    // TODO(devoncarew): consider using a sorted list instead
    Collections.sort(matches, SearchMatch.SORT_BY_ELEMENT_NAME);
    return matches;
  }

  /**
   * Return <code>true</code> if the search is complete.
   * 
   * @return <code>true</code> if the search is complete
   */
  public boolean isComplete() {
    return isComplete;
  }

  @Override
  public void matchFound(SearchMatch match) {
    matches.add(match);
  }

  @Override
  public void searchComplete() {
    isComplete = true;
    latch.countDown();
  }
}
