package com.google.dart.tools.deploy;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.preference.IPreferenceStore;

public class DeployPreferenceInitializer extends AbstractPreferenceInitializer {
  private static final String CONSOLE_LOW_WATER_MARK = "Console.lowWaterMark";
  private static final String CONSOLE_HIGH_WATER_MARK = "Console.highWaterMark";

  @Override
  public void initializeDefaultPreferences() {
    initializeDebugUIPreferences();
  }

  private void initializeDebugUIPreferences() {
    IPreferenceStore store = DebugUITools.getPreferenceStore();
    int low = 1000000;
    int high = low + 100 * 80;
    store.setValue(CONSOLE_LOW_WATER_MARK, low);
    store.setValue(CONSOLE_HIGH_WATER_MARK, high);
  }
}
