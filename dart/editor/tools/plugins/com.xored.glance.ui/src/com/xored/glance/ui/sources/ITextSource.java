/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.ui.sources;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Yuri Strot
 */
public interface ITextSource {

  /**
   * Add text source listener
   * 
   * @param listener text source listener
   */
  public void addTextSourceListener(ITextSourceListener listener);

  public void dispose();

  /**
   * Return text blocks associated with this source
   * 
   * @return text blocks
   */
  public ITextBlock[] getBlocks();

  /**
   * Return current source selection. This selection using to identify where start search
   * 
   * @return source selection
   */
  public SourceSelection getSelection();

  /**
   * @param monitor
   */
  public void index(IProgressMonitor monitor);

  /**
   * Called before search started
   */
  public void init();

  public boolean isDisposed();

  public boolean isIndexRequired();

  /**
   * Remove text source listener
   * 
   * @param listener text source listener
   */
  public void removeTextSourceListener(ITextSourceListener listener);

  /**
   * Focus match
   * 
   * @param match match to focus
   */
  public void select(Match match);

  /**
   * Highlight matches
   * 
   * @param matches
   */
  public void show(Match[] matches);

  public void updateSourceSelection();

}
