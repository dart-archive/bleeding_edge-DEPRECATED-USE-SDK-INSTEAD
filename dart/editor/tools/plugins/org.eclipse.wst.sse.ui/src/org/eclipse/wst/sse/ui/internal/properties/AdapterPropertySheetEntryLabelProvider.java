/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.properties;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;

public class AdapterPropertySheetEntryLabelProvider extends LabelProvider {
  public Image getImage(Object element) {
    if (element == null)
      return null;
    if (element instanceof IPropertySheetEntry) {
      return ((IPropertySheetEntry) element).getImage();
    }
    if (element instanceof INodeNotifier) {
      IPropertySheetEntry entry = (IPropertySheetEntry) ((INodeNotifier) element).getAdapterFor(IPropertySheetEntry.class);
      if (entry != null)
        return entry.getImage();
    }
    return super.getImage(element);
  }

  public String getText(Object element) {
    if (element == null)
      return null;
    if (element instanceof IPropertySheetEntry) {
      return ((IPropertySheetEntry) element).getValueAsString();
    }
    if (element instanceof INodeNotifier) {
      IPropertySheetEntry entry = (IPropertySheetEntry) ((INodeNotifier) element).getAdapterFor(IPropertySheetEntry.class);
      if (entry != null)
        return entry.getValueAsString();
    }
    return super.getText(element);
  }
}
