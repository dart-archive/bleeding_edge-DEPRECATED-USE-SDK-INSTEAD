/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.decor;

import org.eclipse.core.runtime.IProgressMonitor;

import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextSourceListener;

public interface IStructContent {

  public void index(IProgressMonitor monitor);

  public ITextBlock[] getBlocks();

  public ITextBlock getContent(StructCell cell);

  public IPath getPath(ITextBlock block);

  public void dispose();

  public void addListener(ITextSourceListener listener);

  public void removeListener(ITextSourceListener listener);

  public ITextSourceListener[] getListeners();

}
