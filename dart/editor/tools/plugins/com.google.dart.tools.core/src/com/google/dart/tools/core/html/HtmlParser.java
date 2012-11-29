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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A parser for html files; it generates as AST tree rooted in the XmlDocument class.
 * 
 * @see XmlDocument
 */
public class HtmlParser extends XmlParser {
  private static Set<String> SELF_CLOSING = new HashSet<String>(Arrays.asList(new String[] {
      "area", "base", "basefont", "br", "col", "frame", "hr", "img", "input", "link", "meta",
      "param", "!"}));

  public HtmlParser(String data) {
    super(data);
  }

  @Override
  protected String[] getPassThroughElements() {
    return new String[] {"script", "</"};
  }

  @Override
  protected boolean isSelfClosing(String entityName) {
    return SELF_CLOSING.contains(entityName);
  }

}
