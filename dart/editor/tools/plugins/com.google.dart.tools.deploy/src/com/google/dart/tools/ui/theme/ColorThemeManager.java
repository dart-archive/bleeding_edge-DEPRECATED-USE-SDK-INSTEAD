/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.theme;

import com.google.dart.tools.deploy.Activator;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.theme.mapper.GenericMapper;
import com.google.dart.tools.ui.theme.mapper.ThemePreferenceMapper;
import com.google.dart.tools.ui.theme.preferences.ThemePreferencePage;

import static com.google.dart.tools.ui.theme.ColorThemeKeys.BACKGROUND;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.CURRENT_LINE;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.DARTDOC;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.DARTDOC_KEYWORD;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.DARTDOC_LINK;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.DARTDOC_TAG;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.DEBUG_CURRENT_INSTRUCTION_POINTER;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.DEBUG_SECONDARY_INSTRUCTION_POINTER;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.FIELD;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.FOREGROUND;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.LOCAL_VARIABLE;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.METHOD;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.MULTI_LINE_COMMENT;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.OCCURRENCE_INDICATION;
import static com.google.dart.tools.ui.theme.ColorThemeKeys.WRITE_OCCURRENCE_INDICATION;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Load and apply color themes.
 * 
 * @see com.github.eclipsecolortheme.ColorThemeManager
 */
public class ColorThemeManager {

  private static class IEclipsePreferencesAdapter extends DartEditor.EclipsePreferencesAdapter {
    private IEclipsePreferences store;

    IEclipsePreferencesAdapter(IEclipsePreferences prefs) {
      super(null, null);
      store = prefs;
    }

    @Override
    protected IEclipsePreferences getNode() {
      return store;
    }
  }

  public static ColorTheme parseTheme(InputStream input) throws ParserConfigurationException,
      SAXException, IOException {
    ColorTheme theme = new ColorTheme();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(input);
    Element root = document.getDocumentElement();
    theme.setId(root.getAttribute("id")); // $NON-NLS-1$
    theme.setName(root.getAttribute("name")); // $NON-NLS-1$
    theme.setAuthor(root.getAttribute("author")); // $NON-NLS-1$
    theme.setWebsite(root.getAttribute("website")); // $NON-NLS-1$

    Map<String, ColorThemeSetting> entries = new HashMap<String, ColorThemeSetting>();
    NodeList entryNodes = root.getChildNodes();
    for (int i = 0; i < entryNodes.getLength(); i++) {
      Node entryNode = entryNodes.item(i);
      if (entryNode.hasAttributes()) {
        String color = entryNode.getAttributes().getNamedItem("color").getNodeValue(); // $NON-NLS-1$
        Node nodeBold = entryNode.getAttributes().getNamedItem("bold"); // $NON-NLS-1$
        Node nodeItalic = entryNode.getAttributes().getNamedItem("italic"); // $NON-NLS-1$
        Node nodeUnderline = entryNode.getAttributes().getNamedItem("underline"); // $NON-NLS-1$
        Node nodeStrikethrough = entryNode.getAttributes().getNamedItem("strikethrough"); // $NON-NLS-1$
        ColorThemeSetting setting = new ColorThemeSetting(color);
        if (nodeBold != null) {
          setting.setBoldEnabled(Boolean.parseBoolean(nodeBold.getNodeValue()));
        }
        if (nodeItalic != null) {
          setting.setItalicEnabled(Boolean.parseBoolean(nodeItalic.getNodeValue()));
        }
        if (nodeStrikethrough != null) {
          setting.setStrikethroughEnabled(Boolean.parseBoolean(nodeStrikethrough.getNodeValue()));
        }
        if (nodeUnderline != null) {
          setting.setUnderlineEnabled(Boolean.parseBoolean(nodeUnderline.getNodeValue()));
        }
        entries.put(entryNode.getNodeName(), setting);
      }
    }
    theme.setEntries(entries);

    return theme;
  }

  private static void amendThemeEntries(Map<String, ColorThemeSetting> theme) {
    applyDefault(theme, METHOD, FOREGROUND);
    applyDefault(theme, FIELD, FOREGROUND);
    applyDefault(theme, LOCAL_VARIABLE, FOREGROUND);
    applyDefault(theme, DARTDOC, MULTI_LINE_COMMENT);
    applyDefault(theme, DARTDOC_LINK, DARTDOC);
    applyDefault(theme, DARTDOC_TAG, DARTDOC);
    applyDefault(theme, DARTDOC_KEYWORD, DARTDOC);
    applyDefault(theme, OCCURRENCE_INDICATION, BACKGROUND);
    applyDefault(theme, WRITE_OCCURRENCE_INDICATION, OCCURRENCE_INDICATION);
    applyDefault(theme, DEBUG_CURRENT_INSTRUCTION_POINTER, CURRENT_LINE);
    applyDefault(theme, DEBUG_SECONDARY_INSTRUCTION_POINTER, CURRENT_LINE);
  }

  private static void applyDefault(Map<String, ColorThemeSetting> theme, ColorThemeKeys key,
      ColorThemeKeys defaultKey) {
    if (!theme.containsKey(key)) {
      theme.put(key.name, theme.get(defaultKey.name));
    }
  }

  private static IPreferenceStore getPreferenceStore() {
    return ThemePreferencePage.globalPreferences();
  }

  private static void readImportedThemes(Map<String, ColorTheme> themes) {
    IPreferenceStore store = getPreferenceStore();

    for (int i = 1;; i++) {
      String xml = store.getString("importedColorTheme" + i); // $NON-NLS-1$
      if (xml == null || xml.length() == 0) {
        break;
      }
      try {
        ColorTheme theme = parseTheme(new ByteArrayInputStream(xml.getBytes()));
        amendThemeEntries(theme.getEntries());
        themes.put(theme.getName(), theme);
      } catch (Exception e) {
        // TODO(messick): Add proper error reporting
        System.err.println("Error while parsing imported theme"); // $NON-NLS-1$
        e.printStackTrace();
      }
    }
  }

