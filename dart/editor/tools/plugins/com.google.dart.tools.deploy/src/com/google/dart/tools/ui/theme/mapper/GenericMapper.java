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
package com.google.dart.tools.ui.theme.mapper;

import com.google.dart.tools.ui.theme.ColorThemeMapping;
import com.google.dart.tools.ui.theme.ColorThemeSemanticHighlightingMapping;
import com.google.dart.tools.ui.theme.ColorThemeSetting;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @see com.github.eclipsecolortheme.mapper.GenericMapper
 */
public class GenericMapper extends ThemePreferenceMapper {

  private static String extractAttribute(Node node, String name) {
    return node.getAttributes().getNamedItem(name).getNodeValue();
  }

  private Map<String, ColorThemeMapping> mappings = new HashMap<String, ColorThemeMapping>();

  @Override
  public void clear() {
    for (String pluginKey : mappings.keySet()) {
      ColorThemeMapping mapping = mappings.get(pluginKey);
      mapping.removePreferences(preferences);
    }
  }

  @Override
  public void map(Map<String, ColorThemeSetting> theme) {
    // put preferences according to mappings
    for (String pluginKey : mappings.keySet()) {
      ColorThemeMapping mapping = mappings.get(pluginKey);
      ColorThemeSetting setting = theme.get(mapping.getThemeKey());
      if (setting != null) {
        mapping.putPreferences(preferences, setting);
      }
    }
  }

  /**
   * Parse mapping from input file.
   * 
   * @param input InputStream for an XML file
   * @throws SAXException
   * @throws IOException
   * @throws ParserConfigurationException
   */
  public void parseMapping(InputStream input) throws SAXException, IOException,
      ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(input);
    Element root = document.getDocumentElement();
    parseMappings(root);
    parseSemanticHighlightingMappings(root);
  }

  protected ColorThemeMapping createMapping(String pluginKey, String themeKey) {
    return new ColorThemeMapping(pluginKey, themeKey);
  }

  protected ColorThemeSemanticHighlightingMapping createSemanticHighlightingMapping(
      String pluginKey, String themeKey) {
    return new ColorThemeSemanticHighlightingMapping(pluginKey, themeKey);
  }

  private void parseMappings(Element root) {
    Node mappingsNode = root.getElementsByTagName("mappings").item(0); // $NON-NLS-1$
    NodeList mappingNodes = mappingsNode.getChildNodes();
    for (int i = 0; i < mappingNodes.getLength(); i++) {
      Node mappingNode = mappingNodes.item(i);
      if (mappingNode.hasAttributes()) {
        String pluginKey = extractAttribute(mappingNode, "pluginKey"); // $NON-NLS-1$
        String themeKey = extractAttribute(mappingNode, "themeKey"); // $NON-NLS-1$
        mappings.put(pluginKey, createMapping(pluginKey, themeKey));
      }
    }
  }

  private void parseSemanticHighlightingMappings(Element root) {
    Node mappingsNode = root.getElementsByTagName("semanticHighlightingMappings").item(0); // $NON-NLS-1$
    if (mappingsNode != null) {
      NodeList mappingNodes = mappingsNode.getChildNodes();
      for (int i = 0; i < mappingNodes.getLength(); i++) {
        Node mappingNode = mappingNodes.item(i);
        if (mappingNode.hasAttributes()) {
          String pluginKey = extractAttribute(mappingNode, "pluginKey"); // $NON-NLS-1$
          String themeKey = extractAttribute(mappingNode, "themeKey"); // $NON-NLS-1$
          mappings.put(pluginKey, createSemanticHighlightingMapping(pluginKey, themeKey));
        }
      }
    }
  }
}
