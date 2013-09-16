/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;

public interface IComponentDescriptionProvider {
  boolean isApplicable(Object component);

  String getQualifier(Object component);

  String getName(Object component);

  IFile getFile(Object component);

  Image getFileIcon(Object component);

  ILabelProvider getLabelProvider();
}
