/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.debug.ui.launch;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.menus.IMenuStateIds;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import java.util.Map;

/**
 * Toggle the break-on-exceptions setting, and update the command based on changes to the
 * preferences.
 */
public class ToggleBreakOnExceptionsHandler extends AbstractHandler implements IElementUpdater {
  private IPreferenceChangeListener listener = new IPreferenceChangeListener() {
    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
      if (DartDebugCorePlugin.PREFS_BREAK_ON_EXCEPTIONS.equals(event.getKey())) {
        update();
      }
    }
  };

  private UIElement uiElement;

  public ToggleBreakOnExceptionsHandler() {
    DartDebugCorePlugin.getPlugin().getPrefs().addPreferenceChangeListener(listener);
  }

  @Override
  public void dispose() {
    uiElement = null;

    DartDebugCorePlugin.getPlugin().getPrefs().removePreferenceChangeListener(listener);

    super.dispose();
  }

  @Override
  public final Object execute(ExecutionEvent event) throws ExecutionException {
    ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(
        ICommandService.class);
    State state = event.getCommand().getState(IMenuStateIds.STYLE);

    if (state == null) {
      throw new ExecutionException("declare a ToggleState with id=STYLE");
    }

    boolean value = !DartDebugCorePlugin.getPlugin().getBreakOnExceptions();
    state.setValue(value);
    executeToggle(event, value);

    commandService.refreshElements(event.getCommand().getId(), null);

    return null;
  }

  /**
   * Update command element with toggle state
   */
  @SuppressWarnings("rawtypes")
  @Override
  public void updateElement(UIElement element, Map parameters) {
    this.uiElement = element;

    element.setChecked(DartDebugCorePlugin.getPlugin().getBreakOnExceptions());
  }

  private void executeToggle(ExecutionEvent event, boolean checked) {
    DartDebugCorePlugin.getPlugin().setBreakOnExceptions(checked);
  }

  private void update() {
    if (uiElement != null) {
      uiElement.setChecked(DartDebugCorePlugin.getPlugin().getBreakOnExceptions());
    }
  }

}
