/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentoutline;

import java.util.Collection;

public interface IJFaceNodeAdapterFactory {
  public void addListener(Object listener);

  /**
   * returns "copy" so no one can modify our list. Its is a shallow copy.
   */
  public Collection getListeners();

  public void removeListener(Object listener);
}
