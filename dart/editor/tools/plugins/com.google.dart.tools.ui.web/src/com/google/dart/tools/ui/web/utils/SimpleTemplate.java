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

package com.google.dart.tools.ui.web.utils;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;

/**
 * A simple template class for use with IContentAssistProcessors.
 * <p>
 * Ex. div$gt;${}$lt;/div$gt;
 * <p>
 * Where ${} indicates where the cursor should be placed.
 */
public class SimpleTemplate {
  private String name;
  private int cursorPos;
  private String replacementText;

  public SimpleTemplate(String name, String pattern) {
    this.name = name;

    cursorPos = pattern.length();
    replacementText = pattern;

    int index = pattern.indexOf("${}");

    if (index != -1) {
      cursorPos = index;
      replacementText = pattern.substring(0, index) + pattern.substring(index + "${}".length());
    }
  }

  public ICompletionProposal createCompletion(String prefix, int offset, Image image) {
    return new CompletionProposal(
        getReplacementText(),
        offset - prefix.length(),
        prefix.length(),
        cursorPosition(),
        image,
        name,
        null,
        null);
  }

  public int cursorPosition() {
    return cursorPos;
  }

  public String getName() {
    return name;
  }

  public String getReplacementText() {
    return replacementText;
  }

  public boolean matches(String prefix) {
    return name.startsWith(prefix);
  }

}
