/*
 * Copyright 2011 Google Inc.
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

import com.google.dart.tools.search.ui.actions.TextSearchAction;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.omni.OmniBoxImages;
import com.google.dart.tools.ui.omni.OmniBoxMessages;
import com.google.dart.tools.ui.omni.OmniElement;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Element for text searches.
 */
public class TextSearchElement extends OmniElement {

  private String searchText;

  public TextSearchElement(TextSearchProvider provider) {
    super(provider);
  }

  @Override
  public void execute() {
    new TextSearchAction(((TextSearchProvider) getProvider()).getShell(), searchText).run();
  }

  @Override
  public String getId() {
    return getProvider().getId() + "." + getSearchText(); //$NON-NLS-1$
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return OmniBoxImages.getFileSearchImageDescriptor();
  }

  @Override
  public String getLabel() {
    //cache the value shown in the label so we can use it to execute the search
    searchText = getSearchText();
    return Messages.format(OmniBoxMessages.TextSearchElement_occurences, searchText);
  }

  private String getSearchText() {
    return ((TextSearchProvider) getProvider()).getSearchText();
  }

}
