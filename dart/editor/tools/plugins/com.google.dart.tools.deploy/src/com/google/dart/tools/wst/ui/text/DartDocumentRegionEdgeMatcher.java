/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.wst.ui.text;

import com.google.dart.tools.ui.internal.text.functions.DartPairMatcher;

import org.eclipse.wst.sse.ui.internal.text.DocumentRegionEdgeMatcher;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;

@SuppressWarnings("restriction")
public class DartDocumentRegionEdgeMatcher extends DocumentRegionEdgeMatcher {
  protected final static char[] BRACKETS = {'{', '}', '(', ')', '[', ']', '<', '>'};

  public DartDocumentRegionEdgeMatcher() {
    super(new String[] {
        DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_COMMENT_TEXT,
        DOMRegionContext.XML_CDATA_TEXT, DOMRegionContext.XML_PI_OPEN,
        DOMRegionContext.XML_PI_CONTENT}, new DartPairMatcher(BRACKETS));
  }

}
