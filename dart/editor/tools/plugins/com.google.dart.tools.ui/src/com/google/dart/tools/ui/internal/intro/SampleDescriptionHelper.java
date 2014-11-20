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
package com.google.dart.tools.ui.internal.intro;

import com.google.common.collect.Lists;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Helper for providing {@link SampleDescription}s.
 */
public final class SampleDescriptionHelper {
  static final String[] SAMPLE_NAMES = new String[] {
      "dartiverse_search", "pop_pop_win", "sunflower", "todomvc"};

  /**
   * @return all {@link SampleDescription} from the "samples" directory.
   */
  public static List<SampleDescription> getDescriptions() {
    List<SampleDescription> descriptions = Lists.newArrayList();
    scanSamples(SAMPLE_NAMES, descriptions);
    Collections.sort(descriptions);
    return descriptions;
  }

  /**
   * Scans "samples" directory and attempts to find descriptions for each "sample" child.
   */
  static void scanSamples(String[] names, List<SampleDescription> descriptions) {
    for (String sampleName : names) {
      addDescription(sampleName, descriptions);
    }
  }

  /**
   * Attempts to add {@link SampleDescription} for given directory.
   * 
   * @return <code>true</code> if {@link SampleDescription} was added.
   */
  private static void addDescription(final String sampleName,
      final List<SampleDescription> descriptions) {
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      DefaultHandler handler = new DefaultHandler() {
        private StringBuilder sb = new StringBuilder();
        private String name;
        private String descriptionText;
        private String filePath;
        private String url;
        private boolean earlyAccess;

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
          sb.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
          if (qName.equals("description")) {
            descriptionText = sb.toString();
          }

          if (qName.equals("sample") && name != null && descriptionText != null) {
            descriptions.add(new SampleDescription(
                name,
                descriptionText,
                filePath,
                url,
                earlyAccess,
                "samples/" + sampleName + ".png"));
          }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
          sb.setLength(0);
          if (qName.equals("sample")) {
            name = attributes.getValue("name");
            filePath = attributes.getValue("file");
            url = attributes.getValue("url");
            earlyAccess = "true".equals(attributes.getValue("early"));
          }
        }
      };

      InputStream is = getSampleResourceUrl(sampleName + ".xml").openStream();

      try {
        saxParser.parse(is, handler);
      } finally {
        is.close();
      }
    } catch (Throwable e) {
      DartCore.logError(e);
    }
  }

  private static URL getSampleResourceUrl(String resourceName) {
    return DartToolsPlugin.getDefault().getBundle().getEntry("/samples/" + resourceName);
  }
}
