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

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartDocumentable;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.utilities.dartdoc.DartDocUtilities;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;

import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
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
public class DartTextHover extends DefaultTextHover {
  private CompilationUnitEditor editor;
  private DartSourceViewerConfiguration sourceViewerConfiguration;

  private static List<ITextHover> hoverContributors = new ArrayList<ITextHover>();

  /**
   * Register a ITextHover tooltip contributor
   * 
   * @param hoverContributor
   */
  public static void addContributer(ITextHover hoverContributor) {
    hoverContributors.add(hoverContributor);
  }

  /**
   * Remove a hover contributor
   * 
   * @param hoverContributor
   */
  public static void removeContributer(ITextHover hoverContributor) {
    hoverContributors.remove(hoverContributor);
  }

  public DartTextHover(ITextEditor editor, ISourceViewer sourceViewer,
      DartSourceViewerConfiguration sourceViewerConfiguration) {
    super(sourceViewer);

    if (editor instanceof CompilationUnitEditor) {
      this.editor = (CompilationUnitEditor) editor;
    }

    this.sourceViewerConfiguration = sourceViewerConfiguration;
  }

  @SuppressWarnings("deprecation")
  @Override
  public String getHoverInfo(ITextViewer textViewer, IRegion region) {
    String annotationHover = super.getHoverInfo(textViewer, region);

    if (annotationHover != null) {
      return annotationHover;
    }

    if (editor == null) {
      return null;
    }

    if (hoverContributors.size() > 0) {
      // return the first non null string from the contributors
      for (ITextHover hoverContributer : hoverContributors) {
        String hoverText = hoverContributer.getHoverInfo(textViewer, region);
        if (hoverText != null) {
          return hoverText;
        }
      }
    }

    DartUnit unit = editor.getAST();

    if (unit == null) {
      return null;
    }

    try {
      DartDocumentable documentable = DartDocUtilities.getDartDocumentable(
          (CompilationUnit) editor.getInputDartElement(), unit, region.getOffset(),
          region.getOffset() + region.getLength());

      if (documentable != null) {
        StringBuffer docs = new StringBuffer();

        docs.append("<b>" + DartDocUtilities.getTextSummary(documentable) + "</b>");

        String dartdoc = DartDocUtilities.getDartDocAsHtml(documentable);

        if (dartdoc != null) {
          docs.append("<br><br>");
          docs.append(dartdoc);
        }

        return docs.toString().trim();
      } else {
        return null;
      }
    } catch (DartModelException e) {
      return null;
    }
  }

  @Override
  protected boolean isIncluded(Annotation annotation) {
    return sourceViewerConfiguration.isShownInText(annotation);
  }

}
