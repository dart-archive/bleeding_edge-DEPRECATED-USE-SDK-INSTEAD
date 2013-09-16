/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search.dialogs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.core.search.scope.SearchScope;

public interface IComponentSearchListProvider {
  void populateComponentList(IComponentList list, SearchScope scope, IProgressMonitor pm);
}
