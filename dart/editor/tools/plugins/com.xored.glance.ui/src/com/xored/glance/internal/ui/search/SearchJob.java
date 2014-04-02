/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.internal.ui.search;

import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.Match;

import java.util.regex.Matcher;

/**
 * @author Yuri Strot
 */
public class SearchJob extends SearchScopeEntry {

  interface ISearchMonitor extends IMatchListener {

    boolean isCanceled();

  }

  private boolean finished;

  private Matcher matcher;

  public SearchJob(ITextBlock block, Matcher matcher, ISearchMonitor monitor) {
    super(block, monitor);
    update(matcher);
  }

  /**
   * @return the finished
   */
  public boolean isFinished() {
    return finished;
  }

  public boolean run() {
    if (matcher == null) {
      return false;
    }
    matcher.reset(getText());
    int from = getStart();
    if (!find(from, getText().length())) {
      return false;
    }
    addMatchToBegin();
    if (!find(0, from - 1)) {
      return false;
    }
    finished = true;
    setStart(0);
    return true;
  }

  public void update(Matcher matcher) {
    this.matcher = matcher;
    clear();
  }

  @Override
  protected void doClear() {
    super.doClear();
    finished = false;
  }

  private Match find(int from) {
    try {
      if (matcher.find(from)) {
        int start = matcher.start();
        int end = matcher.end();
        if (end != start) { // don't report 0-length matches
          return new Match(getBlock(), start, end - start);
        }
      }
    } catch (Exception e) {
      // It can be an exception while we matching.
      // So return if exception occured
    }
    return null;
  }

  private boolean find(int from, int to) {
    int k = 1;
    int limit = getText().length();
    if (from >= to || from > limit) {
      return true;
    }
    Match match = find(from);
    if (getMonitor().isCanceled()) {
      return false;
    }
    if (match != null) {
      from = match.getOffset() + 1;
      if (from > to || from > limit) {
        return true;
      }
      addMatch(match);
      match = find(from);
      while ((match = find(from)) != null) {
        if (match.getOffset() >= to) {
          return true;
        }
        addMatch(match);
        if (k++ == 20) {
          if (getMonitor().isCanceled()) {
            return false;
          }
          k = 0;
        }
        from = match.getOffset() + 1;
      }
    }
    return true;
  }

  private ISearchMonitor getMonitor() {
    return (ISearchMonitor) getListener();
  }

}
