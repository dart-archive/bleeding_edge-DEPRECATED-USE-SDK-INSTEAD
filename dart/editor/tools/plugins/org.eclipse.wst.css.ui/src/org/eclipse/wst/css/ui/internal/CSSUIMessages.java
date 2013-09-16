/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal;

import org.eclipse.osgi.util.NLS;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class CSSUIMessages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.wst.css.ui.internal.CSSUIPluginResources";//$NON-NLS-1$
  private static ResourceBundle fResourceBundle;

  static {
    // load message values from bundle file
    NLS.initializeMessages(BUNDLE_NAME, CSSUIMessages.class);
  }

  public static ResourceBundle getResourceBundle() {
    try {
      if (fResourceBundle == null)
        fResourceBundle = ResourceBundle.getBundle(BUNDLE_NAME);
    } catch (MissingResourceException x) {
      fResourceBundle = null;
    }
    return fResourceBundle;
  }

  public static String ID_Selector_Case__UI_;
  public static String INFO_Not_Categorized_1;
  public static String PrefsLabel_WrappingWithoutAttr;
  public static String PrefsLabel_WrappingInsertLineBreak;
  public static String PrefsLabel_CaseGroup;
  public static String PrefsLabel_CaseIdent;
  public static String PrefsLabel_CasePropName;
  public static String PrefsLabel_CasePropValue;
  public static String PrefsLabel_CaseIdentUpper;
  public static String PrefsLabel_CaseIdentLower;
  public static String PrefsLabel_SelectorTagName;
  public static String PrefsLabel_CasePropNameUpper;
  public static String PrefsLabel_CasePropNameLower;
  public static String PrefsLabel_CasePropValueUpper;
  public static String PrefsLabel_CasePropValueLower;
  public static String PrefsLabel_SelectorTagNameUpper;
  public static String PrefsLabel_SelectorTagNameLower;
  public static String PrefsLabel_ColorSample;
  public static String PrefsLabel_ColorNormal;
  public static String PrefsLabel_ColorAtmarkRule;
  public static String PrefsLabel_ColorSelector;
  public static String PrefsLabel_ColorCombinator;
  public static String PrefsLabel_ColorUniversal;
  public static String PrefsLabel_ColorId;
  public static String PrefsLabel_ColorPseudoClass;
  public static String PrefsLabel_ColorClass;
  public static String PrefsLabel_ColorMedia;
  public static String PrefsLabel_ColorComment;
  public static String PrefsLabel_ColorPropertyName;
  public static String PrefsLabel_ColorPropertyValue;
  public static String PrefsLabel_ColorUri;
  public static String PrefsLabel_ColorString;
  public static String PrefsLabel_ColorColon;
  public static String PrefsLabel_ColorSemiColon;
  public static String PrefsLabel_ColorCurlyBrace;
  public static String PrefsLabel_ColorError;
  public static String PrefsLabel_ColorAttrDelimiter;
  public static String PrefsLabel_ColorAttrName;
  public static String PrefsLabel_ColorAttrValue;
  public static String PrefsLabel_ColorAttrOperator;
  public static String SortAction_0;
  public static String _UI_WIZARD_NEW_TITLE;
  public static String _UI_WIZARD_NEW_HEADING;
  public static String _UI_WIZARD_NEW_DESCRIPTION;
  public static String _ERROR_FILENAME_MUST_END_CSS;
  public static String _WARNING_FOLDER_MUST_BE_INSIDE_WEB_CONTENT;
  public static String Title_InvalidValue;
  public static String Message_InvalidValue;
  public static String FormatMenu_label;
  public static String Class_Selector_Case__UI_;
  public static String CleanupDocument_label; // resource bundle
  public static String CleanupDocument_tooltip; // resource bundle
  public static String CleanupDocument_description; // resource bundle
  public static String UI_none;
  public static String Cleanup_UI_;
  public static String CSS_Cleanup_UI_;
  public static String ToggleComment_tooltip;
  public static String AddBlockComment_tooltip;
  public static String RemoveBlockComment_tooltip;
  public static String Identifier_case__UI_;
  public static String Property_name_case__UI_;
  public static String Property_value_case__UI_;
  public static String Selector_tag_name_case__UI_;
  public static String Quote_values_UI_;
  public static String Format_source_UI_;
  public static String As_is_UI_;
  public static String Lower_UI_;
  public static String Upper_UI_;
  public static String SourceMenu_label;
  public static String Formatting_UI_;
  public static String Line_width__UI_;
  public static String Indent_using_tabs_;
  public static String Indent_using_spaces;
  public static String Indentation_size;
  public static String Indentation_size_tip;
  public static String StructureSelectEnclosing_label;
  public static String StructureSelectEnclosing_tooltip;
  public static String StructureSelectEnclosing_description;
  public static String StructureSelectNext_label;
  public static String StructureSelectNext_tooltip;
  public static String StructureSelectNext_description;
  public static String StructureSelectPrevious_label;
  public static String StructureSelectPrevious_tooltip;
  public static String StructureSelectPrevious_description;
  public static String Creating_files_encoding;
  public static String ResourceGroup_nameExists;
  public static String NewCSSTemplatesWizardPage_0;
  public static String NewCSSTemplatesWizardPage_1;
  public static String NewCSSTemplatesWizardPage_2;
  public static String NewCSSTemplatesWizardPage_3;
  public static String NewCSSTemplatesWizardPage_4;
  public static String NewCSSTemplatesWizardPage_5;
  public static String NewCSSTemplatesWizardPage_6;
  public static String NewCSSTemplatesWizardPage_7;
  public static String CSSContentSettingsPropertyPage_0;
  public static String CSSContentSettingsPropertyPage_1;
  public static String SyntaxColoringPage_0;
  public static String SyntaxColoringPage_2;
  public static String SyntaxColoringPage_3;
  public static String SyntaxColoringPage_4;
  public static String SyntaxColoringPage_5;
  public static String SyntaxColoringPage_6;
  public static String _UI_STRUCTURED_TEXT_EDITOR_PREFS_LINK;
}
