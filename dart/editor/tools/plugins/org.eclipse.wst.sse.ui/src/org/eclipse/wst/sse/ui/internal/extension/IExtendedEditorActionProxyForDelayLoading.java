/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.extension;

import org.eclipse.jface.action.IAction;

/**
 * @deprecated
 */
public interface IExtendedEditorActionProxyForDelayLoading {
  public IAction getAction();

  public boolean isRealized();

  public void realize();

  boolean isBundleActive();
}
