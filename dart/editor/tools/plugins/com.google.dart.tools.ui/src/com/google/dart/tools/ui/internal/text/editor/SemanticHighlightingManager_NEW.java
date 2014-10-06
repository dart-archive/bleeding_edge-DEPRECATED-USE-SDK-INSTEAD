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
import com.google.dart.server.generated.types.HighlightRegion;
import com.google.dart.server.generated.types.HighlightRegionType;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.AnalysisServerData;
import com.google.dart.tools.core.analysis.model.AnalysisServerHighlightsListener;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.text.IColorManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
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
   * Semantic highlighting position updater.
   */
  private class HighlightingPositionUpdater implements IPositionUpdater {
    @Override
    public void update(DocumentEvent event) {
      if (positions == null) {
        return;
      }
      // prepare event values
      int eventOffset = event.getOffset();
      int eventOldLength = event.getLength();
      int eventEnd = eventOffset + eventOldLength;
      // update positions
      for (HighlightPosition position : positions) {
        int offset = position.getOffset();
        int length = position.getLength();
        int end = offset + length;
        if (offset > eventEnd) {
          updateWithPrecedingEvent(position, event);
        } else if (end < eventOffset) {
          updateWithSucceedingEvent(position, event);
        } else if (offset <= eventOffset && end >= eventEnd) {
          updateWithIncludedEvent(position, event);
        } else if (offset <= eventOffset) {
          updateWithOverEndEvent(position, event);
        } else if (end >= eventEnd) {
          updateWithOverStartEvent(position, event);
        } else {
          updateWithIncludingEvent(position, event);
        }
      }
    }

    private boolean isDartIdentifierPart(String text, int index) {
      char c = text.charAt(index);
      return Character.isJavaIdentifierPart(c);
    }

    /**
     * Update the given position with the given event.
     * <p>
     * The event is included by the position.
     */
    private void updateWithIncludedEvent(HighlightPosition position, DocumentEvent event) {
      String eventText = event.getText();
      if (eventText != null) {
        int length = position.getLength();
        int newLength = length + eventText.length();
        position.setLength(newLength);
      }
    }

    /**
     * Update the given position with the given event.
     * <p>
     * The event includes the position.
     */
    private void updateWithIncludingEvent(HighlightPosition position, DocumentEvent event) {
      position.delete();
      position.update(event.getOffset(), 0);
    }

    /**
     * Update the given position with the given event.
     * <p>
     * The event overlaps with the end of the position.
     */
    private void updateWithOverEndEvent(HighlightPosition position, DocumentEvent event) {
      String newText = event.getText();
      if (newText == null) {
        newText = "";
      }
      int eventNewLength = newText.length();

      int includedLength = 0;
      while (includedLength < eventNewLength && isDartIdentifierPart(newText, includedLength)) {
        includedLength++;
      }
      position.setLength(event.getOffset() - position.getOffset() + includedLength);
    }

    /**
     * Update the given position with the given event.
     * <p>
     * The event overlaps with the start of the position.
     */
    private void updateWithOverStartEvent(HighlightPosition position, DocumentEvent event) {
      int eventOffset = event.getOffset();
      int eventEnd = eventOffset + event.getLength();

      String newText = event.getText();
      if (newText == null) {
        newText = "";
      }
      int eventNewLength = newText.length();

      int excludedLength = eventNewLength;
      while (excludedLength > 0 && isDartIdentifierPart(newText, excludedLength - 1)) {
        excludedLength--;
      }
      int deleted = eventEnd - position.getOffset();
      int inserted = eventNewLength - excludedLength;
      position.update(eventOffset + excludedLength, position.getLength() - deleted + inserted);
    }

    /**
     * Update the given position with the given event.
     * <p>
     * The event precedes the position.
     */
    private void updateWithPrecedingEvent(HighlightPosition position, DocumentEvent event) {
      String newText = event.getText();
      int eventNewLength = newText != null ? newText.length() : 0;
      int deltaLength = eventNewLength - event.getLength();

      position.setOffset(position.getOffset() + deltaLength);
    }

    /**
     * Update the given position with the given event.
     * <p>
     * The event succeeds the position.
     */
    private void updateWithSucceedingEvent(HighlightPosition position, DocumentEvent event) {
    }
  }

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

    public void update(int offset, int len) {
      setOffset(offset);
      setLength(len);
    }
  }

  private final DartSourceViewer viewer;
  private final String file;
  private final IDocument document;
  private final IPositionUpdater positionUpdater = new HighlightingPositionUpdater();
  private HighlightPosition[] positions;

  public SemanticHighlightingManager_NEW(DartSourceViewer viewer, String file) {
    this.viewer = viewer;
    this.file = file;
    this.document = viewer.getDocument();
    document.addPositionUpdater(positionUpdater);
    // subscribe
    AnalysisServerData analysisServerData = DartCore.getAnalysisServerData();
    analysisServerData.subscribeHighlights(file, this);
    viewer.prependTextPresentationListener(this);
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
      int hiEnd = hiOffset + hiLength;
      if (hiEnd < daOffset || hiOffset >= daEnd) {
        continue;
      }
      if (hiEnd > daEnd) {
        continue;
      }
      // prepare highlight key
      String highlightType = position.highlight.getType();
      String themeKey = getThemeKey(highlightType);
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
  public void computedHighlights(String file, final HighlightRegion[] highlights) {
    // create HighlightPosition(s)
    HighlightPosition[] newPositions = new HighlightPosition[highlights.length];
    for (int i = 0; i < highlights.length; i++) {
      HighlightRegion highlight = highlights[i];
      newPositions[i] = new HighlightPosition(highlight);
    }
    positions = newPositions;
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
    document.removePositionUpdater(positionUpdater);
  }

  /**
   * The type will be a {@link String} from {@link HighlightRegionType}.
   */
  private String getThemeKey(String type) {
    if (type.equals(HighlightRegionType.ANNOTATION)) {
      return "annotation";
    } else if (type.equals(HighlightRegionType.BUILT_IN)
        || type.equals(HighlightRegionType.KEYWORD)) {
      return "builtin";
    } else if (type.equals(HighlightRegionType.CLASS)) {
      return "class";
    } else if (type.equals(HighlightRegionType.CONSTRUCTOR)) {
      return "constructor";
    } else if (type.equals(HighlightRegionType.DYNAMIC_TYPE)) {
      return "dynamicType";
    } else if (type.equals(HighlightRegionType.FIELD)) {
      return "field";
    } else if (type.equals(HighlightRegionType.FIELD_STATIC)) {
      return "staticField";
    } else if (type.equals(HighlightRegionType.FUNCTION)) {
      return "function";
    } else if (type.equals(HighlightRegionType.FUNCTION_DECLARATION)) {
      return "methodDeclarationName";
    } else if (type.equals(HighlightRegionType.FUNCTION_TYPE_ALIAS)) {
      return "functionTypeAlias";
    } else if (type.equals(HighlightRegionType.GETTER_DECLARATION)) {
      return "getterDeclaration";
    } else if (type.equals(HighlightRegionType.IMPORT_PREFIX)) {
      return "importPrefix";
    } else if (type.equals(HighlightRegionType.LABEL)) {
      return "label";
    } else if (type.equals(HighlightRegionType.LITERAL_BOOLEAN)) {
      return "builtin";
    } else if (type.equals(HighlightRegionType.LITERAL_DOUBLE)
        || type.equals(HighlightRegionType.LITERAL_INTEGER)) {
      return "number";
    } else if (type.equals(HighlightRegionType.LITERAL_STRING)) {
      return "string";
    } else if (type.equals(HighlightRegionType.LOCAL_VARIABLE)) {
      return "localVariable";
    } else if (type.equals(HighlightRegionType.LOCAL_VARIABLE_DECLARATION)) {
      return "localVariableDeclaration";
    } else if (type.equals(HighlightRegionType.METHOD)) {
      return "method";
    } else if (type.equals(HighlightRegionType.METHOD_STATIC)) {
      return "staticMethod";
    } else if (type.equals(HighlightRegionType.METHOD_DECLARATION)) {
      return "methodDeclarationName";
    } else if (type.equals(HighlightRegionType.METHOD_DECLARATION_STATIC)) {
      return "staticMethodDeclarationName";
    } else if (type.equals(HighlightRegionType.PARAMETER)) {
      return "parameterVariable";
    } else if (type.equals(HighlightRegionType.SETTER_DECLARATION)) {
      return "setterDeclaration";
    } else if (type.equals(HighlightRegionType.TOP_LEVEL_VARIABLE)) {
      return "staticField";
    } else if (type.equals(HighlightRegionType.TYPE_NAME_DYNAMIC)) {
      return "builtin";
    } else if (type.equals(HighlightRegionType.TYPE_PARAMETER)) {
      return "typeParameter";
    } else {
      // unsupported:
//    COMMENT_BLOCK:
//    COMMENT_DOCUMENTATION:
//    COMMENT_END_OF_LINE:
//    DIRECTIVE:
//    IDENTIFIER_DEFAULT:
//    LITERAL_LIST:
//    LITERAL_MAP:
      return null;
    }
  }
}
