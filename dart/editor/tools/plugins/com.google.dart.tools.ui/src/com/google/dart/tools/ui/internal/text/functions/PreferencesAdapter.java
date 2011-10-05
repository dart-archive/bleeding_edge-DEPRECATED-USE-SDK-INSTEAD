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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Adapts {@link org.eclipse.core.runtime.Preferences} to
 * {@link org.eclipse.jface.preference.IPreferenceStore}
 */
public class PreferencesAdapter implements IPreferenceStore {

  /**
   * Property change listener. Listens for events of type
   * {@link org.eclipse.core.runtime.Preferences.PropertyChangeEvent} and fires a
   * {@link org.eclipse.jface.util.PropertyChangeEvent} on the adapter with arguments from the
   * received event.
   */
  private class PropertyChangeListener implements Preferences.IPropertyChangeListener {

    /*
     * @see org.eclipse.core.runtime.Preferences.IPropertyChangeListener#propertyChange
     * (org.eclipse.core.runtime.Preferences.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(Preferences.PropertyChangeEvent event) {
      firePropertyChangeEvent(event.getProperty(), event.getOldValue(), event.getNewValue());
    }
  }

  /** Listeners on the adapter */
  private ListenerList fListeners = new ListenerList(ListenerList.IDENTITY);

  /** Listener on the adapted Preferences */
  private PropertyChangeListener fListener = new PropertyChangeListener();

  /** Adapted Preferences */
  private Preferences fPreferences;

  /** True iff no events should be forwarded */
  private boolean fSilent;

  /**
   * Initialize with empty Preferences.
   */
  public PreferencesAdapter() {
    this(new Preferences());
  }

  /**
   * Initialize with the given Preferences.
   * 
   * @param preferences The preferences to wrap.
   */
  public PreferencesAdapter(Preferences preferences) {
    fPreferences = preferences;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addPropertyChangeListener(IPropertyChangeListener listener) {
    if (fListeners.size() == 0) {
      fPreferences.addPropertyChangeListener(fListener);
    }
    fListeners.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean contains(String name) {
    return fPreferences.contains(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
    if (!fSilent) {
      final PropertyChangeEvent event = new PropertyChangeEvent(this, name, oldValue, newValue);
      Object[] listeners = fListeners.getListeners();
      for (int i = 0; i < listeners.length; i++) {
        final IPropertyChangeListener listener = (IPropertyChangeListener) listeners[i];
        Runnable runnable = new Runnable() {
          @Override
          public void run() {
            listener.propertyChange(event);
          }
        };

        if (Display.getCurrent() != null) {
          runnable.run();
        } else {
          // Post runnable into UI thread
          Shell shell = DartToolsPlugin.getActiveWorkbenchShell();
          Display display;
          if (shell != null) {
            display = shell.getDisplay();
          } else {
            display = Display.getDefault();
          }
          display.asyncExec(runnable);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getBoolean(String name) {
    return fPreferences.getBoolean(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getDefaultBoolean(String name) {
    return fPreferences.getDefaultBoolean(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getDefaultDouble(String name) {
    return fPreferences.getDefaultDouble(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public float getDefaultFloat(String name) {
    return fPreferences.getDefaultFloat(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getDefaultInt(String name) {
    return fPreferences.getDefaultInt(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getDefaultLong(String name) {
    return fPreferences.getDefaultLong(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDefaultString(String name) {
    return fPreferences.getDefaultString(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getDouble(String name) {
    return fPreferences.getDouble(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public float getFloat(String name) {
    return fPreferences.getFloat(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getInt(String name) {
    return fPreferences.getInt(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getLong(String name) {
    return fPreferences.getLong(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getString(String name) {
    return fPreferences.getString(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDefault(String name) {
    return fPreferences.isDefault(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean needsSaving() {
    return fPreferences.needsSaving();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putValue(String name, String value) {
    try {
      fSilent = true;
      fPreferences.setValue(name, value);
    } finally {
      fSilent = false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removePropertyChangeListener(IPropertyChangeListener listener) {
    fListeners.remove(listener);
    if (fListeners.size() == 0) {
      fPreferences.removePropertyChangeListener(fListener);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDefault(String name, boolean value) {
    fPreferences.setDefault(name, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDefault(String name, double value) {
    fPreferences.setDefault(name, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDefault(String name, float value) {
    fPreferences.setDefault(name, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDefault(String name, int value) {
    fPreferences.setDefault(name, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDefault(String name, long value) {
    fPreferences.setDefault(name, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDefault(String name, String defaultObject) {
    fPreferences.setDefault(name, defaultObject);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setToDefault(String name) {
    fPreferences.setToDefault(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setValue(String name, boolean value) {
    fPreferences.setValue(name, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setValue(String name, double value) {
    fPreferences.setValue(name, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setValue(String name, float value) {
    fPreferences.setValue(name, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setValue(String name, int value) {
    fPreferences.setValue(name, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setValue(String name, long value) {
    fPreferences.setValue(name, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setValue(String name, String value) {
    fPreferences.setValue(name, value);
  }
}
