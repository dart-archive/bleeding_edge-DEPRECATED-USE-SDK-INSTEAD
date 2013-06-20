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
   * Return text blocks associated with this source
   * 
   * @return text blocks
   */
  public ITextBlock[] getBlocks();

  /**
   * Add text source listener
   * 
   * @param listener text source listener
   */
  public void addTextSourceListener(ITextSourceListener listener);

  /**
   * Remove text source listener
   * 
   * @param listener text source listener
   */
  public void removeTextSourceListener(ITextSourceListener listener);

  /**
   * Return current source selection. This selection using to identify where start search
   * 
   * @return source selection
   */
  public SourceSelection getSelection();

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

  /**
   * Called before search started
   */
  public void init();

  /**
   * @param monitor
   */
  public void index(IProgressMonitor monitor);

  public boolean isIndexRequired();

  public boolean isDisposed();

  public void dispose();

}
