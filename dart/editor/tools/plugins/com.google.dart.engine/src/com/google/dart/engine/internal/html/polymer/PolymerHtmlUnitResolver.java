/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.engine.internal.html.polymer;

import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.RecursiveXmlVisitor;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;

/**
 * Instances of the class {@link PolymerHtmlUnitResolver} resolve Polymer specific
 * {@link XmlTagNode}s and expressions.
 * <p>
 * TODO(scheglov) implement it
 */
public class PolymerHtmlUnitResolver extends RecursiveXmlVisitor<Void> {
  private final InternalAnalysisContext context;
  private final TypeProvider typeProvider;
  private final AnalysisErrorListener errorListener;

  private final Source source;
  private final LineInfo lineInfo;
  private final HtmlUnit unit;

  public PolymerHtmlUnitResolver(InternalAnalysisContext context,
      AnalysisErrorListener errorListener, Source source, LineInfo lineInfo, HtmlUnit unit)
      throws AnalysisException {
    this.context = context;
    this.typeProvider = context.getTypeProvider();
    this.errorListener = errorListener;
    this.source = source;
    this.lineInfo = lineInfo;
    this.unit = unit;
  }

  /**
   * Resolves Polymer specific features.
   */
  public void resolveUnit() throws AnalysisException {
    // TODO(scheglov) implement it
//    unit.accept(this);
  }

  @Override
  public Void visitXmlAttributeNode(XmlAttributeNode node) {
    // TODO(scheglov) implement it
    return super.visitXmlAttributeNode(node);
  }

  @Override
  public Void visitXmlTagNode(XmlTagNode node) {
    // TODO(scheglov) implement it
    // visit children
    return super.visitXmlTagNode(node);
  }
}
