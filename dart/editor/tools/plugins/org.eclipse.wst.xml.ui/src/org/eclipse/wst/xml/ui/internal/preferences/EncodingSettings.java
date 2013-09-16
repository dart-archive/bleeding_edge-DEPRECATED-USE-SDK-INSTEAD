/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.preferences;

import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.sse.core.internal.encoding.CommonCharsetNames;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

/**
 * EncodingSettings is a composite that can be used to display the set of encoding values that are
 * available to the user. The list of encoding values is based off the SupportedJavaEncoding class.
 * As the user selects an encoding from the combo box, the readonly field below it changes to show
 * the IANA tag for that particular encoding description. The labels for the widgets are
 * configurable and the initial value to display to the user can be set using the setIANATag(). The
 * currently selected entry's IANA tag can be retrieved with getIANATag(). Entries displayed to the
 * user can be added and removed.
 */
public class EncodingSettings extends Composite {

  private class ComboListener implements ModifyListener {
    public void modifyText(ModifyEvent e) {
      int i = encodingCombo.getSelectionIndex();
      if ((i >= 0) && (i < ianaVector.size())) {
        ianaText.setText((String) (ianaVector.elementAt(encodingCombo.getSelectionIndex())));
      }
    }
  }

  private static String ENCODING_LABEL = XMLUIMessages.EncodingSettings_1;

  private static String IANA_LABEL = XMLUIMessages.EncodingSettings_0;

  private ModifyListener comboListener = new ComboListener();
  protected Combo encodingCombo;
  protected Label encodingLabel, ianaLabel;
  protected Text ianaText;
  protected Vector ianaVector;

  /**
   * Method EncodingSettings.
   * 
   * @param parent
   */
  public EncodingSettings(Composite parent) {
    super(parent, SWT.NONE);
    init(IANA_LABEL, ENCODING_LABEL);
  }

  /**
   * Method EncodingSettings.
   * 
   * @param parent
   * @param encodingLabel - text label to use beside the locale sensitive description of the
   *          currently selected encoding
   */
  public EncodingSettings(Composite parent, String encodingLabel) {
    super(parent, SWT.NONE);
    init(IANA_LABEL, encodingLabel);
  }

  /**
   * Method EncodingSettings.
   * 
   * @param parent
   * @param ianaLabel = text label to use beside the display only IANA field
   * @param encodingLabel - text label to use beside the locale sensitive description of the
   *          currently selected encoding
   */
  public EncodingSettings(Composite parent, String ianaLabel, String encodingLabel) {
    super(parent, SWT.NONE);
    init(ianaLabel, encodingLabel);
  }

  /**
   * Method addEntry. Add an entry to the end of the Encoding Combobox
   * 
   * @param description - encoding description to display
   * @param ianaTag - IANA tag for the description
   */
  public void addEntry(String description, String ianaTag) {
    encodingCombo.add(description);
    ianaVector.add(ianaTag);
  }

  /**
   * Method addEntry. Add an entry to the Encoding Combobox at index index
   * 
   * @param description - encoding description to display
   * @param ianaTag - IANA tag for the description
   * @param index - index into the combo to add to
   */
  public void addEntry(String description, String ianaTag, int index) {
    if (index == ianaVector.size()) {
      // just add to the end
      addEntry(description, ianaTag);
      return;
    }

    if ((0 <= index) && (index < ianaVector.size())) {
      encodingCombo.add(description, index);
      ianaVector.add(index, ianaTag);
    }
  }

  protected Combo createComboBox(Composite parent, boolean isReadOnly) {
    int style = isReadOnly == true ? SWT.READ_ONLY : SWT.DROP_DOWN;

    Combo combo = new Combo(parent, style);

    GridData data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.grabExcessHorizontalSpace = true;
    combo.setLayoutData(data);
    return combo;
  }

  /**
   * Helper method for creating labels.
   */
  protected Label createLabel(Composite parent, String text) {
    Label label = new Label(parent, SWT.LEFT);
    label.setText(text);

    GridData data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    label.setLayoutData(data);
    return label;
  }

  protected Text createTextField(Composite parent, int width) {
    Text text = new Text(parent, SWT.SINGLE | SWT.READ_ONLY);

    GridData data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.grabExcessHorizontalSpace = true;
    data.widthHint = width;
    text.setLayoutData(data);

    text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    return text;
  }

  /**
   * @see org.eclipse.swt.widgets.Widget#dispose()
   */
  public void dispose() {
    encodingCombo.removeModifyListener(comboListener);
    super.dispose();
  }

