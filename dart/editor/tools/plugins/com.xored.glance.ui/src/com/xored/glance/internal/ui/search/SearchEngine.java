/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.internal.ui.search;

import com.xored.glance.internal.ui.search.SearchJob.ISearchMonitor;
import com.xored.glance.ui.sources.ConfigurationManager;
import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextSource;
import com.xored.glance.ui.sources.Match;

import java.util.regex.Matcher;

/**
 * @author Yuri Strot
 */
public class SearchEngine extends Thread {

  private class SearchScope extends AbstractSearchScope implements ISearchMonitor {

    private boolean firstFound = false;

    /**
     * @param source
     */
    public SearchScope(ITextSource source) {
      super(source);
    }

    @Override
    public void added(SearchScopeEntry entry, Match match) {
      if (!firstFound) {
        if (ConfigurationManager.getInstance().incremenstalSearch()) {
          select(match);
        }
        listener.firstFound(match);
        firstFound = true;
      }
    }

    @Override
    public void blocksChanged(ITextBlock[] removed, ITextBlock[] added) {
      synchronized (monitor) {
        super.blocksChanged(removed, added);
        cancel = true;
        interrupt();
      }
    }

    @Override
    public void blocksReplaced(ITextBlock[] newBlocks) {
      synchronized (monitor) {
        super.blocksReplaced(newBlocks);
        cancel = true;
        interrupt();
      }
    }

    @Override
    public void cleared(SearchScopeEntry entry) {
      synchronized (monitor) {
        cancel = true;
        interrupt();
      }
    }

    public SearchJob getJob() {
      synchronized (monitor) {
        while (currentEntry < entries.size()) {
          SearchJob job = (SearchJob) entries.get(currentEntry);
          if (!job.isFinished()) {
            return job;
          }
          currentEntry++;
        }
        return updateEntry();
      }
    }

    @Override
    public Match[] getMatches() {
      synchronized (monitor) {
        return super.getMatches();
      }
    }

    @Override
    public boolean isCanceled() {
      synchronized (monitor) {
        return cancel;
      }
    }

    @Override
    public void select(Match match) {
      if (match != null) {
        super.select(match);
        listener.setMatchIndex(match.getIndex());
      }
    }

    public void updateResult() {
      if (!firstFound) {
        listener.firstFound(null);
        firstFound = true;
      }
      listener.allFound(getMatches());
    }

    @Override
    protected SearchScopeEntry createEntry(ITextBlock block) {
      return new SearchJob(block, matcher, this);
    }

    protected void updateMatcher(boolean findFirst) {
      firstFound = !findFirst;
      for (SearchScopeEntry entry : entries) {
        SearchJob job = (SearchJob) entry;
        job.update(matcher);
      }
      updateStart();
    }

    private SearchJob updateEntry() {
      currentEntry = 0;
      for (SearchScopeEntry entry : entries) {
        SearchJob job = (SearchJob) entry;
        if (!job.isFinished()) {
          return job;
        }
        currentEntry++;
      }
      currentEntry = 0;
      return null;
    }
  }

  private static final long PAUSE = 100 * 5;// 100 s

  private boolean cancel;

  private SearchRule rule;

  private Matcher matcher;

  private Object monitor = new Object();

  private boolean exit;

  private boolean paused;

  private SearchScope scope;

  private ISearchListener listener;

  public SearchEngine(ISearchListener listener) {
    this.listener = listener;
  }

  public void exit() {
    synchronized (monitor) {
      exit = true;
      interrupt();
    }
  }

  @Override
  public void run() {
    while (!exit) {
      sleepWhileInterrupted(0);
      if (scope == null) {
        continue;
      }
      if (exit) {
        break;
      }
      while (true) {
        long start = System.currentTimeMillis();
        if (!findMatches()) {
          continue;
        }
        long waitPause = paused ? PAUSE - (System.currentTimeMillis() - start) : 0;
        if (waitPause > 0) {
          sleepWhileInterrupted(waitPause);
        }
        if (!scope.isCanceled()) {
          break;
        }
      }
      listener.finished();
      scope.showMatches();
    }
  }

  public void selectNext() {
    if (scope != null) {
      scope.selectNext();
    }
  }

  public void selectPrev() {
    if (scope != null) {
      scope.selectPrev();
    }
  }

  public void setRule(SearchRule rule) {
    synchronized (monitor) {
      doSetRule(rule);
    }
  }

  public void setSource(SearchRule rule, ITextSource source, boolean paused) {
    synchronized (monitor) {
      if (scope != null) {
        scope.dispose();
        scope = null;
      }
      scope = new SearchScope(source);
      this.paused = paused;
      doSetRule(rule);
    }
  }

  public void updateSourceSelection() {
    scope.updateSourceSelection();
  }

  protected void doSetRule(SearchRule rule) {
    cancel = true;
    boolean findFirst = false;
    if (this.rule == null || !this.rule.equals(rule)) {
      this.rule = rule;
      findFirst = true;
      matcher = rule.getText().length() == 0 ? null : rule.getPattern().matcher(new String());
    }
    if (scope != null) {
      scope.updateMatcher(findFirst);
    }
    interrupt();
  }

  private boolean findMatches() {
    cancel = false;
    if (matcher == null) {
      scope.showEmptyText();
      return true;
    }
    SearchJob job = null;
    while ((job = scope.getJob()) != null) {
      if (!job.run()) {
        return false;
      }
    }
    scope.updateResult();
    return true;
  }

  private void sleepWhileInterrupted(long millis) {
    try {
      if (millis == 0) {
        while (true) {
          Thread.sleep(50);
        }
      } else {
        Thread.sleep(millis);
      }
    } catch (InterruptedException e1) {
    }
  }

}
