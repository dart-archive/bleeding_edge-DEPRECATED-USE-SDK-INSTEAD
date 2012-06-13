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

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Helper for providing {@link SampleDescription}s.
 */
public final class SampleDescriptionHelper {
  /**
   * @return all {@link SampleDescription} from the "samples" directory.
   */
  public static List<SampleDescription> getDescriptions() throws Exception {
    List<SampleDescription> descriptions = Lists.newArrayList();
    File samplesDirectory = getSamplesDirectory();
    scanSamples(descriptions, samplesDirectory);
    Collections.sort(descriptions);
    return descriptions;
  }

  /**
   * Scans "samples" directory and attempts to find descriptions for each "sample" child.
   */
  static void scanSamples(List<SampleDescription> descriptions, File samplesDirectory)
      throws Exception {
    // ignore not directory
    if (!samplesDirectory.exists() || !samplesDirectory.isDirectory()) {
      return;
    }

    // scan samples
    for (File sampleDirectory : samplesDirectory.listFiles()) {
      addDescription(descriptions, sampleDirectory);
    }
  }

  /**
   * Attempts to add {@link SampleDescription} for given directory.
   * 
   * @return <code>true</code> if {@link SampleDescription} was added.
   */
  private static void addDescription(final List<SampleDescription> descriptions,
      final File directory) {
    String sampleName = directory.getName();

    if (doesSampleResourceExist(sampleName + ".xml")) {
      try {
        final File logoFile = createSampleImageFile(sampleName + ".png");

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        DefaultHandler handler = new DefaultHandler() {
          private StringBuilder sb = new StringBuilder();
          private String name;
          private String filePath;
          private String keywordText;
          private String descriptionText;

          @Override
          public void characters(char[] ch, int start, int length) throws SAXException {
            sb.append(ch, start, length);
          }

          @Override
          public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("description")) {
              descriptionText = sb.toString();
            }
            if (qName.equals("sample") && filePath != null && name != null
                && descriptionText != null) {
              descriptions.add(new SampleDescription(directory, filePath, name, descriptionText,
                  keywordText, logoFile));
            }
          }

          @Override
          public void startElement(String uri, String localName, String qName, Attributes attributes)
              throws SAXException {
            sb.setLength(0);
            if (qName.equals("sample")) {
              name = attributes.getValue("name");
              filePath = attributes.getValue("file");
              keywordText = attributes.getValue("keywords");
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
  }

  private static File createSampleImageFile(String resourceName) throws IOException {
    try {
      URL entryUrl = getSampleResourceUrl(resourceName);
      URL fileUrl = FileLocator.toFileURL(entryUrl);
      String fileUrlString = fileUrl.toString().replace(" ", "%20");
      URI fileUri = URI.create(fileUrlString);
      return new File(fileUri);
    } catch (Throwable e) {
      throw new IOException(e);
    }
  }

  private static boolean doesSampleResourceExist(String resourceName) {
    return getSampleResourceUrl(resourceName) != null;
  }

  private static URL getSampleResourceUrl(String resourceName) {
    return DartToolsPlugin.getDefault().getBundle().getEntry("/samples/" + resourceName);
  }

  /**
   * @return the {@link File} of the "samples" directory.
   */
  private static File getSamplesDirectory() throws Exception {
    File installDir = new File(Platform.getInstallLocation().getURL().getFile());
    return new File(installDir, "samples");
  }

}
