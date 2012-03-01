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

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.callhierarchy.CallLocation;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

class LocationLabelProvider extends LabelProvider implements ITableLabelProvider {
  private static final int COLUMN_ICON = 0;
  private static final int COLUMN_LINE = 1;
  private static final int COLUMN_INFO = 2;

  LocationLabelProvider() {
    // Do nothing
  }

  @Override
  public Image getColumnImage(Object element, int columnIndex) {
    if (columnIndex == COLUMN_ICON) {
      return DartPluginImages.get(DartPluginImages.IMG_OBJS_SEARCH_OCCURRENCE);
    }
    return null;
  }

  @Override
  public String getColumnText(Object element, int columnIndex) {
    if (element instanceof CallLocation) {
      CallLocation callLocation = (CallLocation) element;

      switch (columnIndex) {
        case COLUMN_LINE:
          int lineNumber = callLocation.getLineNumber();
          if (lineNumber == CallLocation.UNKNOWN_LINE_NUMBER) {
            return CallHierarchyMessages.LocationLabelProvider_unknown;
          } else {
            return String.valueOf(lineNumber);
          }
        case COLUMN_INFO:
          return removeWhitespaceOutsideStringLiterals(callLocation);
      }
    }

    return ""; //$NON-NLS-1$
  }

  @Override
  public Image getImage(Object element) {
    return getColumnImage(element, COLUMN_ICON);
  }

  @Override
  public String getText(Object element) {
    return getColumnText(element, COLUMN_INFO);
  }

  private String removeWhitespaceOutsideStringLiterals(CallLocation callLocation) {
    StringBuffer buf = new StringBuffer();
    boolean withinString = false;

    String s = callLocation.getCallText();
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);

      if (ch == '"') {
        withinString = !withinString;
      }

      if (withinString) {
        buf.append(ch);
      } else if (Character.isWhitespace(ch)) {
        if ((buf.length() == 0) || !Character.isWhitespace(buf.charAt(buf.length() - 1))) {
          if (ch != ' ') {
            ch = ' ';
          }

          buf.append(ch);
        }
      } else {
        buf.append(ch);
      }
    }

    return buf.toString();
  }
}
