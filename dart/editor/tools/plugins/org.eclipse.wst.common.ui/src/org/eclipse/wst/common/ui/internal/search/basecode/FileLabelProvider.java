/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Derived from org.eclipse.search.internal.ui.FileLabelProvider
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search.basecode;

import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.AbstractTextSearchViewPage;

import com.ibm.icu.text.MessageFormat;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.wst.common.ui.internal.search.SearchMessages;

public class FileLabelProvider extends LabelProvider {

  public static final int SHOW_LABEL = 1;
  public static final int SHOW_LABEL_PATH = 2;
  public static final int SHOW_PATH_LABEL = 3;
  public static final int SHOW_PATH = 4;

  private static final String fgSeparatorFormat = "{0} - {1}"; //$NON-NLS-1$

  private WorkbenchLabelProvider fLabelProvider;
  private AbstractTextSearchViewPage fPage;

  private int fOrder;
  private String[] fArgs = new String[2];

  public FileLabelProvider(AbstractTextSearchViewPage page, int orderFlag) {
    fLabelProvider = new WorkbenchLabelProvider();
    fOrder = orderFlag;
    fPage = page;
  }

  public void setOrder(int orderFlag) {
    fOrder = orderFlag;
  }

  public int getOrder() {
    return fOrder;
  }

  public String getText(Object element) {
    if (!(element instanceof IResource))
      return null;

    IResource resource = (IResource) element;
    String text = null;

    if (!resource.exists())
      text = SearchMessages.FileLabelProvider_removed_resource_label;

    else {
      IPath path = resource.getFullPath().removeLastSegments(1);
      if (path.getDevice() == null)
        path = path.makeRelative();
      if (fOrder == SHOW_LABEL || fOrder == SHOW_LABEL_PATH) {
        text = fLabelProvider.getText(resource);
        if (path != null && fOrder == SHOW_LABEL_PATH) {
          fArgs[0] = text;
          fArgs[1] = path.toString();
          text = MessageFormat.format(fgSeparatorFormat, fArgs);
        }
      } else {
        if (path != null)
          text = path.toString();
        else
          text = ""; //$NON-NLS-1$
        if (fOrder == SHOW_PATH_LABEL) {
          fArgs[0] = text;
          fArgs[1] = fLabelProvider.getText(resource);
          text = MessageFormat.format(fgSeparatorFormat, fArgs);
        }
      }
    }

    int matchCount = 0;
    AbstractTextSearchResult result = fPage.getInput();
    if (result != null)
      matchCount = result.getMatchCount(element);
    if (matchCount <= 1)
      return text;
    String format = SearchMessages.FileLabelProvider_count_format;
    return MessageFormat.format(format, new Object[] {text, new Integer(matchCount)});
  }

  public Image getImage(Object element) {
    if (!(element instanceof IResource))
      return null;

    IResource resource = (IResource) element;
    Image image = fLabelProvider.getImage(resource);
    return image;
  }

  public void dispose() {
    super.dispose();
    fLabelProvider.dispose();
  }

  public boolean isLabelProperty(Object element, String property) {
    return fLabelProvider.isLabelProperty(element, property);
  }

  public void removeListener(ILabelProviderListener listener) {
    super.removeListener(listener);
    fLabelProvider.removeListener(listener);
  }

  public void addListener(ILabelProviderListener listener) {
    super.addListener(listener);
    fLabelProvider.addListener(listener);
  }
}
