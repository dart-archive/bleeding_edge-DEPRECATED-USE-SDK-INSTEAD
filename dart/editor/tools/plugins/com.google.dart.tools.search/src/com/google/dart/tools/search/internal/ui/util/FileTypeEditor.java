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
package com.google.dart.tools.search.internal.ui.util;

import com.google.dart.tools.search.internal.ui.SearchMessages;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.dialogs.TypeFilteringDialog;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class FileTypeEditor extends SelectionAdapter implements DisposeListener {

  public static String typesToString(String[] types) {
    Arrays.sort(types, FILE_TYPES_COMPARATOR);
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < types.length; i++) {
      if (i > 0) {
        result.append(TYPE_DELIMITER);
        result.append(" "); //$NON-NLS-1$
      }
      result.append(types[i]);
    }
    return result.toString();
  }

  private Combo fTextField;

  private Button fBrowseButton;
  private final static String TYPE_DELIMITER = SearchMessages.FileTypeEditor_typeDelimiter;

  public final static String FILE_PATTERN_NEGATOR = "!"; //$NON-NLS-1$

  private static final Comparator<String> FILE_TYPES_COMPARATOR = new Comparator<String>() {
    @Override
    public int compare(String fp1, String fp2) {
      boolean isNegative1 = fp1.startsWith(FILE_PATTERN_NEGATOR);
      boolean isNegative2 = fp2.startsWith(FILE_PATTERN_NEGATOR);
      if (isNegative1 != isNegative2) {
        return isNegative1 ? 1 : -1;
      }
      return fp1.compareTo(fp2);
    }
  };

  public FileTypeEditor(Combo textField, Button browseButton) {
    fTextField = textField;
    fBrowseButton = browseButton;

    fTextField.addDisposeListener(this);
    fBrowseButton.addDisposeListener(this);
    fBrowseButton.addSelectionListener(this);
  }

  public String[] getFileTypes() {
    Set<String> result = new HashSet<String>();
    StringTokenizer tokenizer = new StringTokenizer(fTextField.getText(), TYPE_DELIMITER);

    while (tokenizer.hasMoreTokens()) {
      String currentExtension = tokenizer.nextToken().trim();
      result.add(currentExtension);
    }
    return result.toArray(new String[result.size()]);
  }

  public void setFileTypes(String[] types) {
    fTextField.setText(typesToString(types));
  }

  @Override
  public void widgetDisposed(DisposeEvent event) {
    Widget widget = event.widget;
    if (widget == fTextField) {
      fTextField = null;
    } else if (widget == fBrowseButton) {
      fBrowseButton = null;
    }
  }

  @Override
  public void widgetSelected(SelectionEvent event) {
    if (event.widget == fBrowseButton) {
      handleBrowseButton();
    }
  }

  protected void handleBrowseButton() {
    TypeFilteringDialog dialog = new TypeFilteringDialog(
        fTextField.getShell(),
        Arrays.asList(getFileTypes()));
    if (dialog.open() == Window.OK) {
      Object[] result = dialog.getResult();
      HashSet<String> patterns = new HashSet<String>();
      boolean starIncluded = false;
      for (int i = 0; i < result.length; i++) {
        String curr = result[i].toString();
        if (curr.equals("*")) { //$NON-NLS-1$
          starIncluded = true;
        } else {
          patterns.add("*." + curr); //$NON-NLS-1$
        }
      }
      if (patterns.isEmpty() && starIncluded) { // remove star when other file extensions active
        patterns.add("*"); //$NON-NLS-1$
      }
      String[] filePatterns = patterns.toArray(new String[patterns.size()]);
      Arrays.sort(filePatterns);
      setFileTypes(filePatterns);
    }
  }
}
