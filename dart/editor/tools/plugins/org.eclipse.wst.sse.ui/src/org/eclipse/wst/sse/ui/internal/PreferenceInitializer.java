/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.wst.sse.ui.internal.preferences.EditorPreferenceNames;
import org.eclipse.wst.sse.ui.internal.preferences.ui.ColorHelper;
import org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredFoldingStrategy;
import org.eclipse.wst.sse.ui.internal.provisional.preferences.CommonEditorPreferenceNames;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences
   * ()
   */
  public void initializeDefaultPreferences() {
    IPreferenceStore store = SSEUIPlugin.getDefault().getPreferenceStore();
    ColorRegistry registry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();

    // use the base annotation & quick diff preference page
    EditorsUI.useAnnotationsPreferencePage(store);
    EditorsUI.useQuickDiffPreferencePage(store);

    // let annotations show up in the vertical ruler if that's what the
    // preference is
    //		// these annotation preferences have a different default value than
    // the one the base provides
    //		store.setDefault("errorIndicationInVerticalRuler", false);
    // //$NON-NLS-1$
    //		store.setDefault("warningIndicationInVerticalRuler", false);
    // //$NON-NLS-1$

    // these annotation preferences are not part of base text editor
    // preference
    store.setDefault(CommonEditorPreferenceNames.EVALUATE_TEMPORARY_PROBLEMS, true);

    setMatchingBracketsPreferences(store, registry);

    // hover help preferences are not part of base text editor preference
    String mod2Name = Action.findModifierString(SWT.MOD2); // SWT.COMMAND
    // on Mac;
    // SWT.CONTROL
    // elsewhere
    store.setDefault(
        EditorPreferenceNames.EDITOR_TEXT_HOVER_MODIFIERS,
        "combinationHover|true|0;problemHover|false|0;documentationHover|false|0;annotationHover|true|" + mod2Name); //$NON-NLS-1$

    // set default read-only foreground color scale value
    store.setDefault(EditorPreferenceNames.READ_ONLY_FOREGROUND_SCALE, 30);

    // set default enable folding value
    store.setDefault(AbstractStructuredFoldingStrategy.FOLDING_ENABLED, true);

    // set default for show message dialog when unknown content type in editor
    store.setDefault(EditorPreferenceNames.SHOW_UNKNOWN_CONTENT_TYPE_MSG, true);

    // set content assist defaults
    store.setDefault(EditorPreferenceNames.CODEASSIST_AUTOACTIVATION_DELAY, 500);
    PreferenceConverter.setDefault(store, EditorPreferenceNames.CODEASSIST_PROPOSALS_BACKGROUND,
        ColorHelper.findRGB(registry, EditorPreferenceNames.CODEASSIST_PROPOSALS_BACKGROUND,
            new RGB(255, 255, 255)));
    PreferenceConverter.setDefault(store, EditorPreferenceNames.CODEASSIST_PROPOSALS_FOREGROUND,
        ColorHelper.findRGB(registry, EditorPreferenceNames.CODEASSIST_PROPOSALS_FOREGROUND,
            new RGB(0, 0, 0)));
    PreferenceConverter.setDefault(store, EditorPreferenceNames.CODEASSIST_PARAMETERS_BACKGROUND,
        ColorHelper.findRGB(registry, EditorPreferenceNames.CODEASSIST_PARAMETERS_BACKGROUND,
            new RGB(255, 255, 255)));
    PreferenceConverter.setDefault(store, EditorPreferenceNames.CODEASSIST_PARAMETERS_FOREGROUND,
        ColorHelper.findRGB(registry, EditorPreferenceNames.CODEASSIST_PARAMETERS_FOREGROUND,
            new RGB(0, 0, 0)));

    store.setDefault(EditorPreferenceNames.SEMANTIC_HIGHLIGHTING, true);
  }

  private void setMatchingBracketsPreferences(IPreferenceStore store, ColorRegistry registry) {
    // matching brackets is not part of base text editor preference
    store.setDefault(EditorPreferenceNames.MATCHING_BRACKETS, true);
    PreferenceConverter.setDefault(store, EditorPreferenceNames.MATCHING_BRACKETS_COLOR,
        ColorHelper.findRGB(registry, EditorPreferenceNames.MATCHING_BRACKETS_COLOR, new RGB(192,
            192, 192)));
  }
}
