/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.ui.swtbot.matchers;

import org.eclipse.ui.IEditorReference;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * Matches an editor with a specified title
 */
public class EditorWithTitle extends BaseMatcher<IEditorReference> {
  private final String title;

  public EditorWithTitle(String title) {
    this.title = title;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("Editor " + title);
  }

  @Override
  public boolean matches(Object item) {
    if (item instanceof IEditorReference) {
      return title.equals(((IEditorReference) item).getTitle());
    }
    return false;
  }
}
