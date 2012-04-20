/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.search;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.ParentElement;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.IEditorMatchAdapter;
import com.google.dart.tools.search.ui.text.IFileMatchAdapter;
import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IEditorPart;

import java.util.HashSet;
import java.util.Set;

/**
 * An abstract base implementation for Dart element based search results.
 */
public abstract class AbstractDartSearchResult extends AbstractTextSearchResult implements
    IEditorMatchAdapter, IFileMatchAdapter {

  protected static final Match[] NO_MATCHES = new Match[0];

  @Override
  public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
    return computeContainedMatches(editor.getEditorInput());
  }

  @Override
  public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
    return computeContainedMatches(file);
  }

  @Override
  public IEditorMatchAdapter getEditorMatchAdapter() {
    return this;
  }

  @Override
  public IFile getFile(Object element) {
    if (element instanceof DartElement) {
      DartElement dartElement = (DartElement) element;
      CompilationUnit cu = dartElement.getAncestor(CompilationUnit.class);
      if (cu != null) {
        return (IFile) cu.getResource();
      }
      return null;
    }
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
  public boolean isShownInEditor(Match match, IEditorPart editor) {
    Object element = match.getElement();
    if (element instanceof DartElement) {
      element = ((DartElement) element).getOpenable();
      return element != null
          && element.equals(editor.getEditorInput().getAdapter(DartElement.class));
    } else if (element instanceof IFile) {
      return element.equals(editor.getEditorInput().getAdapter(IFile.class));
    }
    return false;
  }

  private void addMatches(Set<Match> matches, Match[] m) {
    if (m.length != 0) {
      for (int i = 0; i < m.length; i++) {
        matches.add(m[i]);
      }
    }
  }

  private void collectMatches(Set<Match> matches, DartElement element) {
    Match[] m = getMatches(element);
    addMatches(matches, m);
    if (element instanceof ParentElement) {
      ParentElement parent = (ParentElement) element;
      try {
        DartElement[] children = parent.getChildren();
        for (int i = 0; i < children.length; i++) {
          collectMatches(matches, children[i]);
        }
      } catch (DartModelException e) {
        // don't track
      }
    }
  }

  private void collectMatches(Set<Match> matches, IFile element) {
    Match[] m = getMatches(element);
    addMatches(matches, m);
  }

  private Match[] computeContainedMatches(IAdaptable adaptable) {
    DartElement dartElement = (DartElement) adaptable.getAdapter(DartElement.class);
    Set<Match> matches = new HashSet<Match>();
    if (dartElement != null) {
      collectMatches(matches, dartElement);
    }
    IFile file = (IFile) adaptable.getAdapter(IFile.class);
    if (file != null) {
      collectMatches(matches, file);
    }
    if (!matches.isEmpty()) {
      return matches.toArray(new Match[matches.size()]);
    }
    return NO_MATCHES;
  }

}
