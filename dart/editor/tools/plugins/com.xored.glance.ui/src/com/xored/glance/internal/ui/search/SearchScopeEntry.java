/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.internal.ui.search;

import java.util.ArrayList;
import java.util.List;

import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextBlockListener;
import com.xored.glance.ui.sources.Match;
import com.xored.glance.ui.sources.TextChangedEvent;

/**
 * @author Yuri Strot
 */
public class SearchScopeEntry implements ITextBlockListener, Comparable<SearchScopeEntry> {

  public SearchScopeEntry(ITextBlock block, IMatchListener listener) {
    this.block = block;
    this.listener = listener;
    init();
  }

  public void setStart(int start) {
    synchronized (MONITOR) {
      this.start = start;
    }
  }

  public int getStart() {
    synchronized (MONITOR) {
      return start;
    }
  }

  protected void addMatch(Match match) {
    synchronized (MONITOR) {
      matches.add(nextMatchIndex, match);
      nextMatchIndex++;
      listener.added(this, match);
    }
  }

  public void dispose() {
    block.removeTextBlockListener(this);
  }

  interface IMatchListener {

    public void added(SearchScopeEntry entry, Match match);

    public void cleared(SearchScopeEntry entry);
  }

  public void textChanged(TextChangedEvent event) {
    clear();
    listener.cleared(this);
  }

  public int compareTo(SearchScopeEntry entry) {
    return block.compareTo(entry.block);
  }

  protected void init() {
    clear();
    block.addTextBlockListener(this);
  }

  protected void clear() {
    synchronized (MONITOR) {
      doClear();
    }
  }

  protected void doClear() {
    matches = new ArrayList<Match>();
    text = block.getText();
    nextMatchIndex = 0;
  }

  protected void addMatchToBegin() {
    synchronized (MONITOR) {
      nextMatchIndex = 0;
    }
  }

  /**
   * @return the text
   */
  public String getText() {
    synchronized (MONITOR) {
      return text;
    }
  }

  /**
   * @return the matches
   */
  public List<Match> getMatches() {
    synchronized (MONITOR) {
      return matches;
    }
  }

  /**
   * @return the block
   */
  public ITextBlock getBlock() {
    return block;
  }

  /**
   * @return the listener
   */
  public IMatchListener getListener() {
    return listener;
  }

  private Object MONITOR = new Object();
  private IMatchListener listener;
  private String text;
  private ITextBlock block;
  private int start;
  private int nextMatchIndex;
  private List<Match> matches;

}
