/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search;

import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.IEditorMatchAdapter;
import com.google.dart.tools.search.ui.text.IFileMatchAdapter;
import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.wst.common.ui.internal.UIPlugin;

public class SearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter,
    IFileMatchAdapter {
  private final Match[] EMPTY_ARR = new Match[0];

  private AbstractSearchQuery fQuery;

  public SearchResult(AbstractSearchQuery job) {
    fQuery = job;
  }

  @Override
  public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
    IEditorInput ei = editor.getEditorInput();
    if (ei instanceof IFileEditorInput) {
      IFileEditorInput fi = (IFileEditorInput) ei;
      return getMatches(fi.getFile());
    }
    return EMPTY_ARR;
  }

  @Override
  public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
    return getMatches(file);
  }

  @Override
  public IEditorMatchAdapter getEditorMatchAdapter() {
    return this;
  }

  @Override
  public IFile getFile(Object element) {
    if (element instanceof IFile) {
      return (IFile) element;
    }
    return null;
  }

  @Override
  public IFileMatchAdapter getFileMatchAdapter() {
    return this;
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return UIPlugin.getDefault().getImageDescriptor("icons/tsearch_dpdn_obj.gif");
  }

  @Override
  public String getLabel() {
    return fQuery.getResultLabel(getMatchCount());
  }

  @Override
  public ISearchQuery getQuery() {
    return fQuery;
  }

  @Override
  public String getTooltip() {
    return getLabel();
  }

  @Override
  public boolean isShownInEditor(Match match, IEditorPart editor) {
    IEditorInput ei = editor.getEditorInput();
    if (ei instanceof IFileEditorInput) {
      IFileEditorInput fi = (IFileEditorInput) ei;
      return match.getElement().equals(fi.getFile());
    }
    return false;
  }
}
