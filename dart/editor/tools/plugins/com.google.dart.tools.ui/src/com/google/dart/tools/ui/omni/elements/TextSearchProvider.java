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

package com.google.dart.tools.ui.omni.elements;

import com.google.dart.tools.ui.omni.OmniBoxMessages;
import com.google.dart.tools.ui.omni.OmniBoxPopup;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.swt.widgets.Shell;

/**
 * Provider for text search elements.
 */
public class TextSearchProvider extends OmniProposalProvider {

  private final OmniBoxPopup omniBoxPopup;

  private static final String ID = TextSearchProvider.class.getName();
  private static final int TEXT_INDEX = ID.length() + 1;

  public TextSearchProvider(OmniBoxPopup omniBoxPopup) {
    this.omniBoxPopup = omniBoxPopup;
  }

  @Override
  public OmniElement getElementForId(String id) {
    if (id == null || (id.length() <= TEXT_INDEX)) {
      return null;
    }
    //strip id leader to get the search pattern
    id = id.substring(TEXT_INDEX);
    return new TextSearchElement.Memento(this, id);
  }

  @Override
  public OmniElement[] getElements(String pattern) {
    return new TextSearchElement[] {new TextSearchElement(this)};
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return OmniBoxMessages.TextSearchProvider_label;
  }

  public String getSearchText() {
    return omniBoxPopup.getFilterTextExactCase();
  }

  public Shell getShell() {
    return omniBoxPopup.getShell();
  }

}
