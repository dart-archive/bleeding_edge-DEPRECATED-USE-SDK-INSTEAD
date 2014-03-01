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
package com.google.dart.tools.wst.ui.style;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlighting;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightingManager.Highlighting;
import com.google.dart.tools.ui.internal.text.editor.SemanticToken;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;

import java.util.Collection;
import java.util.List;

/**
 * Analyze a document to produce semantic highlighting ranges. Adapted from
 * SemanticHighlightingRecolciler.
 */
public class SemanticHighlightingEngine {

  /**
   * Collects styled text regions from the AST for highlighting.
   */
  private class PositionCollector extends GeneralizingAstVisitor<Void> {
    /**
     * Cache tokens for performance.
     */
    private SemanticToken token = new SemanticToken();
    private Collection<StyleRange> positions;

    public PositionCollector(Collection<StyleRange> positions) {
      this.positions = positions;
    }

    @Override
    public Void visitNode(AstNode node) {
      processNode(token, node);
      return super.visitNode(node);
    }

    /**
     * Add a position with the given range and highlighting iff it does not exist already.
     * 
     * @param offset The range offset
     * @param length The range length
     * @param highlighting The highlighting
     */
    protected void addPosition(int offset, int length, Highlighting highlighting) {
      TextAttribute attr = highlighting.getTextAttribute();
      Color fore = attr.getForeground();
      Color back = attr.getBackground();
      int style = attr.getStyle();
      StyleRange range = new StyleRange(offset + deltaOffset, length, fore, back, style);
      if ((attr.getStyle() & TextAttribute.STRIKETHROUGH) != 0) {
        range.strikeout = true;
      }
      if ((attr.getStyle() & TextAttribute.UNDERLINE) != 0) {
        range.underline = true;
      }
      positions.add(range);
    }
  }

  private IDocument document;
  private int deltaOffset;
  private SemanticHighlighting[] semanticHighlightings;
  private Highlighting[] highlightings;
  private PositionCollector collector;

  public SemanticHighlightingEngine(SemanticHighlighting[] semanticHighlightings,
      Highlighting[] highlightings) {
    this.semanticHighlightings = semanticHighlightings;
    this.highlightings = highlightings;
  }

  public void analyze(IDocument document, int deltaOffset, AstNode node,
      Collection<StyleRange> positions) {
    this.document = document;
    this.deltaOffset = deltaOffset;
    this.collector = new PositionCollector(positions);
    node.accept(this.collector);
  }

  private final void processNode(SemanticToken token, AstNode node) {
    // update token
    token.update(node);
    token.attachSource(document);
    // try SemanticHighlighting instances
    for (int i = 0, n = semanticHighlightings.length; i < n; i++) {
      if (highlightings[i].isEnabled()) {
        SemanticHighlighting semanticHighlighting = semanticHighlightings[i];
        // try multiple positions
        {
          List<SourceRange> ranges = semanticHighlighting.consumesMulti(token);
          if (ranges != null) {
            for (SourceRange range : ranges) {
              int offset = range.getOffset();
              int length = range.getLength();
              if (offset > -1 && length > 0) {
                collector.addPosition(offset, length, highlightings[i]);
              }
            }
            break;
          }
        }
        // try single position
        boolean consumes;
        if (node instanceof SimpleIdentifier) {
          consumes = semanticHighlighting.consumesIdentifier(token);
        } else {
          consumes = semanticHighlighting.consumes(token);
        }
        if (consumes) {
          int offset = node.getOffset();
          int length = node.getLength();
          if (offset > -1 && length > 0) {
            collector.addPosition(offset, length, highlightings[i]);
          }
          break;
        }
      }
    }
    token.clear();
  }

}
