/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.editor.IHelpContextIds;

public class XMLTypingPreferencePage extends AbstractPreferencePage {

  private Button fCloseComment;
  private Button fCloseEndTag;
  private Button fRemoveEndTag;
  private Button fCloseElement;
  private Button fCloseStrings;
  private Button fCloseBrackets;

  protected Control createContents(Composite parent) {
    Composite composite = super.createComposite(parent, 1);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,
        IHelpContextIds.XML_PREFWEBX_FILES_HELPID);

    createStartTagGroup(composite);
    createEndTagGroup(composite);
    createAutoComplete(composite);
    createAutoRemove(composite);

    setSize(composite);
    loadPreferences();

    return composite;
  }

  private void createStartTagGroup(Composite parent) {
    Group group = createGroup(parent, 2);

    group.setText(XMLUIMessages.XMLTyping_Start_Tag);

    fCloseElement = createCheckBox(group, XMLUIMessages.XMLTyping_Complete_Elements);
    ((GridData) fCloseElement.getLayoutData()).horizontalSpan = 2;
  }

  private void createEndTagGroup(Composite parent) {
    Group group = createGroup(parent, 2);

    group.setText(XMLUIMessages.XMLTyping_End_Tag);

    fCloseEndTag = createCheckBox(group, XMLUIMessages.XMLTyping_Complete_End_Tags);
    ((GridData) fCloseEndTag.getLayoutData()).horizontalSpan = 2;
  }

  private void createAutoComplete(Composite parent) {
    Group group = createGroup(parent, 2);

    group.setText(XMLUIMessages.XMLTyping_Auto_Complete);

    fCloseComment = createCheckBox(group, XMLUIMessages.XMLTyping_Complete_Comments);
    ((GridData) fCloseComment.getLayoutData()).horizontalSpan = 2;

    fCloseStrings = createCheckBox(group, XMLUIMessages.XMLTyping_Close_Strings);
    ((GridData) fCloseStrings.getLayoutData()).horizontalSpan = 2;

    fCloseBrackets = createCheckBox(group, XMLUIMessages.XMLTyping_Close_Brackets);
    ((GridData) fCloseBrackets.getLayoutData()).horizontalSpan = 2;
  }

  private void createAutoRemove(Composite parent) {
    Group group = createGroup(parent, 2);

    group.setText(XMLUIMessages.XMLTyping_Auto_Remove);

    fRemoveEndTag = createCheckBox(group, XMLUIMessages.XMLTyping_Remove_End_Tags);
    ((GridData) fRemoveEndTag.getLayoutData()).horizontalSpan = 2;
  }

  public boolean performOk() {
    boolean result = super.performOk();

    XMLUIPlugin.getDefault().savePluginPreferences();

    return result;
  }

  protected void initializeValues() {
    initCheckbox(fCloseComment, XMLUIPreferenceNames.TYPING_COMPLETE_COMMENTS);
    initCheckbox(fCloseEndTag, XMLUIPreferenceNames.TYPING_COMPLETE_END_TAGS);
    initCheckbox(fCloseElement, XMLUIPreferenceNames.TYPING_COMPLETE_ELEMENTS);
    initCheckbox(fRemoveEndTag, XMLUIPreferenceNames.TYPING_REMOVE_END_TAGS);
    initCheckbox(fCloseStrings, XMLUIPreferenceNames.TYPING_CLOSE_STRINGS);
    initCheckbox(fCloseBrackets, XMLUIPreferenceNames.TYPING_CLOSE_BRACKETS);
  }

  protected void performDefaults() {
    defaultCheckbox(fCloseComment, XMLUIPreferenceNames.TYPING_COMPLETE_COMMENTS);
    defaultCheckbox(fCloseEndTag, XMLUIPreferenceNames.TYPING_COMPLETE_END_TAGS);
    defaultCheckbox(fCloseElement, XMLUIPreferenceNames.TYPING_COMPLETE_ELEMENTS);
    defaultCheckbox(fRemoveEndTag, XMLUIPreferenceNames.TYPING_REMOVE_END_TAGS);
    defaultCheckbox(fCloseStrings, XMLUIPreferenceNames.TYPING_CLOSE_STRINGS);
    defaultCheckbox(fCloseBrackets, XMLUIPreferenceNames.TYPING_CLOSE_BRACKETS);
  }

  protected void storeValues() {
    getPreferenceStore().setValue(XMLUIPreferenceNames.TYPING_COMPLETE_COMMENTS,
        (fCloseComment != null) ? fCloseComment.getSelection() : false);
    getPreferenceStore().setValue(XMLUIPreferenceNames.TYPING_COMPLETE_END_TAGS,
        (fCloseEndTag != null) ? fCloseEndTag.getSelection() : false);
    getPreferenceStore().setValue(XMLUIPreferenceNames.TYPING_COMPLETE_ELEMENTS,
        (fCloseElement != null) ? fCloseElement.getSelection() : false);
    getPreferenceStore().setValue(XMLUIPreferenceNames.TYPING_REMOVE_END_TAGS,
        (fRemoveEndTag != null) ? fRemoveEndTag.getSelection() : false);
    getPreferenceStore().setValue(XMLUIPreferenceNames.TYPING_CLOSE_STRINGS,
        (fCloseStrings != null) ? fCloseStrings.getSelection() : false);
    getPreferenceStore().setValue(XMLUIPreferenceNames.TYPING_CLOSE_BRACKETS,
        (fCloseBrackets != null) ? fCloseBrackets.getSelection() : false);
  }

  protected IPreferenceStore doGetPreferenceStore() {
    return XMLUIPlugin.getDefault().getPreferenceStore();
  }
}
