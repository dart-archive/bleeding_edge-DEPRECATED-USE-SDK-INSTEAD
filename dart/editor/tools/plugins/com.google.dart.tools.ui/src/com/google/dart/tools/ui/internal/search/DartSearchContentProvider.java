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

import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Dart search result page content provider.
 */
public abstract class DartSearchContentProvider implements IStructuredContentProvider {

  protected final static Object[] EMPTY_ARRAY = new Object[0];

  private final DartSearchResultPage page;
  private AbstractTextSearchResult result;

  DartSearchContentProvider(DartSearchResultPage page) {
    this.page = page;
  }

  public abstract void clear();

  @Override
  public void dispose() {
    // nothing to do
  }

  public abstract void elementsChanged(Object[] updatedElements);

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    initialize((AbstractTextSearchResult) newInput);
  }

  protected void initialize(AbstractTextSearchResult result) {
    this.result = result;
  }

  DartSearchResultPage getPage() {
    return page;
  }

  AbstractTextSearchResult getSearchResult() {
    return result;
  }

}
