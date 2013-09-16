/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.search;

import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.IEditorMatchAdapter;
import com.google.dart.tools.search.ui.text.IFileMatchAdapter;
import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.wst.sse.ui.internal.editor.EditorPluginImageHelper;
import org.eclipse.wst.sse.ui.internal.editor.EditorPluginImages;

/**
 * @author pavery
 */
public class OccurrencesSearchResult extends AbstractTextSearchResult implements
    IEditorMatchAdapter, IFileMatchAdapter {

  private ISearchQuery fQuery = null;
  private final Match[] NO_MATCHES = new Match[0];

  public OccurrencesSearchResult(ISearchQuery query) {
    this.fQuery = query;
  }

  /**
   * @see org.eclipse.search.ui.text.IEditorMatchAdapter#computeContainedMatches(org.eclipse.search.ui.text.AbstractTextSearchResult,
   *      org.eclipse.ui.IEditorPart)
   */
  public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {

    IEditorInput editorInput = editor.getEditorInput();
    if (editorInput instanceof IFileEditorInput) {
      IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
      return computeContainedMatches(result, fileEditorInput.getFile());
    }
    return this.NO_MATCHES;
  }

  /**
   * @see org.eclipse.search.ui.text.IFileMatchAdapter#computeContainedMatches(org.eclipse.search.ui.text.AbstractTextSearchResult,
   *      org.eclipse.core.resources.IFile)
   */
  public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
    Match[] matches = this.getMatches();
    Match[] containedMatches = new Match[0];

    /*
     * only contains matches if the file for one of the matches is the same as the given file. Note:
     * all matches in a result are related to the same file
     */
    if (matches.length > 0 && matches[0].getElement() instanceof BasicSearchMatchElement
        && ((BasicSearchMatchElement) matches[0].getElement()).getFile().equals(file)) {

      containedMatches = matches;
    }
    return containedMatches;
  }

  /**
   * @see org.eclipse.search.ui.text.AbstractTextSearchResult#getEditorMatchAdapter()
   */
  public IEditorMatchAdapter getEditorMatchAdapter() {
    return this;
  }

  /**
   * @see org.eclipse.search.ui.text.IFileMatchAdapter#getFile(java.lang.Object)
   */
  public IFile getFile(Object element) {
    // return the file for the match
    IFile file = null;
    //System.out.println("get file for:"+element);
    if (element instanceof IMarker) {
      IResource r = ((IMarker) element).getResource();
      if (r instanceof IFile) {
        file = (IFile) r;
      }
    }
    return file;
  }

  public IFileMatchAdapter getFileMatchAdapter() {
    return this;
  }

  public ImageDescriptor getImageDescriptor() {
    return EditorPluginImageHelper.getInstance().getImageDescriptor(
        EditorPluginImages.IMG_OBJ_OCC_MATCH);
  }

  /**
   * This label shows up in the search history
   */
  public String getLabel() {
    return getQuery().getLabel();
  }

  /**
   * @return the matches associated with this result
   */
  public Match[] getMatches() {
    return collectMatches(getElements());
  }

  public ISearchQuery getQuery() {
    return this.fQuery;
  }

  public String getTooltip() {
    return getLabel();
  }

  /**
   * @see org.eclipse.search.ui.text.IEditorMatchAdapter#isShownInEditor(org.eclipse.search.ui.text.Match,
   *      org.eclipse.ui.IEditorPart)
   */
  public boolean isShownInEditor(Match match, IEditorPart editor) {
    return true;
  }

  /**
   * <p>
   * Taken from {@link org.eclipse.jdt.internal.ui.search.OccurrencesSearchResult#collectMatches}
   * </p>
   * 
   * @param elements get the matches for these elements
   * @return the matches associated with this result
   */
  private Match[] collectMatches(Object[] elements) {
    Match[] matches = new Match[getMatchCount()];
    int writeIndex = 0;
    for (int i = 0; i < elements.length; i++) {
      Match[] perElement = getMatches(elements[i]);
      for (int j = 0; j < perElement.length; j++) {
        matches[writeIndex++] = perElement[j];
      }
    }
    return matches;
  }
}
