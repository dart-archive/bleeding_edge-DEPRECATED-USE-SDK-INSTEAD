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
package com.google.dart.tools.ui.internal.text.folding;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.preferences.OverlayPreferenceStore;
import com.google.dart.tools.ui.internal.preferences.OverlayPreferenceStore.OverlayKey;
import com.google.dart.tools.ui.text.folding.IDartFoldingPreferenceBlock;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Java default folding preferences.
 */
public class DefaultJavaFoldingPreferenceBlock implements IDartFoldingPreferenceBlock {

  private IPreferenceStore fStore;
  private OverlayPreferenceStore fOverlayStore;
  private OverlayKey[] fKeys;
  private Map<Button, String> fCheckBoxes = new HashMap<Button, String>();
  private SelectionListener fCheckBoxListener = new SelectionListener() {
    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
      Button button = (Button) e.widget;
      fOverlayStore.setValue(fCheckBoxes.get(button), button.getSelection());
    }
  };

  public DefaultJavaFoldingPreferenceBlock() {
    fStore = DartToolsPlugin.getDefault().getPreferenceStore();
    fKeys = createKeys();
    fOverlayStore = new OverlayPreferenceStore(fStore, fKeys);
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.folding.IJavaFoldingPreferences#
   * createControl(org.eclipse.swt.widgets.Group)
   */
  @Override
  public Control createControl(Composite composite) {
    fOverlayStore.load();
    fOverlayStore.start();

    Composite inner = new Composite(composite, SWT.NONE);
    GridLayout layout = new GridLayout(1, true);
    layout.verticalSpacing = 3;
    layout.marginWidth = 0;
    inner.setLayout(layout);

    Label label = new Label(inner, SWT.LEFT);
    label.setText(FoldingMessages.DefaultJavaFoldingPreferenceBlock_title);

    addCheckBox(inner, FoldingMessages.DefaultJavaFoldingPreferenceBlock_comments,
        PreferenceConstants.EDITOR_FOLDING_JAVADOC, 0);
    addCheckBox(inner, FoldingMessages.DefaultJavaFoldingPreferenceBlock_headers,
        PreferenceConstants.EDITOR_FOLDING_HEADERS, 0);
//		addCheckBox(inner, FoldingMessages.DefaultJavaFoldingPreferenceBlock_innerTypes, PreferenceConstants.EDITOR_FOLDING_INNERTYPES, 0);
    addCheckBox(inner, FoldingMessages.DefaultJavaFoldingPreferenceBlock_methods,
        PreferenceConstants.EDITOR_FOLDING_METHODS, 0);
//		addCheckBox(inner, FoldingMessages.DefaultJavaFoldingPreferenceBlock_imports, PreferenceConstants.EDITOR_FOLDING_IMPORTS, 0);

    return inner;
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.folding.AbstractJavaFoldingPreferences #dispose()
   */
  @Override
  public void dispose() {
    fOverlayStore.stop();
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.folding.AbstractJavaFoldingPreferences
   * #initialize()
   */
  @Override
  public void initialize() {
    initializeFields();
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.folding.AbstractJavaFoldingPreferences
   * #performDefaults()
   */
  @Override
  public void performDefaults() {
    fOverlayStore.loadDefaults();
    initializeFields();
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.folding.AbstractJavaFoldingPreferences #performOk()
   */
  @Override
  public void performOk() {
    fOverlayStore.propagate();
  }

  private Button addCheckBox(Composite parent, String label, String key, int indentation) {
    Button checkBox = new Button(parent, SWT.CHECK);
    checkBox.setText(label);

    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gd.horizontalIndent = indentation;
    gd.horizontalSpan = 1;
    gd.grabExcessVerticalSpace = false;
    checkBox.setLayoutData(gd);
    checkBox.addSelectionListener(fCheckBoxListener);

    fCheckBoxes.put(checkBox, key);

    return checkBox;
  }

  private OverlayKey[] createKeys() {
    ArrayList<OverlayKey> overlayKeys = new ArrayList<OverlayKey>();

    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN,
        PreferenceConstants.EDITOR_FOLDING_JAVADOC));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_INNERTYPES));
    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN,
        PreferenceConstants.EDITOR_FOLDING_METHODS));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_IMPORTS));
    overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN,
        PreferenceConstants.EDITOR_FOLDING_HEADERS));

    return overlayKeys.toArray(new OverlayKey[overlayKeys.size()]);
  }

  private void initializeFields() {
    Iterator<Button> it = fCheckBoxes.keySet().iterator();
    while (it.hasNext()) {
      Button b = it.next();
      String key = fCheckBoxes.get(b);
      b.setSelection(fOverlayStore.getBoolean(key));
    }
  }
}
