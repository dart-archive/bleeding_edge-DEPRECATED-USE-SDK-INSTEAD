/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.core.model.DartElement;

import org.eclipse.jface.viewers.StyledString;

public class DartElementLabelComposer {

  /**
   * Creates a new dart element composer based on the given buffer.
   * 
   * @param buffer the buffer
   */
  public DartElementLabelComposer(StyledString result) {

  }

  /**
   * Appends the label for a Dart element with the flags as defined by this class.
   * 
   * @param element the element to render
   * @param flags the rendering flags.
   */
  public void appendElementLabel(DartElement element, long flags) {
    //TODO (pquitslund): compose an element label
  }

}
