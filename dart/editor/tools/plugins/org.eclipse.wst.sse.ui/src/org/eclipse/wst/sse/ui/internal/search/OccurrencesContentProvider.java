/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.search;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

/**
 * @author pavery
 */
public class OccurrencesContentProvider implements IStructuredContentProvider {

  protected final Object[] EMPTY_ARRAY = new Object[0];
  private OccurrencesSearchResult fResult = null;
  private TableViewer fTableViewer = null;

  public void clear() {
    if (this.fTableViewer != null)
      this.fTableViewer.refresh();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.IContentProvider#dispose()
   */
  public void dispose() {
    // do nothing
  }

  public void elementsChanged(Object[] updatedElements) {

    //TODO: copied from JavaSearchTableContentProvider
    for (int i = 0; i < updatedElements.length; i++) {
      if (this.fResult.getMatchCount(updatedElements[i]) > 0) {
        if (this.fTableViewer.testFindItem(updatedElements[i]) != null)
          this.fTableViewer.refresh(updatedElements[i]);
        else
          this.fTableViewer.add(updatedElements[i]);
      } else {
        this.fTableViewer.remove(updatedElements[i]);
      }
    }
  }

  /**
   * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
   */
  public Object[] getElements(Object inputElement) {

    this.fResult = (OccurrencesSearchResult) inputElement;
    return this.fResult.getElements();
  }

  /**
   * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
   *      java.lang.Object, java.lang.Object)
   */
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

    if (viewer instanceof TableViewer)
      this.fTableViewer = (TableViewer) viewer;
    this.fResult = (OccurrencesSearchResult) newInput;
  }

  public void refresh() {

    if (this.fTableViewer != null)
      this.fTableViewer.refresh();
  }
}
