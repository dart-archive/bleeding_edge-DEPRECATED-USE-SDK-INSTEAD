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

package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;

public class DartOutlineElementLabelProvider extends LabelProvider implements IStyledLabelProvider {

  @Override
  public StyledString getStyledText(Object element) {

    return null;
  }

  @Override
  public String getText(Object element) {
    if (element instanceof ConstructorElement) {
      ConstructorElement ce = (ConstructorElement) element;
      if (ce.getName() == null) {
        return ce.getEnclosingElement().getName();
      } else {
        return ce.getEnclosingElement().getName() + '.' + ce.getName();
      }
    } else if (element instanceof Element) {
      Element e = (Element) element;
      return e.getName();
    }
    return "Not_An_Element : " + element.getClass();
  }
}
