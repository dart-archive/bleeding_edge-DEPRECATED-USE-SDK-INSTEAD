/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Derived from org.eclipse.search.internal.ui.IFileSearchContentProvider
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search.basecode;

public interface IFileSearchContentProvider {

  public abstract void elementsChanged(Object[] updatedElements);

  public abstract void clear();

}
