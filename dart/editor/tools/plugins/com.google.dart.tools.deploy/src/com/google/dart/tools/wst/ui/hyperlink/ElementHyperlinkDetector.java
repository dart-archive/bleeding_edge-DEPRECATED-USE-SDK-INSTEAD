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
  public static ElementRegion getElementRegion(HtmlUnit htmlUnit, int offset) {
    Element element;
    Region region;
    // try attribute
    {
      XmlAttributeNode attrNode = HtmlUnitUtils.getAttributeNode(htmlUnit, offset);
      if (attrNode != null) {
        element = attrNode.getElement();
        Token nameToken = attrNode.getNameToken();
        region = new Region(nameToken.getOffset(), nameToken.getLength());
        if (element != null) {
          return new ElementRegion(element, region);
        }
        return null;
      }
    }
    // try tag
    {
      XmlTagNode tagNode = HtmlUnitUtils.getTagNode(htmlUnit, offset);
      if (tagNode != null) {
        element = tagNode.getElement();
        Token tagToken = tagNode.getTagToken();
        region = new Region(tagToken.getOffset(), tagToken.getLength());
        if (element != null) {
          return new ElementRegion(element, region);
        }
        return null;
      }
    }
    // try expression
    {
      Expression expression = HtmlUnitUtils.getExpression(htmlUnit, offset);
      if (expression != null) {
        element = HtmlUnitUtils.getElementToOpen(htmlUnit, expression);
        region = new Region(expression.getOffset(), expression.getLength());
        if (element != null) {
          return new ElementRegion(element, region);
        }
        return null;
      }
    }
    // no region
    return null;
  }

  @Override
  public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
      boolean canShowMultipleHyperlinks) {
    HtmlUnit htmlUnit = getHtmlUnit(textViewer);
    if (htmlUnit == null) {
      return null;
    }
    // prepare target Expression and Element
    int offset = region.getOffset();
    ElementRegion elementRegion = getElementRegion(htmlUnit, offset);
    // create Element hyperlink
    if (elementRegion != null) {
      return new IHyperlink[] {new ElementHyperlink(elementRegion.region, elementRegion.element)};
    }
    return null;
  }

  private HtmlUnit getHtmlUnit(ITextViewer textViewer) {
    IDocument document = textViewer.getDocument();
    HtmlReconcilerHook reconciler = HtmlReconcilerManager.getInstance().reconcilerFor(document);
    return reconciler.getResolvedUnit();
  }
}
