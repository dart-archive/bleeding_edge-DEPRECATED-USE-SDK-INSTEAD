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

package com.google.dart.tools.core.html;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class to pull Dart scripts out of an html file.
 */
public class DartHtmlScriptHelper {

  /**
   * Parse the given html file and return the content and location of the "application/dart"
   * scripts.
   * 
   * @param file
   * @return
   * @throws IOException
   */
  public static List<Token> getDartScripts(File file) throws IOException {
    return getDartScripts(Files.toString(file, Charsets.UTF_8));
  }

  /**
   * Parse the given html file and return the content and location of the "application/dart"
   * scripts.
   * 
   * @param file
   * @return
   * @throws IOException
   */
  public static List<Token> getDartScripts(String data) {
    HtmlParser parser = new HtmlParser(data);

    XmlDocument document = parser.parse();

    List<Token> scripts = new ArrayList<Token>();

    for (XmlNode child : document.getChildren()) {
      if (child instanceof XmlElement) {
        locateScripts((XmlElement) child, scripts);
      }
    }

    return scripts;
  }

  private static void locateScripts(XmlElement node, List<Token> scripts) {
    if ("script".equals(node.getLabel())) {
      String type = node.getAttributeString("type");

      if ("application/dart".equalsIgnoreCase(type)) {
        Token endToken = node.getEndToken();
        Token token = new Token(
            node.getContents(),
            endToken.getLocation() + 1,
            endToken.getLineNumber());
        scripts.add(token);
      }
    }

    for (XmlNode child : node.getChildren()) {
      if (child instanceof XmlElement) {
        locateScripts((XmlElement) child, scripts);
      }
    }
  }

}
