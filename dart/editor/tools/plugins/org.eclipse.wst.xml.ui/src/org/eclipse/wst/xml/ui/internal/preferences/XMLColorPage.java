/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring Benjamin Muskalla,
 * b.muskalla@gmx.net - [158660] character entities should have their own syntax highlighting
 * preference
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.preferences;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.preferences.OverlayPreferenceStore;
import org.eclipse.wst.sse.ui.internal.preferences.OverlayPreferenceStore.OverlayKey;
import org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractColorPage;
import org.eclipse.wst.sse.ui.internal.preferences.ui.StyledTextColorPicker;
import org.eclipse.wst.xml.core.internal.provisional.contenttype.ContentTypeIdForXML;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.editor.IHelpContextIds;
import org.eclipse.wst.xml.ui.internal.style.IStyleConstantsXML;

/**
 * @deprecated
 */
public class XMLColorPage extends AbstractColorPage {

  protected Control createContents(Composite parent) {
    Composite pageComponent = createComposite(parent, 1);
    ((GridData) pageComponent.getLayoutData()).horizontalAlignment = GridData.HORIZONTAL_ALIGN_FILL;

    super.createContents(pageComponent);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(pageComponent,
        IHelpContextIds.XML_PREFWEBX_STYLES_HELPID);
    return pageComponent;
  }

