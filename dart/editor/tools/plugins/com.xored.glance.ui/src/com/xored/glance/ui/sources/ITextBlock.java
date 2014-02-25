/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.ui.sources;

/**
 * @author Yuri Strot
 */
public interface ITextBlock extends Comparable<ITextBlock> {

  public void addTextBlockListener(ITextBlockListener listener);

  public String getText();

  public void removeTextBlockListener(ITextBlockListener listener);

}
