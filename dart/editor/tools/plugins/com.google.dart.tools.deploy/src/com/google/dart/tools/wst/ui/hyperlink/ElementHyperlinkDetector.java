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

package com.google.dart.tools.wst.ui.hyperlink;

import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.HtmlUnitUtils;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.tools.wst.ui.HtmlReconcilerHook;
import com.google.dart.tools.wst.ui.HtmlReconcilerManager;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

public class ElementHyperlinkDetector extends AbstractHyperlinkDetector {
  @Override
  public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
      boolean canShowMultipleHyperlinks) {
    HtmlUnit parseHtmlUnit = getHtmlUnit(textViewer);
    if (parseHtmlUnit == null) {
      return null;
    }
    // prepare target Expression and Element
    int offset = region.getOffset();
    Element element = null;
    Region linkRegion = null;
    // try attribute
    if (element == null) {
      XmlAttributeNode attrNode = HtmlUnitUtils.getAttributeNode(parseHtmlUnit, offset);
      if (attrNode != null) {
        element = attrNode.getElement();
        Token nameToken = attrNode.getNameToken();
        linkRegion = new Region(nameToken.getOffset(), nameToken.getLength());
      }
    }
    // try tag
    if (element == null) {
      XmlTagNode tagNode = HtmlUnitUtils.getTagNode(parseHtmlUnit, offset);
      if (tagNode != null) {
        element = tagNode.getElement();
        Token tagToken = tagNode.getTagToken();
        linkRegion = new Region(tagToken.getOffset(), tagToken.getLength());
      }
    }
    // try expression
    if (element == null) {
      Expression expression = HtmlUnitUtils.getExpression(parseHtmlUnit, offset);
      if (expression != null) {
        element = HtmlUnitUtils.getElementToOpen(parseHtmlUnit, expression);
        linkRegion = new Region(expression.getOffset(), expression.getLength());
      }
    }
    // create Element hyperlink
    if (element != null && linkRegion != null) {
      return new IHyperlink[] {new ElementHyperlink(linkRegion, element)};
    }
    return null;
  }

  private HtmlUnit getHtmlUnit(ITextViewer textViewer) {
    IDocument document = textViewer.getDocument();
    HtmlReconcilerHook reconciler = HtmlReconcilerManager.getInstance().reconcilerFor(document);
    return reconciler.getResolvedUnit();
  }
}
