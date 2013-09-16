/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.search;

import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.ui.internal.Logger;

/**
 * Defines a basic search query. This query does not do anything, it needs to be extended and the
 * <code>{@link #doQuery()}</code> method needs to be overridden to make the query actually be
 * functional.
 */
public class BasicSearchQuery implements ISearchQuery {
  /**
   * the file we're searching
   */
  private IFile fFile = null;

  /**
   * The result of this query
   */
  private AbstractTextSearchResult fResult = null;

  /**
   * The progress monitor for the query
   */
  private IProgressMonitor fProgressMonitor = null;

  /**
   * <p>
   * Construct a new basic query.
   * </p>
   * <p>
   * <b>IMPORTANT: </b>It is very important that after creating the query and <b>before</b> running
   * the query that you call {@link #setResult(AbstractTextSearchResult)}. This is not a parameter
   * because typically a search result needs a reference to its query and thus the query needs to be
   * constructed before its result object can be set.
   * </p>
   * 
   * @param file the file this query will take place on
   */
  public BasicSearchQuery(IFile file) {
    this.fFile = file;
  }

  /**
   * Adds a match to the results of this query.
   * <p>
   * <b>IMPORTANT: </b>It is very important that after creating the query and <b>before</b> running
   * the query that you call {@link #setResult(AbstractTextSearchResult)}. This is not a parameter
   * because typically a search result needs a reference to its query and thus the query needs to be
   * constructed before its result object can be set.
   * </p>
   * 
   * @param document the document the match is being added too
   * @param matchStart the start character of the match
   * @param matchEnd the end character of the match
   */
  public void addMatch(IDocument document, int matchStart, int matchEnd) {

    try {
      int lineNumber = document.getLineOfOffset(matchStart);
      int lineStart = document.getLineOffset(lineNumber);
      int lineLength = document.getLineLength(lineNumber);

      String elementMessage = document.get().substring(lineStart, lineStart + lineLength);

      //add the match to the result
      BasicSearchMatchElement element = new BasicSearchMatchElement(fFile, lineNumber, lineStart,
          elementMessage);
      fResult.addMatch(new Match(element, Match.UNIT_CHARACTER, matchStart, matchEnd - matchStart));

    } catch (BadLocationException e) {
      Logger.logException(e);
    }
  }

  /**
   * <p>
   * <i>Note: </i> as of yet no testing has gone into whether this query can be re-run or not or
   * what that even entails.
   * <p>
   * 
   * @see org.eclipse.search.ui.ISearchQuery#canRerun()
   */
  @Override
  public boolean canRerun() {
    return false;
  }

  /**
   * <p>
   * This query can be run in the background
   * </p>
   * 
   * @see org.eclipse.search.ui.ISearchQuery#canRunInBackground()
   */
  @Override
  public boolean canRunInBackground() {
    return true;
  }

  /**
   * public to avoid synthetic method access from inner class
   * 
   * @return
   */
  public IFile getFile() {
    return this.fFile;
  }

  /**
   * There is no default label for a basic query, this should be overridden by implementing classes
   * 
   * @see org.eclipse.search.ui.ISearchQuery#getLabel()
   */
  @Override
  public String getLabel() {
    return ""; //$NON-NLS-1$
  }

  /**
   * <p>
   * This will be <code>null</code> if {@link #setResult(AbstractTextSearchResult)} has not been
   * called yet.
   * </p>
   * 
   * @see org.eclipse.search.ui.ISearchQuery#getSearchResult()
   */
  @Override
  public ISearchResult getSearchResult() {
    return fResult;
  }

  /**
   * Runs the query
   * 
   * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public IStatus run(IProgressMonitor monitor) {
    fProgressMonitor = monitor;
    return doQuery();
  }

  /**
   * <p>
   * The actual work of the query, called by {@link #run(IProgressMonitor)}
   * </p>
   * <p>
   * <i>Note: </i>This method should be overridden by implementers so that their query will actually
   * do something
   * </p>
   * 
   * @return the status of the query when it has finished
   * @deprecated use {@link #doQuery(IProgressMonitor)} so that the operation is cancelable
   */
  @Deprecated
  protected IStatus doQuery() {
    return doQuery(fProgressMonitor);
  }

  /**
   * <p>
   * The actual work of the query, called by {@link #run(IProgressMonitor)}
   * </p>
   * <p>
   * <i>Note: </i>This method should be overridden by implementers so that their query will actually
   * do something
   * </p>
   * 
   * @param monitor {@link IProgressMonitor} used to track progress and cancel the operation
   * @return the status of the query when it has finished
   */
  protected IStatus doQuery(IProgressMonitor monitor) {
    return Status.OK_STATUS;
  }

  /**
   * @return the total number of matches this query found
   */
  protected int getMatchCount() {
    return fResult.getMatchCount();
  }

  /**
   * <p>
   * used in search result display labels, should be overridden by implementers
   * </p>
   * 
   * @return
   */
  protected String getSearchText() {
    return ""; //$NON-NLS-1$
  }

  /**
   * <p>
   * This <b>needs</b> to be called after constructing the query but before running it, see note on
   * {@link #BasicSearchQuery(IFile)}
   * </p>
   * 
   * @param result the result this query will use to store its results
   */
  protected void setResult(AbstractTextSearchResult result) {
    this.fResult = result;
  }
}
