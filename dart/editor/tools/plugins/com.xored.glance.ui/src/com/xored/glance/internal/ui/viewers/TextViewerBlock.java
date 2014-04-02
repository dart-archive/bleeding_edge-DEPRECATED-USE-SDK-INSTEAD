/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.internal.ui.viewers;

import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextBlockListener;
import com.xored.glance.ui.sources.TextChangedEvent;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextViewer;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author Yuri Strot
 */
public class TextViewerBlock implements ITextBlock, ITextListener {

  private ListenerList listeners;

  protected TextViewer viewer;

  public TextViewerBlock(TextViewer viewer) {
    this.viewer = viewer;
    listeners = new ListenerList();
    addListeners();
  }

  @Override
  public void addTextBlockListener(ITextBlockListener listener) {
    listeners.add(listener);
  }

  @Override
  public int compareTo(ITextBlock o) {
    return 0;
  }

  public void dispose() {
    removeListeners();
  }

  @Override
  public String getText() {
    return viewer.getDocument().get();
  }

  @Override
  public void removeTextBlockListener(ITextBlockListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void textChanged(TextEvent event) {
    if (event.getDocumentEvent() != null) {
      TextChangedEvent changedEvent = new TextChangedEvent(
          event.getOffset(),
          event.getLength(),
          event.getReplacedText());
      fireTextChanged(changedEvent);
    }
  }

  protected void addListeners() {
    if (!addFirstListener()) {
      viewer.addTextListener(this);
    }
  }

  protected void fireTextChanged(TextChangedEvent changedEvent) {
    Object[] objects = listeners.getListeners();
    for (Object object : objects) {
      ITextBlockListener listener = (ITextBlockListener) object;
      listener.textChanged(changedEvent);
    }
  }

  protected void removeListeners() {
    viewer.removeTextListener(this);
  }

  /**
   * Add our text listener to the beginning of the listener list thro reflection. This method return
   * true if this operation succeed and fail otherwise.
   */
  private boolean addFirstListener() {
    try {
      Field filed = TextViewer.class.getDeclaredField("fTextListeners");
      filed.setAccessible(true);
      @SuppressWarnings("unchecked")
      List<Object> list = (List<Object>) filed.get(viewer);
      if (list == null) {
        // no listeners, can add it usual way
        return false;
      }
      if (!list.contains(this)) {
        list.add(0, this);
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

}
