/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;

import java.text.MessageFormat;

/**
 * Basic ISearchQuery that finds matches of region type and region text.
 * 
 * @author pavery
 */
public class OccurrencesSearchQuery extends BasicSearchQuery {

  /**
   * We need a runnable so that the search markers show up in the live document.
   */
  private class FindRegions implements IWorkspaceRunnable {

    private IStructuredDocument fFindRegionsDocument = null;
    private String fMatchName = null;
    private String fMatchRegionType = null;

    public FindRegions(IStructuredDocument document, String matchText, String matchRegionType) {

      this.fFindRegionsDocument = document;
      this.fMatchName = matchText;
      this.fMatchRegionType = matchRegionType;
    }

    private void findOccurrences(IProgressMonitor monitor) {

      if (!isCanceled(monitor)) {

        int matchStart = -1;
        int matchEnd = -1;
        String findRegionText = ""; //$NON-NLS-1$

        ITextRegion r = null;
        ITextRegionList regions = null;
        IStructuredDocumentRegion current = this.fFindRegionsDocument.getFirstStructuredDocumentRegion();

        // this is the main loop that iterates the document
        while (current != null && !isCanceled(monitor)) {
          regions = current.getRegions();
          for (int i = 0; i < regions.size() && !isCanceled(monitor); i++) {

            r = regions.get(i);

            // maybe this is the equals check where some valid
            // matches are failing (like searching on end tag)
            if (r.getType().equals(this.fMatchRegionType)
                && current.getText(r).equals(this.fMatchName)) {

              findRegionText = current.getText(r);

              // region found
              matchStart = current.getStartOffset(r);
              matchEnd = matchStart + findRegionText.trim().length();

              addMatch(this.fFindRegionsDocument, matchStart, matchEnd);
            }
          }
          current = current.getNext();
        }
      }
    }

    private boolean isCanceled(IProgressMonitor monitor) {
      return monitor != null && monitor.isCanceled();
    }

    public void run(IProgressMonitor monitor) throws CoreException {

      try {
        findOccurrences(monitor);
      } catch (Exception e) {
        Logger.logException(e);
      }
    }
  }// end inner class FindRegions

  private IStructuredDocument fDocument = null;
  private String fRegionText = null;
  private String fRegionType = null;

  public OccurrencesSearchQuery(IFile file, IStructuredDocument document, String regionText,
      String regionType) {
    super(file);
    super.setResult(new OccurrencesSearchResult(this));
    this.fDocument = document;
    this.fRegionText = regionText;
    this.fRegionType = regionType;
  }

  /**
   * <p>
   * <i>Note: </i>Some investigation needs to be put into how to do this safely
   * </p>
   * 
   * @see org.eclipse.wst.sse.ui.internal.search.BasicSearchQuery#canRunInBackground()
   */
  public boolean canRunInBackground() {
    return false;
  }

  /**
   * <p>
   * The label format is:<br/>
   * searchText - # occurrences in file
   * </p>
   * 
   * @see org.eclipse.wst.sse.ui.internal.search.BasicSearchQuery#getLabel()
   */
  public String getLabel() {
    String label = SSEUIMessages.OccurrencesSearchQuery_0; //$NON-NLS-1$
    String[] args = {getSearchText(), "" + super.getMatchCount(), getFilename()};
    return MessageFormat.format(label, args);
  }

  /**
   * This query looks for all occurrences of the selected string
   * 
   * @see org.eclipse.wst.sse.ui.internal.search.BasicSearchQuery#doQuery()
   */
  protected IStatus doQuery(IProgressMonitor monitor) {
    IStatus status = Status.OK_STATUS;
    FindRegions findRegions = new FindRegions(this.fDocument, this.fRegionText, this.fRegionType);
    try {
      // BUG158846 - deadlock if lock up entire workspace, so only lock
      // up the file we are searching on
      ISchedulingRule markerRule = ResourcesPlugin.getWorkspace().getRuleFactory().markerRule(
          getFile());
      ResourcesPlugin.getWorkspace().run(findRegions, markerRule, IWorkspace.AVOID_UPDATE, monitor);
    } catch (CoreException e) {
      status = new Status(IStatus.ERROR, SSEUIPlugin.ID, IStatus.OK, "", null); //$NON-NLS-1$
    }
    return status;
  }

  protected String getSearchText() {
    return this.fRegionText;
  }

  private String getFilename() {
    String filename = SSEUIMessages.OccurrencesSearchQuery_2; //$NON-NLS-1$ "file"
    if (getFile() != null)
      filename = getFile().getName().toString();
    return filename;
  }
}
