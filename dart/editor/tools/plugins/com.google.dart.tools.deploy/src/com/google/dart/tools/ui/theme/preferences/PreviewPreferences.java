package com.google.dart.tools.ui.theme.preferences;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides just enough preference management to update the preview in the preference page, without
 * also updating all open editors.
 */
public class PreviewPreferences implements IEclipsePreferences {

  private Map<String, String> store = new HashMap<String, String>();
  private List<IEclipsePreferences.IPreferenceChangeListener> listeners = new ArrayList<IEclipsePreferences.IPreferenceChangeListener>();;

  @Override
  public String absolutePath() {
    return null;
  }

  @Override
  public void accept(IPreferenceNodeVisitor visitor) throws BackingStoreException {
  }

  @Override
  public void addNodeChangeListener(INodeChangeListener listener) {
  }

  @Override
  public void addPreferenceChangeListener(IPreferenceChangeListener listener) {
    listeners.add(listener);
  }

  @Override
  public String[] childrenNames() throws BackingStoreException {
    return null;
  }

  @Override
  public void clear() throws BackingStoreException {
    store.clear();
  }

  @Override
  public void flush() throws BackingStoreException {
  }

  @Override
  public String get(String key, String def) {
    if (store.containsKey(key)) {
      return store.get(key);
    } else {
      return def;
    }
  }

  @Override
  public boolean getBoolean(String key, boolean def) {
    return Boolean.parseBoolean(get(key, String.valueOf(def)));
  }

  @Override
  public byte[] getByteArray(String key, byte[] def) {
    return null;
  }

  @Override
  public double getDouble(String key, double def) {
    return 0;
  }

  @Override
  public float getFloat(String key, float def) {
    return 0;
  }

  @Override
  public int getInt(String key, int def) {
    return 0;
  }

  @Override
  public long getLong(String key, long def) {
    return 0;
  }

  @Override
  public String[] keys() throws BackingStoreException {
    return store.keySet().toArray(new String[0]);
  }

  @Override
  public String name() {
    return null;
  }

  @Override
  public Preferences node(String path) {
    return null;
  }

  @Override
  public boolean nodeExists(String pathName) throws BackingStoreException {
    return false;
  }

  @Override
  public Preferences parent() {
    return null;
  }

  @Override
  public void put(String key, String value) {
    Object oldValue = store.get(key);
    store.put(key, value);
    firePreferenceEvent(key, oldValue, value);
  }

  @Override
  public void putBoolean(String key, boolean value) {
    put(key, String.valueOf(value));
  }

  @Override
  public void putByteArray(String key, byte[] value) {
  }

  @Override
  public void putDouble(String key, double value) {
  }

  @Override
  public void putFloat(String key, float value) {
  }

  @Override
  public void putInt(String key, int value) {
    put(key, String.valueOf(value));
  }

  @Override
  public void putLong(String key, long value) {
  }

  @Override
  public void remove(String key) {
    store.remove(key);
  }

  @Override
  public void removeNode() throws BackingStoreException {
  }

  @Override
  public void removeNodeChangeListener(INodeChangeListener listener) {
  }

  @Override
  public void removePreferenceChangeListener(IPreferenceChangeListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void sync() throws BackingStoreException {
  }

  /*
   * Convenience method for notifying preference change listeners.
   */
  protected void firePreferenceEvent(String key, Object oldValue, Object newValue) {
    if (listeners == null) {
      return;
    }
    final PreferenceChangeEvent event = new PreferenceChangeEvent(this, key, oldValue, newValue);
    for (int i = 0; i < listeners.size(); i++) {
      final IPreferenceChangeListener listener = listeners.get(i);
      ISafeRunnable job = new ISafeRunnable() {
        @Override
        public void handleException(Throwable exception) {
          // already logged in Platform#run()
        }

        @Override
        public void run() throws Exception {
          listener.preferenceChange(event);
        }
      };
      SafeRunner.run(job);
    }
  }
}
