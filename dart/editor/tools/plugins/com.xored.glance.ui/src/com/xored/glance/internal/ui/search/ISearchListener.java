/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.internal.ui.search;

import com.xored.glance.ui.sources.Match;

/**
 * @author Yuri Strot
 */
public interface ISearchListener {

  public void allFound(Match[] matches);

  public void finished();

  public void firstFound(Match match);

  public void setMatchIndex(int index);

}
