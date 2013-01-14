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

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IStructuredSelection;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DartElementSelection extends DartTextSelection implements IStructuredSelection {

  public DartElementSelection(DartEditor editor, DartElement element, IDocument document,
      int offset, int length) {
    super(editor, element, document, offset, length);
  }

  @Override
  public DartElement getFirstElement() {
    DartElement[] elements = toArray();
    if (elements.length > 0) {
      return elements[0];
    }
    return null;
  }

  @Override
  public Iterator<?> iterator() {
    return toList().iterator();
  }

  @Override
  public int size() {
    return toArray().length;
  }

  @Override
  public DartElement[] toArray() {
    try {
      DartElement[] nodes = resolveElementAtOffset();
      if (nodes == null) {
        // TODO(scheglov): Delete this test if not needed.
        nodes = new DartElement[0];
      }
      return nodes;
    } catch (DartModelException ex) {
      return new DartElement[0];
    }
  }

  @Override
  public List<?> toList() {
    return Arrays.asList(toArray());
  }

}
