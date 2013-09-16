/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.tabletree;

import org.eclipse.ui.IActionBars;
import org.eclipse.wst.sse.ui.internal.ISourceViewerActionBarContributor;

public interface IDesignViewerActionBarContributor extends ISourceViewerActionBarContributor {
  public void initViewerSpecificContributions(IActionBars bars);
}
