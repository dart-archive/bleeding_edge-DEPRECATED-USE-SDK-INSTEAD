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

package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.type.Type;
import com.google.dart.tools.core.utilities.dartdoc.DartDocUtilities;
import com.google.dart.tools.ui.internal.actions.NewSelectionConverter;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;

import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of ITextHover for Dart documentation. Other ITextHover implementations can
 * register themselves using {@link #addContributer(ITextHover)} and they will be invoked before the
 * default dart doc hover tool tip.
 */
public class DartTextHover extends DefaultTextHover implements ITextHoverExtension,
    ITextHoverExtension2 {
  private static List<ITextHover> hoverContributors = new ArrayList<ITextHover>();

  /**
   * Register a ITextHover tooltip contributor
   * 
   * @param hoverContributor
   */
  public static void addContributer(ITextHover hoverContributor) {
    hoverContributors.add(hoverContributor);
  }

  public static String getElementDocumentationHtml(Type type, Element element) {
    if (element != null) {
      String textSummary = DartDocUtilities.getTextSummaryAsHtml(type, element);

      if (textSummary != null) {

        StringBuffer docs = new StringBuffer();
        docs.append("<b>" + textSummary + "</b>");

        String dartdoc = DartDocUtilities.getDartDocAsHtml(element);

        if (dartdoc != null) {
          docs.append("<br><br>");
          docs.append(dartdoc);
        }

        return docs.toString().trim();
      }
    }
    return null;
  }

  /**
   * Remove a hover contributor
   * 
   * @param hoverContributor
   */
  public static void removeContributer(ITextHover hoverContributor) {
    hoverContributors.remove(hoverContributor);
  }

  private static StringBuilder append(StringBuilder buffer, String s) {
    if (buffer.length() != 0) {
      buffer.append("<br><br>");
    }
    if (s != null) {
      buffer.append(s);
    }
    return buffer;
  }

  private CompilationUnitEditor editor;

  private DartSourceViewerConfiguration sourceViewerConfiguration;

  private ITextHover lastReturnedHover;

  public DartTextHover(ITextEditor editor, ISourceViewer sourceViewer,
      DartSourceViewerConfiguration sourceViewerConfiguration) {
    super(sourceViewer);

    if (editor instanceof CompilationUnitEditor) {
      this.editor = (CompilationUnitEditor) editor;
    }

    this.sourceViewerConfiguration = sourceViewerConfiguration;
  }

  @Override
  public IInformationControlCreator getHoverControlCreator() {
    if (lastReturnedHover instanceof ITextHoverExtension) {
      return ((ITextHoverExtension) lastReturnedHover).getHoverControlCreator();
    } else {
      return null;
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public String getHoverInfo(ITextViewer textViewer, IRegion region) {
    // Return any annotation info - i.e. errors and warnings.
    String annotationHover = super.getHoverInfo(textViewer, region);

    if (annotationHover != null) {
      return escapeHtmlEntities(annotationHover);
    }

    // Check through the contributed hover providers.
    for (ITextHover hoverContributer : hoverContributors) {
      String hoverText = hoverContributer.getHoverInfo(textViewer, region);

      if (hoverText != null) {
        lastReturnedHover = hoverContributer;

        return hoverText;
      }
    }

    // Check for a dartdoc contribution.
    return getDartDocHover(region);
  }

  @Override
  public Object getHoverInfo2(ITextViewer textViewer, IRegion region) {
    // Overridden from ITextHoverExtension2. We try and return the richest help available; this
    // means trying to call getHoverInfo2() on any contributors, and falling back on getHoverInfo().
    lastReturnedHover = null;

    StringBuilder buffer = new StringBuilder();

    // Append any annotation info - i.e. errors and warnings.
    String annotationHover = super.getHoverInfo(textViewer, region);
    if (annotationHover != null) {
      append(buffer, escapeHtmlEntities(annotationHover));
    }

    // Check through the contributed hover providers.
    for (ITextHover hoverContributer : hoverContributors) {
      if (hoverContributer instanceof ITextHoverExtension2) {
        Object hoverInfo = ((ITextHoverExtension2) hoverContributer).getHoverInfo2(
            textViewer,
            region);

        if (hoverInfo != null) {
          lastReturnedHover = hoverContributer;
          return hoverInfo;
        }
      } else {
        String hoverText = hoverContributer.getHoverInfo(textViewer, region);

        if (hoverText != null) {
          lastReturnedHover = hoverContributer;
          return hoverText;
        }
      }
    }

    // Check for a dartdoc contribution.
    String dartDocHover = getDartDocHover(region);
    return append(buffer, dartDocHover).toString();
  }

  @Override
  protected boolean isIncluded(Annotation annotation) {
    return sourceViewerConfiguration.isShownInText(annotation);
  }

  private String escapeHtmlEntities(String str) {
    str = str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

    return str;
  }

  /**
   * Return the associated DartDoc hover, if any.
   */
  private String getDartDocHover(IRegion region) {
    if (editor != null) {
      int offset = region.getOffset();
      AstNode node = NewSelectionConverter.getNodeAtOffset(editor, offset);
      if (node == null) {
        return null;
      }
      Type type = node instanceof Expression ? ((Expression) node).getBestType() : null;
      Element element = ElementLocator.locateWithOffset(node, offset);
      return getElementDocumentationHtml(type, element);
    }

    return null;
  }

}