  /**
   * Set up all the style preference keys in the overlay store
   */
  protected OverlayKey[] createOverlayStoreKeys() {
    ArrayList overlayKeys = new ArrayList();

    ArrayList styleList = new ArrayList();
    initStyleList(styleList);
    Iterator i = styleList.iterator();
    while (i.hasNext()) {
      overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING,
          (String) i.next()));
    }

    OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
    overlayKeys.toArray(keys);
    return keys;
  }

  protected IPreferenceStore doGetPreferenceStore() {
    return XMLUIPlugin.getDefault().getPreferenceStore();
  }

  public String getSampleText() {
    return XMLUIMessages.Sample_XML_doc; // = "<?xml
    // version=\"1.0\"?>\n<?customProcessingInstruction\n\tXML
    // processor
    // specific\n\tcontent
    // ?>\n<!DOCTYPE
    // colors\n\tPUBLIC
    // \"//IBM/XML/COLORS/\"
    // \"colors.dtd\">\n<colors>\n\t<!--
    // begin color definitions
    // -->\n\t<color
    // name=\"plaintext\"
    // foreground=\"#000000\"\n\t\tbackground=\"#D4D0C8\"/>\n\t<color
    // name=\"bold\"
    // foreground=\"#000000\"\n\t\tbackground=\"#B3ACA0\">\n\t<![CDATA[<123456789>]]>\n\tNormal
    // text content.\n\t<color
    // name=\"inverse\"
    // foreground=\"#F0F0F0\"\n\t\tbackground=\"#D4D0C8\"/>\n\n</colors>\n";
  }

  protected void initCommonContextStyleMap(Dictionary contextStyleMap) {

    contextStyleMap.put(DOMRegionContext.XML_COMMENT_OPEN, IStyleConstantsXML.COMMENT_BORDER);
    contextStyleMap.put(DOMRegionContext.XML_COMMENT_TEXT, IStyleConstantsXML.COMMENT_TEXT);
    contextStyleMap.put(DOMRegionContext.XML_COMMENT_CLOSE, IStyleConstantsXML.COMMENT_BORDER);

    contextStyleMap.put(DOMRegionContext.XML_TAG_OPEN, IStyleConstantsXML.TAG_BORDER);
    contextStyleMap.put(DOMRegionContext.XML_END_TAG_OPEN, IStyleConstantsXML.TAG_BORDER);
    contextStyleMap.put(DOMRegionContext.XML_TAG_NAME, IStyleConstantsXML.TAG_NAME);
    contextStyleMap.put(DOMRegionContext.XML_TAG_ATTRIBUTE_NAME,
        IStyleConstantsXML.TAG_ATTRIBUTE_NAME);
    contextStyleMap.put(DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE,
        IStyleConstantsXML.TAG_ATTRIBUTE_VALUE);
    contextStyleMap.put(DOMRegionContext.XML_TAG_CLOSE, IStyleConstantsXML.TAG_BORDER);
    contextStyleMap.put(DOMRegionContext.XML_EMPTY_TAG_CLOSE, IStyleConstantsXML.TAG_BORDER);

    contextStyleMap.put(DOMRegionContext.XML_DECLARATION_OPEN, IStyleConstantsXML.DECL_BORDER);
    contextStyleMap.put(DOMRegionContext.XML_DECLARATION_CLOSE, IStyleConstantsXML.DECL_BORDER);
    contextStyleMap.put(DOMRegionContext.XML_ELEMENT_DECLARATION, IStyleConstantsXML.DECL_BORDER);
    contextStyleMap.put(DOMRegionContext.XML_ELEMENT_DECL_CLOSE, IStyleConstantsXML.DECL_BORDER);

    contextStyleMap.put(DOMRegionContext.XML_CHAR_REFERENCE, IStyleConstantsXML.ENTITY_REFERENCE);
    contextStyleMap.put(DOMRegionContext.XML_ENTITY_REFERENCE, IStyleConstantsXML.ENTITY_REFERENCE);
    contextStyleMap.put(DOMRegionContext.XML_PE_REFERENCE, IStyleConstantsXML.ENTITY_REFERENCE);

    contextStyleMap.put(DOMRegionContext.XML_CONTENT, IStyleConstantsXML.XML_CONTENT);
  }

  protected void initCommonDescriptions(Dictionary descriptions) {

    // create descriptions for hilighting types
    descriptions.put(IStyleConstantsXML.COMMENT_BORDER, XMLUIMessages.Comment_Delimiters_UI_); // =
    // "Comment
    // Delimiters"
    descriptions.put(IStyleConstantsXML.COMMENT_TEXT, XMLUIMessages.Comment_Content_UI_); // =
    // "Comment
    // Content"
    descriptions.put(IStyleConstantsXML.TAG_BORDER, XMLUIMessages.Tag_Delimiters_UI_); // =
    // "Tag
    // Delimiters"
    descriptions.put(IStyleConstantsXML.TAG_NAME, XMLUIMessages.Tag_Names_UI_); // =
    // "Tag
    // Names"
    descriptions.put(IStyleConstantsXML.TAG_ATTRIBUTE_NAME, XMLUIMessages.Attribute_Names_UI_); // =
    // "Attribute
    // Names"
    descriptions.put(IStyleConstantsXML.TAG_ATTRIBUTE_VALUE, XMLUIMessages.Attribute_Values_UI_); // =
    // "Attribute
    // Values"
    descriptions.put(IStyleConstantsXML.DECL_BORDER, XMLUIMessages.Declaration_Delimiters_UI_); // =
    // "Declaration
    // Delimiters"
    descriptions.put(IStyleConstantsXML.XML_CONTENT, XMLUIMessages.Content_UI_); // =
    // "Content"
    descriptions.put(IStyleConstantsXML.ENTITY_REFERENCE, XMLUIMessages.Entity_Reference_UI_); //$NON-NLS-1$ = "Entity References"
  }

  protected void initCommonStyleList(ArrayList list) {

    // list.add(IStyleConstantsXML.CDATA_BORDER);
    // list.add(IStyleConstantsXML.CDATA_TEXT);
    // list.add(IStyleConstantsXML.PI_BORDER);
    // list.add(IStyleConstantsXML.PI_CONTENT);

    list.add(IStyleConstantsXML.TAG_BORDER);
    list.add(IStyleConstantsXML.TAG_NAME);
    list.add(IStyleConstantsXML.TAG_ATTRIBUTE_NAME);
    list.add(IStyleConstantsXML.TAG_ATTRIBUTE_VALUE);
    list.add(IStyleConstantsXML.COMMENT_BORDER);
    list.add(IStyleConstantsXML.COMMENT_TEXT);
    list.add(IStyleConstantsXML.DECL_BORDER);
    list.add(IStyleConstantsXML.XML_CONTENT);
    list.add(IStyleConstantsXML.ENTITY_REFERENCE);
  }

  protected void initContextStyleMap(Dictionary contextStyleMap) {

    initCommonContextStyleMap(contextStyleMap);
    initDocTypeContextStyleMap(contextStyleMap);
    contextStyleMap.put(DOMRegionContext.XML_CDATA_OPEN, IStyleConstantsXML.CDATA_BORDER);
    contextStyleMap.put(DOMRegionContext.XML_CDATA_TEXT, IStyleConstantsXML.CDATA_TEXT);
    contextStyleMap.put(DOMRegionContext.XML_CDATA_CLOSE, IStyleConstantsXML.CDATA_BORDER);

    contextStyleMap.put(DOMRegionContext.XML_PI_OPEN, IStyleConstantsXML.PI_BORDER);
    contextStyleMap.put(DOMRegionContext.XML_PI_CONTENT, IStyleConstantsXML.PI_CONTENT);
    contextStyleMap.put(DOMRegionContext.XML_PI_CLOSE, IStyleConstantsXML.PI_BORDER);

  }

  protected void initDescriptions(Dictionary descriptions) {

    initCommonDescriptions(descriptions);
    initDocTypeDescriptions(descriptions);
    descriptions.put(IStyleConstantsXML.CDATA_BORDER, XMLUIMessages.CDATA_Delimiters_UI_); // =
    // "CDATA
    // Delimiters"
    descriptions.put(IStyleConstantsXML.CDATA_TEXT, XMLUIMessages.CDATA_Content_UI_); // =
    // "CDATA
    // Content"
    descriptions.put(IStyleConstantsXML.PI_BORDER, XMLUIMessages.Processing_Instruction_Del_UI_); // =
    // "Processing
    // Instruction
    // Delimiters"
    descriptions.put(IStyleConstantsXML.PI_CONTENT,
        XMLUIMessages.Processing_Instruction_Con_UI__UI_); // =
    // "Processing
    // Instruction
    // Content"
  }

  protected void initDocTypeContextStyleMap(Dictionary contextStyleMap) {

    contextStyleMap.put(DOMRegionContext.XML_ELEMENT_DECL_NAME, IStyleConstantsXML.DOCTYPE_NAME);
    contextStyleMap.put(DOMRegionContext.XML_DOCTYPE_DECLARATION, IStyleConstantsXML.TAG_NAME);
    contextStyleMap.put(DOMRegionContext.XML_DOCTYPE_DECLARATION_CLOSE,
        IStyleConstantsXML.DECL_BORDER);

    contextStyleMap.put(DOMRegionContext.XML_DOCTYPE_NAME, IStyleConstantsXML.DOCTYPE_NAME);
    contextStyleMap.put(DOMRegionContext.XML_DOCTYPE_EXTERNAL_ID_PUBLIC,
        IStyleConstantsXML.DOCTYPE_EXTERNAL_ID);
    contextStyleMap.put(DOMRegionContext.XML_DOCTYPE_EXTERNAL_ID_PUBREF,
        IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_PUBREF);
    contextStyleMap.put(DOMRegionContext.XML_DOCTYPE_EXTERNAL_ID_SYSTEM,
        IStyleConstantsXML.DOCTYPE_EXTERNAL_ID);
    contextStyleMap.put(DOMRegionContext.XML_DOCTYPE_EXTERNAL_ID_SYSREF,
        IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_SYSREF);
  }

  protected void initDocTypeDescriptions(Dictionary descriptions) {

    // create descriptions for hilighting types for DOCTYPE related items
    descriptions.put(IStyleConstantsXML.DOCTYPE_NAME, XMLUIMessages.DOCTYPE_Name_UI_); // =
    // "DOCTYPE
    // Name"
    descriptions.put(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID,
        XMLUIMessages.DOCTYPE_SYSTEM_PUBLIC_Keyw_UI_); // =
    // "DOCTYPE
    // SYSTEM/PUBLIC
    // Keyword"
    descriptions.put(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_PUBREF,
        XMLUIMessages.DOCTYPE_Public_Reference_UI_); // =
    // "DOCTYPE
    // Public
    // Reference"
    descriptions.put(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_SYSREF,
        XMLUIMessages.DOCTYPE_System_Reference_UI_); // =
    // "DOCTYPE
    // System
    // Reference"
  }

  protected void initDocTypeStyleList(ArrayList list) {

    list.add(IStyleConstantsXML.DOCTYPE_NAME);
    list.add(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID);
    list.add(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_PUBREF);
    list.add(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_SYSREF);
  }

  protected void initStyleList(ArrayList list) {
    initCommonStyleList(list);
    initDocTypeStyleList(list);
    list.add(IStyleConstantsXML.CDATA_BORDER);
    list.add(IStyleConstantsXML.CDATA_TEXT);
    list.add(IStyleConstantsXML.PI_BORDER);
    list.add(IStyleConstantsXML.PI_CONTENT);
  }

  public boolean performOk() {
    // required since the superclass *removes* existing preferences before
    // saving its own
    super.performOk();

    SSEUIPlugin.getDefault().savePluginPreferences();
    return true;
  }

  protected void setupPicker(StyledTextColorPicker picker) {
    IModelManager mmanager = StructuredModelManager.getModelManager();
    picker.setParser(mmanager.createStructuredDocumentFor(ContentTypeIdForXML.ContentTypeID_XML).getParser());

    Dictionary descriptions = new Hashtable();
    initDescriptions(descriptions);

    Dictionary contextStyleMap = new Hashtable();
    initContextStyleMap(contextStyleMap);

    ArrayList styleList = new ArrayList();
    initStyleList(styleList);

    picker.setContextStyleMap(contextStyleMap);
    picker.setDescriptions(descriptions);
    picker.setStyleList(styleList);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.preferences.ui.AbstractColorPage#savePreferences()
   */
  protected void savePreferences() {
    XMLUIPlugin.getDefault().savePluginPreferences();
  }
}
