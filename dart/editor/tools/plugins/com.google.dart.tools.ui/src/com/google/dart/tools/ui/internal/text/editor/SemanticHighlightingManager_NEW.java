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

package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.server.AnalysisServer;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.HighlightType;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.AnalysisServerData;
import com.google.dart.tools.core.analysis.model.AnalysisServerHighlightsListener;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.text.IColorManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * A helper for displaying {@link HighlightRegion} from {@link AnalysisServer}.
 */
public class SemanticHighlightingManager_NEW implements AnalysisServerHighlightsListener,
    ITextPresentationListener {
  /**
   * A {@link Position} that can be tracked by a {@link Document} and contains the
   * {@link HighlightRegion}.
   */
  private static class HighlightPosition extends Position {
    private final HighlightRegion highlight;

    public HighlightPosition(HighlightRegion highlight) {
      super(highlight.getOffset(), highlight.getLength());
      this.highlight = highlight;
    }

    @Override
    public String toString() {
      return "[" + super.toString() + " " + highlight + "]";
    }
  }

  private final DartSourceViewer viewer;
  private final String file;
  private final IDocument document;
  private final IDocumentListener documentListener;
  private HighlightPosition[] positions;
  private boolean positionsAddedToDocument = false;

  public SemanticHighlightingManager_NEW(DartSourceViewer viewer, String file) {
    this.viewer = viewer;
    this.file = file;
    this.document = viewer.getDocument();
    // subscribe
    AnalysisServerData analysisServerData = DartCore.getAnalysisServerData();
    analysisServerData.subscribeHighlights(file, this);
    viewer.prependTextPresentationListener(this);
    documentListener = new IDocumentListener() {
      @Override
      public void documentAboutToBeChanged(DocumentEvent event) {
      }

      @Override
      public void documentChanged(DocumentEvent event) {
        clearHighlightPositions();
      }
    };
    document.addDocumentListener(documentListener);
  }

  @Override
  public void applyTextPresentation(TextPresentation textPresentation) {
    if (positions == null) {
      return;
    }
    // prepare damaged region
    IRegion damagedRegion = textPresentation.getExtent();
    int daOffset = damagedRegion.getOffset();
    int daEnd = daOffset + damagedRegion.getLength();
    // prepare theme access
    IPreferenceStore store = DartToolsPlugin.getDefault().getPreferenceStore();
    IColorManager colorManager = DartUI.getColorManager();
    // add style ranges
    for (HighlightPosition position : positions) {
      // skip if outside of the damaged region
      int hiOffset = position.getOffset();
      int hiLength = position.getLength();
      if (hiOffset + hiLength < daOffset || hiOffset >= daEnd) {
        continue;
      }
      // prepare highlight key
      HighlightType type = position.highlight.getType();
      String themeKey = getThemeKey(type);
      if (themeKey == null) {
        continue;
      }
      themeKey = "semanticHighlighting." + themeKey;
      // prepare color
      RGB foregroundRGB = PreferenceConverter.getColor(store, themeKey + ".color");
      Color foregroundColor = colorManager.getColor(foregroundRGB);
      // prepare font style
      boolean fontBold = store.getBoolean(themeKey + ".bold");
      boolean fontItalic = store.getBoolean(themeKey + ".italic");
      int fontStyle = 0;
      if (fontBold) {
        fontStyle |= SWT.BOLD;
      }
      if (fontItalic) {
        fontStyle |= SWT.ITALIC;
      }
      // merge style range
      textPresentation.replaceStyleRange(new StyleRange(
          hiOffset,
          hiLength,
          foregroundColor,
          null,
          fontStyle));
    }
  }

  @Override
  public void computedHighlights(String file, HighlightRegion[] highlights) {
    clearHighlightPositions();
    // create and track HighlightPosition(s)
    HighlightPosition[] newPositions = new HighlightPosition[highlights.length];
    for (int i = 0; i < highlights.length; i++) {
      HighlightRegion highlight = highlights[i];
      HighlightPosition position = new HighlightPosition(highlight);
      try {
        document.addPosition(position);
      } catch (BadLocationException e) {
      }
      newPositions[i] = position;
    }
    positions = newPositions;
    positionsAddedToDocument = true;
    // invalidate presentation
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        viewer.invalidateTextPresentation();
      }
    });
  }

  public void dispose() {
    AnalysisServerData analysisServerData = DartCore.getAnalysisServerData();
    analysisServerData.unsubscribeHighlights(file, this);
    viewer.removeTextPresentationListener(this);
    document.removeDocumentListener(documentListener);
    clearHighlightPositions();
  }

  private void clearHighlightPositions() {
    if (positionsAddedToDocument) {
      for (HighlightPosition position : positions) {
        document.removePosition(position);
      }
      positionsAddedToDocument = false;
    }
  }

  private String getThemeKey(HighlightType type) {
    switch (type) {
      case ANNOTATION:
        return "annotation";
      case BUILT_IN:
      case KEYWORD:
        return "builtin";
      case CLASS:
        return "class";
      case CONSTRUCTOR:
        return "constructor";
      case DIRECTIVE:
        return "directive";
      case DYNAMIC_TYPE:
        return "dynamicType";
      case FIELD:
        return "field";
      case FIELD_STATIC:
        return "staticField";
      case FUNCTION:
        return "function";
      case FUNCTION_DECLARATION:
        return "methodDeclarationName";
      case FUNCTION_TYPE_ALIAS:
        return "functionTypeAlias";
      case GETTER_DECLARATION:
        return "getterDeclaration";
      case IMPORT_PREFIX:
        return "importPrefix";
      case LITERAL_BOOLEAN:
        return "builtin";
      case LITERAL_DOUBLE:
      case LITERAL_INTEGER:
        return "number";
      case LITERAL_STRING:
        return "string";
      case LOCAL_VARIABLE:
        return "localVariable";
      case LOCAL_VARIABLE_DECLARATION:
        return "localVariableDeclaration";
      case METHOD:
        return "method";
      case METHOD_STATIC:
        return "staticMethod";
      case METHOD_DECLARATION:
        return "methodDeclarationName";
      case METHOD_DECLARATION_STATIC:
        return "staticMethodDeclarationName";
      case PARAMETER:
        return "parameterVariable";
      case SETTER_DECLARATION:
        return "setterDeclaration";
      case TOP_LEVEL_VARIABLE:
        return "staticField";
      case TYPE_NAME_DYNAMIC:
        return "builtin";
      case TYPE_PARAMETER:
        return "typeParameter";
      case COMMENT_BLOCK:
      case COMMENT_DOCUMENTATION:
      case COMMENT_END_OF_LINE:
      case IDENTIFIER_DEFAULT:
      case LITERAL_LIST:
      case LITERAL_MAP:
        // unsupported
        break;
    }
    return null;
  }
}
