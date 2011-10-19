/*
 * Copyright (c) 2011, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.search.internal.ui.text;

import com.google.dart.tools.search.internal.ui.SearchPluginImages;
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


public class FileSearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter,
    IFileMatchAdapter {
  private final Match[] EMPTY_ARR = new Match[0];

  private FileSearchQuery fQuery;

  public FileSearchResult(FileSearchQuery job) {
    fQuery = job;
  }

  public ImageDescriptor getImageDescriptor() {
    return SearchPluginImages.DESC_OBJ_TSEARCH_DPDN;
  }

  public String getLabel() {
    return fQuery.getResultLabel(getMatchCount());
  }

  public String getTooltip() {
    return getLabel();
  }

  public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
    return getMatches(file);
  }

  public IFile getFile(Object element) {
    if (element instanceof IFile)
      return (IFile) element;
    return null;
  }

  public boolean isShownInEditor(Match match, IEditorPart editor) {
    IEditorInput ei = editor.getEditorInput();
    if (ei instanceof IFileEditorInput) {
      IFileEditorInput fi = (IFileEditorInput) ei;
      return match.getElement().equals(fi.getFile());
    }
    return false;
  }

  public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
    IEditorInput ei = editor.getEditorInput();
    if (ei instanceof IFileEditorInput) {
      IFileEditorInput fi = (IFileEditorInput) ei;
      return getMatches(fi.getFile());
    }
    return EMPTY_ARR;
  }

  public ISearchQuery getQuery() {
    return fQuery;
  }

  public IFileMatchAdapter getFileMatchAdapter() {
    return this;
  }

  public IEditorMatchAdapter getEditorMatchAdapter() {
    return this;
  }
}
