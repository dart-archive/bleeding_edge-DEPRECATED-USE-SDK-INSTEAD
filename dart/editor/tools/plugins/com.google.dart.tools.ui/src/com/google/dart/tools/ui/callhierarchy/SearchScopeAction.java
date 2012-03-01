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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.core.search.SearchScope;

import org.eclipse.jface.action.Action;

abstract class SearchScopeAction extends Action {
  private final SearchScopeActionGroup fGroup;

  public SearchScopeAction(SearchScopeActionGroup group, String text) {
    super(text, AS_RADIO_BUTTON);
    this.fGroup = group;
  }

  /**
   * Fetches the description of the scope with the appropriate include mask.
   * 
   * @param includeMask the include mask
   * @return the description of the scope with the appropriate include mask
   */
  public abstract String getFullDescription(int includeMask);

  /**
   * Fetches the search scope with the appropriate include mask.
   * 
   * @param includeMask the include mask
   * @return the search scope with the appropriate include mask
   */
  public abstract SearchScope getSearchScope(int includeMask);

  public abstract int getSearchScopeType();

  @Override
  public void run() {
    this.fGroup.setSelected(this, true);
    CallHierarchyViewPart part = this.fGroup.getView();
    part.setInputElements(part.getInputElements());
  }
}