  private static void readStockThemes(Map<String, ColorTheme> themes) {
    IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(
        Activator.EXTENSION_POINT_ID_THEME);
    try {
      for (IConfigurationElement e : config) {
        String xml = e.getAttribute("file"); // $NON-NLS-1$
        String contributorPluginId = e.getContributor().getName();
        Bundle bundle = Platform.getBundle(contributorPluginId);
        InputStream input = (InputStream) bundle.getResource(xml).getContent();
        ColorTheme theme = parseTheme(input);
        amendThemeEntries(theme.getEntries());
        themes.put(theme.getName(), theme);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Map<String, ColorTheme> themes;
  private Set<ThemePreferenceMapper> editors;
  private WorkingCopyManager preferenceManager;

  /** Creates a new color theme manager. */
  public ColorThemeManager() {
    preferenceManager = new WorkingCopyManager();
    themes = new HashMap<String, ColorTheme>();
    readStockThemes(themes);
    readImportedThemes(themes);

    editors = new HashSet<ThemePreferenceMapper>();
    IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(
        Activator.EXTENSION_POINT_ID_MAPPER);
    try {
      for (IConfigurationElement e : config) {
        final Object o = e.createExecutableExtension("class"); // $NON-NLS-1$
        if (o instanceof ThemePreferenceMapper) {
          String pluginId = e.getAttribute("pluginId"); // $NON-NLS-1$
          ThemePreferenceMapper mapper = (ThemePreferenceMapper) o;
          mapper.setPluginId(pluginId, preferenceManager);
          if (o instanceof GenericMapper) {
            String xml = e.getAttribute("xml"); // $NON-NLS-1$
            String contributorPluginId = e.getContributor().getName();
            Bundle bundle = Platform.getBundle(contributorPluginId);
            InputStream input = (InputStream) bundle.getResource(xml).getContent();
            ((GenericMapper) mapper).parseMapping(input);
          }
          editors.add(mapper);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Changes the preferences of other plugins to apply the color theme.
   * 
   * @param theme The name of the color theme to apply.
   */
  public void applyTheme(String theme) {
    for (ThemePreferenceMapper editor : editors) {
      applyThemeIn(theme, editor);
    }
  }

  public void clearImportedThemes() {
    IPreferenceStore store = getPreferenceStore();
    for (int i = 1; store.contains("importedColorTheme" + i); i++) { // $NON-NLS-1$
      store.setToDefault("importedColorTheme" + i); // $NON-NLS-1$
    }
    themes.clear();
    readStockThemes(themes);
  }

  public IPreferenceStore createCombinedPreferenceStore() {
    IPreferenceStore[] prefs = new IPreferenceStore[editors.size()];
    int i = 0;
    for (ThemePreferenceMapper editor : editors) {
      IEclipsePreferences pref = editor.getPreviewPreferences();
      prefs[i++] = new IEclipsePreferencesAdapter(pref);
    }
    final ChainedPreferenceStore chain = new ChainedPreferenceStore(prefs);
    for (ThemePreferenceMapper editor : editors) {
      IEclipsePreferences pref = editor.getPreviewPreferences();
      pref.addPreferenceChangeListener(new IPreferenceChangeListener() {
        @Override
        public void preferenceChange(PreferenceChangeEvent event) {
          chain.firePropertyChangeEvent(event.getKey(), event.getOldValue(), event.getNewValue());
        }
      });
    }
    return chain;
  }

  /**
   * Returns the theme stored under the supplied name.
   * 
   * @param name The name of the theme.
   * @return The requested theme or <code>null</code> if none was stored under the supplied name.
   */
  public ColorTheme getTheme(String name) {
    return themes.get(name);
  }

  /**
   * Returns all available color themes.
   * 
   * @return all available color themes.
   */
  public Set<ColorTheme> getThemes() {
    return new HashSet<ColorTheme>(themes.values());
  }

  /**
   * Changes the preferences of the preview to apply the color theme.
   * 
   * @param theme The name of the color theme to apply.
   */
  public void previewTheme(final String theme) {
    for (final ThemePreferenceMapper editor : editors) {
      editor.previewRun(new Runnable() {
        @Override
        public void run() {
          applyThemeIn(theme, editor);
        }
      });
    }
  }

  /**
   * Adds the color theme to the list and saves it to the preferences. Existing themes will be
   * overwritten with the new content.
   * 
   * @param content The content of the color theme file.
   * @return The saved color theme, or <code>null</code> if the theme was not valid.
   */
  public ColorTheme saveTheme(String content) {
    ColorTheme theme;
    try {
      theme = ColorThemeManager.parseTheme(new ByteArrayInputStream(content.getBytes()));
      String name = theme.getName();
      themes.put(name, theme);
      IPreferenceStore store = getPreferenceStore();
      for (int i = 1;; i++) {
        if (!store.contains("importedColorTheme" + i)) { // $NON-NLS-1$
          store.putValue("importedColorTheme" + i, content); // $NON-NLS-1$
          break;
        }
      }
      return theme;
    } catch (Exception e) {
      return null;
    }
  }

  private void applyThemeIn(String theme, ThemePreferenceMapper editor) {
    editor.clear();
    if (themes.get(theme) != null) {
      editor.map(themes.get(theme).getEntries());
    }
    try {
      editor.flush();
    } catch (BackingStoreException e) {
      // TODO(messick): Show a proper error message.
      e.printStackTrace();
    }
  }
}
