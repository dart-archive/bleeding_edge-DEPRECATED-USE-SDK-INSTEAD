/*******************************************************************************
 * Copyright (c) 2001, 2004 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.search;

import com.google.dart.tools.search.ui.text.AbstractTextSearchViewPage;
import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.sse.ui.internal.Logger;

/**
 * Base page for Occurrences in file search results.
 * 
 * @author pavery
 */
public class OccurrencesSearchViewPage extends AbstractTextSearchViewPage {

  private OccurrencesContentProvider fContentProvider = null;

  public OccurrencesSearchViewPage() {
    super(AbstractTextSearchViewPage.FLAG_LAYOUT_FLAT);
  }

  /**
   * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#clear()
   */
  protected void clear() {
    if (this.fContentProvider != null)
      this.fContentProvider.clear();
  }

  /**
   * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTableViewer(org.eclipse.jface.viewers.TableViewer)
   */
  protected void configureTableViewer(TableViewer viewer) {

    //sort results by line number, low to high
    viewer.setComparator(new ViewerComparator() {
      public int compare(Viewer v, Object obj1, Object obj2) {
        BasicSearchMatchElement elem1 = (BasicSearchMatchElement) obj1;
        BasicSearchMatchElement elem2 = (BasicSearchMatchElement) obj2;
        return elem1.getLineNum() - elem2.getLineNum();
      }
    });

    //allow for formated labels
    viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
        new BasicSearchLabelProvider(this)));
    this.fContentProvider = new OccurrencesContentProvider();
    viewer.setContentProvider(this.fContentProvider);
  }

  /**
   * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTreeViewer(org.eclipse.jface.viewers.TreeViewer)
   */
  protected void configureTreeViewer(TreeViewer viewer) {
    // not supported at the moment
  }

  /**
   * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#elementsChanged(java.lang.Object[])
   */
  protected void elementsChanged(Object[] objects) {
    if (this.fContentProvider != null) {
      this.fContentProvider.elementsChanged(objects);
    }
  }

  public void forceRefresh() {
    this.fContentProvider.refresh();
  }

  private IWorkbenchPage getActivePage() {

    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    if (window == null)
      return null;
    return workbench.getActiveWorkbenchWindow().getActivePage();
  }

  /**
   * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#showMatch(org.eclipse.search.ui.text.Match,
   *      int, int)
   */
  protected void showMatch(Match match, int currentOffset, int currentLength, boolean activate)
      throws PartInitException {
    BasicSearchMatchElement element = (BasicSearchMatchElement) match.getElement();

    IWorkbenchPage activePage = getActivePage();
    try {
      if (activePage != null) {
        // open editor if needed
        IDE.openEditor(getActivePage(), element.getFile());
        //set the selection in the open editor
        IEditorPart editor = activePage.getActiveEditor();
        if (activate)
          editor.getSite().getPage().activate(editor);
        editor.getEditorSite().getSelectionProvider().setSelection(
            new TextSelection(currentOffset, currentLength));
      }
    } catch (PartInitException e) {
      // possible exception trying to open editor
      Logger.logException(e);
    }
  }
}