  private void fillCombo() {
    try {
      String[] ianaTags = CommonCharsetNames.getCommonCharsetNames();
      int totalNum = ianaTags.length;
      for (int i = 0; i < totalNum; i++) {
        String iana = ianaTags[i];
        String enc = CommonCharsetNames.getDisplayString(iana);

        if (enc != null) {
          encodingCombo.add(enc);
        } else {
          Logger.log(Logger.WARNING,
              "CommonCharsetNames.getDisplayString(" + iana + ") returned null"); //$NON-NLS-1$ //$NON-NLS-2$
          encodingCombo.add(iana);
        }
        ianaVector.add(iana);
      }
    } catch (Exception e) {
      // e.printStackTrace();
      // MessageDialog.openError(getShell(), "Resource exception",
      // "Unable to obtain encoding strings. Check resource file");
      // XMLEncodingPlugin.getPlugin().getMsgLogger().write(e.toString());
      // XMLEncodingPlugin.getPlugin().getMsgLogger().writeCurrentThread();
      Logger.log(Logger.ERROR, "Exception", e); //$NON-NLS-1$
    }
  }

  /**
   * <code>getEncoding</code> Get the descriptive encoding name that was selected.
   * 
   * @return a <code>String</code> value
   */
  public String getEncoding() {
    return encodingCombo.getText();
  }

  /**
   * Method getEncodingCombo. Returns the combo used to display the encoding descriptions.
   * 
   * @return Combo
   */
  public Combo getEncodingCombo() {
    return encodingCombo;
  }

  /**
   * <code>getIANATag</code> Get the IANA tag equivalent of the selected descriptive encoding name
   * 
   * @return a <code>String</code> value
   */
  public String getIANATag() {
    int i = encodingCombo.getSelectionIndex();
    if (i >= 0) {
      return (String) (ianaVector.elementAt(i));
    }
    return ""; //$NON-NLS-1$
  }

  protected void init(String ianaLabelStr, String encodingLabelStr) {
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    setLayout(layout);
    GridData data = new GridData();
    data.verticalAlignment = GridData.FILL;
    data.horizontalAlignment = GridData.FILL;
    data.grabExcessHorizontalSpace = true;
    setLayoutData(data);

    encodingLabel = createLabel(this, encodingLabelStr);
    encodingCombo = createComboBox(this, true);
    ianaLabel = createLabel(this, ianaLabelStr);
    ianaText = createTextField(this, 20);
    ianaVector = new Vector();

    fillCombo();
    resetToDefaultEncoding();
    encodingCombo.addModifyListener(comboListener);
  }

  /**
   * <code>isEncodingInList</code> Checks whether the encoding name is in the combo
   * 
   * @param enc a <code>string</code> value. The encoding name.
   * @return a <code>boolean</code> value. TRUE if encoding is in list. FALSE if encoding is not in
   *         list.
   */
  public boolean isEncodingInList(String enc) {
    int i = encodingCombo.indexOf(enc);
    if (i >= 0) {
      return true;
    }
    return false;
  }

  /**
   * <code>isIANATagInList</code> Checks whether the IANA tag is in the combo
   * 
   * @param ianaTag a <code>string</code> value. The IANA tag.
   * @return a <code>boolean</code> value. TRUE if tag is in list. FALSE if tag is not in list.
   */
  public boolean isIANATagInList(String ianaTag) {
    int i = ianaVector.indexOf(ianaTag);
    if (i >= 0) {
      return true;
    }
    return false;
  }

  /**
   * Method removeEntry. Removes both the description and the IANA tag at the specified index
   * 
   * @param index
   */
  public void removeEntry(int index) {
    if ((0 <= index) && (index < ianaVector.size())) {
      encodingCombo.remove(index);
      ianaVector.remove(index);
    }
  }

  /**
   * Method resetToDefaultEncoding. Reset the control to the default encoding. Currently UTF-8
   */
  public void resetToDefaultEncoding() {
    String defaultIANATag = "UTF-8"; //$NON-NLS-1$
    ianaText.setText(defaultIANATag);
    setIANATag(defaultIANATag);
  }

  /**
   * Method setEnabled. Enable/disable the EncodingSettings composite.
   * 
   * @param enabled
   */
  public void setEnabled(boolean enabled) {
    encodingCombo.setEnabled(enabled);
    encodingLabel.setEnabled(enabled);
    ianaLabel.setEnabled(enabled);
    ianaText.setEnabled(enabled);
  }

  /**
   * <code>setEncoding</code> Set the selection in the combo to the descriptive encoding name.
   * 
   * @param enc a <code>string</code> value. Note this is not the IANA tag.
   */
  public void setEncoding(String enc) {
    encodingCombo.setText(enc);
    encodingCombo.select(encodingCombo.indexOf(enc));
  }

  /**
   * <code>setIANATag</code> Set the IANA tag for the combo
   * 
   * @param ianaTag a <code>string</code> value. The IANA tag.
   */
  public void setIANATag(String ianaTag) {
    ianaTag = CommonCharsetNames.getPreferenceName(ianaTag);
    int i = ianaVector.indexOf(ianaTag);
    if (i >= 0) {
      encodingCombo.select(i);
    }
  }

}
