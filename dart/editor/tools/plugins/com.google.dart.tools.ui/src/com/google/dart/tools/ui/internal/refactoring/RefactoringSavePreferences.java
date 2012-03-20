package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;

import org.eclipse.jface.preference.IPreferenceStore;

public class RefactoringSavePreferences {

  public static final String PREF_SAVE_ALL_EDITORS = PreferenceConstants.REFACTOR_SAVE_ALL_EDITORS;

  public static boolean getSaveAllEditors() {
    IPreferenceStore store = DartToolsPlugin.getDefault().getPreferenceStore();
    return store.getBoolean(PREF_SAVE_ALL_EDITORS);
  }

  public static void setSaveAllEditors(boolean save) {
    IPreferenceStore store = DartToolsPlugin.getDefault().getPreferenceStore();
    store.setValue(PREF_SAVE_ALL_EDITORS, save);
  }
}
