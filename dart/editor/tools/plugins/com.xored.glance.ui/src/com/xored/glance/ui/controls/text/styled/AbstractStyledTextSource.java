/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.text.styled;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;

import com.xored.glance.ui.sources.BaseTextSource;
import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextSourceListener;
import com.xored.glance.ui.sources.Match;
import com.xored.glance.ui.sources.SourceSelection;

/**
 * @author Yuri Strot
 */
public abstract class AbstractStyledTextSource extends BaseTextSource implements SelectionListener {

  public AbstractStyledTextSource(final StyledText text) {
    this.text = text;
    blocks = new StyledTextBlock[] {createTextBlock()};
    list = new ListenerList();
  }

  protected StyledTextBlock createTextBlock() {
    return new StyledTextBlock(text);
  }

  public final void dispose() {
    if (text != null && !text.isDisposed() && !disposed) {
      doDispose();
    }
    disposed = true;
  }

  protected void doDispose() {
    focusKeeper.dispose();
    text.removeSelectionListener(this);
  }

  public boolean isDisposed() {
    return disposed;
  }

  public ITextBlock[] getBlocks() {
    return blocks;
  }

  public void addTextSourceListener(final ITextSourceListener listener) {
    list.add(listener);
  }

  public void removeTextSourceListener(final ITextSourceListener listener) {
    list.remove(listener);
  }

  public void widgetDefaultSelected(final SelectionEvent e) {
    fireSelectionChanged();
  }

  public void widgetSelected(final SelectionEvent e) {
    fireSelectionChanged();
  }

  private void fireSelectionChanged() {
    final SourceSelection selection = getSelection();
    final Object[] objects = list.getListeners();
    for (final Object object : objects) {
      final ITextSourceListener listener = (ITextSourceListener) object;
      listener.selectionChanged(selection);
    }
  }

  public SourceSelection getSelection() {
    final Point point = text.getSelection();
    final SourceSelection selection = new SourceSelection(blocks[0], point.x, point.y - point.x);
    return selection;
  }

  public void select(final Match match) {
    this.selected = match;
    focusKeeper.setMatch(match);
  }

  protected StyledText getText() {
    return text;
  }

  @Override
  public void init() {
    focusKeeper = new StyledTextSelector(text);
    text.addSelectionListener(this);
  }

  private StyledTextSelector focusKeeper;
  private final StyledText text;

  private boolean disposed;
  private final ListenerList list;
  private final StyledTextBlock[] blocks;
  protected Match selected;
}
