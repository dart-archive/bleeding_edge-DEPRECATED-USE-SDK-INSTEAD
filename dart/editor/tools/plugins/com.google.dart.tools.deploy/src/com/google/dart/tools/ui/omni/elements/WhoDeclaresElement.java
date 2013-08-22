/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.tools.search.internal.ui.FindDeclarationsAction;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.omni.OmniBoxImages;
import com.google.dart.tools.ui.omni.OmniBoxMessages;
import com.google.dart.tools.ui.omni.OmniElement;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Search for all declarations of elements with specified name.
 */
public class WhoDeclaresElement extends OmniElement {

  /**
   * An executed text search (restored from a memento). Notably, executed searches do not update
   * their search filter text.
   */
  static final class Memento extends WhoDeclaresElement {

    public Memento(TextSearchProvider provider, String searchText) {
      super(provider);
      this.searchText = searchText;
    }

    @Override
    public OmniElement getMemento() {
      return this;
    }

    @Override
    protected String getSearchText() {
      return searchText;
    }

    @Override
    protected void updateSearchText() {
      //do not update cache
    }
  }

  protected String searchText;

  public WhoDeclaresElement(TextSearchProvider provider) {
    super(provider);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof WhoDeclaresElement)) {
      return false;
    }
    WhoDeclaresElement other = (WhoDeclaresElement) obj;
    if (this.searchText == other.searchText) {
      return true;
    }
    if (this.searchText != null) {
      return this.searchText.equals(other.searchText);
    }
    return false;
  }

  @Override
  public String getId() {
    return getProvider().getId() + ".whoDeclares." + getSearchText(); //$NON-NLS-1$
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return OmniBoxImages.getFileSearchImageDescriptor();
  }

  @Override
  public String getLabel() {
    // cache the value shown in the label so we can use it to execute the search
    updateSearchText();
    return Messages.format(OmniBoxMessages.WhoDeclaresElement_label, searchText);
  }

  @Override
  public String getMatchText() {
    return getSearchText();
  }

  @Override
  public int getMatchTextOffset() {
    return OmniBoxMessages.WhoDeclaresElement_label.indexOf('{') - 1;
  }

  @Override
  public OmniElement getMemento() {
    return new Memento((TextSearchProvider) getProvider(), searchText);
  }

  @Override
  public int hashCode() {
    int hash = 13;
    if (searchText != null) {
      hash += searchText.hashCode();
    }
    return hash;
  }

  @Override
  protected void doExecute(String text, UIInstrumentationBuilder instrumentation) {
    FindDeclarationsAction.doSearch(searchText);
  }

  protected String getSearchText() {
    return ((TextSearchProvider) getProvider()).getSearchText();
  }

  protected void updateSearchText() {
    searchText = getSearchText();
  }
}
