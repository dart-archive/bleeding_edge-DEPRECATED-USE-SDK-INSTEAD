/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.internal.ui.search;

import com.xored.glance.internal.ui.search.SearchScopeEntry.IMatchListener;
import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextSource;
import com.xored.glance.ui.sources.ITextSourceListener;
import com.xored.glance.ui.sources.Match;
import com.xored.glance.ui.sources.SourceSelection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Yuri Strot
 */
public abstract class AbstractSearchScope implements IMatchListener, ITextSourceListener {

  protected List<SearchScopeEntry> entries;

  protected ITextSource source;

  private SourceSelection selection;

  protected int currentEntry;

  public AbstractSearchScope(ITextSource source) {
    this.source = source;
    init();
  }

  @Override
  public abstract void added(SearchScopeEntry entry, Match match);

  @Override
  public void blocksChanged(ITextBlock[] removed, ITextBlock[] added) {
    for (int i = 0; i < entries.size(); i++) {
      SearchScopeEntry entry = entries.get(i);
      for (ITextBlock block : removed) {
        if (block.equals(entry.getBlock())) {
          entries.remove(i);
          i--;
          break;
        }
      }
    }
    newBlocks(added);
  }

  @Override
  public void blocksReplaced(ITextBlock[] newBlocks) {
    entries = new ArrayList<SearchScopeEntry>(newBlocks.length);
    newBlocks(newBlocks);
  }

  @Override
  public abstract void cleared(SearchScopeEntry entry);

  public void dispose() {
    for (SearchScopeEntry entry : entries) {
      entry.dispose();
    }
    source.removeTextSourceListener(this);
  }

  public Match[] getMatches() {
    List<Match> matches = new ArrayList<Match>();
    for (SearchScopeEntry entry : entries) {
      matches.addAll(entry.getMatches());
    }
    Match[] ms = matches.toArray(new Match[matches.size()]);
    for (int i = 0; i < ms.length; i++) {
      ms[i].setIndex(i + 1);
    }
    return ms;
  }

  @Override
  public void selectionChanged(SourceSelection selection) {
    updateSelection(selection);
  }

  public void selectNext() {
    int index = getSelectedEntryIndex();
    if (index < 0 && entries.size() > 0) {
      index = 0;
    }
    if (index >= 0) {
      int offset = getOffset();
      Match match = findNextMatch(index, entries.size(), offset, Integer.MAX_VALUE);
      if (match == null) {
        match = findNextMatch(0, index, -1, Integer.MAX_VALUE);
        if (match == null) {
          match = findNextMatch(index, index + 1, -1, offset);
        }
      }
      select(match);
    }
  }

  public void selectPrev() {
    int index = getSelectedEntryIndex();
    if (index < 0 && entries.size() > 0) {
      index = 0;
    }
    if (index >= 0) {
      int offset = getOffset();
      Match match = findPrevMatch(index, -1, 0, offset);
      if (match == null) {
        match = findPrevMatch(entries.size() - 1, index, 0, Integer.MAX_VALUE);
        if (match == null) {
          match = findPrevMatch(index, index - 1, offset, Integer.MAX_VALUE);
        }
      }
      select(match);
    }
  }

  public void showEmptyText() {
    source.select(null);
  }

  public void showMatches() {
    source.show(getMatches());
  }

  protected SearchScopeEntry createEntry(ITextBlock block) {
    return new SearchScopeEntry(block, this);
  }

  protected Match findNextMatch(int from, int to, int offsetStart, int offsetEnd) {
    for (int i = from; i < to; i++) {
      SearchScopeEntry entry = entries.get(i);
      for (Match match : entry.getMatches()) {
        if (match.getOffset() > offsetStart && match.getOffset() < offsetEnd) {
          return match;
        }
      }
      offsetStart = -1;
      offsetEnd = Integer.MAX_VALUE;
    }
    return null;
  }

  protected Match findPrevMatch(int from, int to, int offsetStart, int offsetEnd) {
    for (int i = from; i > to; i--) {
      SearchScopeEntry entry = entries.get(i);
      List<Match> matches = entry.getMatches();
      for (int j = matches.size() - 1; j >= 0; j--) {
        Match match = matches.get(j);
        if (match.getOffset() >= offsetStart && match.getOffset() < offsetEnd) {
          return match;
        }
      }
      offsetStart = -1;
      offsetEnd = Integer.MAX_VALUE;
    }
    return null;
  }

  protected int getSelectedEntryIndex() {
    if (selection != null) {
      for (int i = 0; i < entries.size(); i++) {
        SearchScopeEntry entry = entries.get(i);
        ITextBlock block = entry.getBlock();
        if (block.equals(selection.getBlock())) {
          return i;
        }
      }
    }
    return -1;
  }

  protected void init() {
    source.addTextSourceListener(this);
    ITextBlock[] blocks = source.getBlocks();
    entries = new ArrayList<SearchScopeEntry>(blocks.length);
    for (ITextBlock block : blocks) {
      SearchScopeEntry entry = createEntry(block);
      entries.add(entry);
    }
    Collections.sort(entries);
    updateSelection(source.getSelection());
  }

  protected void select(Match match) {
    if (match != null) {
      selection = new SourceSelection(match.getBlock(), match.getOffset(), match.getLength());
      source.select(match);
    }
  }

  protected void updateSourceSelection() {
    source.updateSourceSelection();
    updateSelection(source.getSelection());
  }

  protected void updateStart() {
    int index = getSelectedEntryIndex();
    if (index >= 0) {
      SearchScopeEntry entry = entries.get(index);
      entry.setStart(getOffset());
      currentEntry = index;
    }
  }

  private int getOffset() {
    return selection == null ? 0 : selection.getOffset();
  }

  private void newBlocks(ITextBlock[] newBlocks) {
    for (ITextBlock block : newBlocks) {
      SearchScopeEntry entry = createEntry(block);
      int index = Collections.binarySearch(entries, entry);
      if (index < 0) {
        index = -1 * index - 1;
        entries.add(index, entry);
      }
    }
    // TODO: need to test more without this updates
    // updateSelection(source.getSelection());
  }

  private void updateSelection(SourceSelection selection) {
    if (selection != null && selection.equals(this.selection)) {
      return;
    }
    if (selection == null && entries.size() > 0) {
      ITextBlock block = entries.get(0).getBlock();
      selection = new SourceSelection(block, 0, 0);
    }
    this.selection = selection;
    updateStart();
  }

}
