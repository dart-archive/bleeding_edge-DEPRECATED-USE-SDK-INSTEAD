/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.InterpolationExpression;
import com.google.dart.engine.ast.InterpolationString;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;
import com.google.dart.tools.ui.internal.text.functions.DartPairMatcher;
import com.google.dart.tools.ui.internal.text.functions.ISourceVersionDependent;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

/**
 * Double click strategy aware of Java identifier syntax rules.
 * 
 * @coverage dart.editor.ui.text
 */
public class DartDoubleClickSelector_OLD implements ITextDoubleClickStrategy,
    ISourceVersionDependent {

  /**
   * Detects java words depending on the source level. In 1.4 mode, detects <code>[[:ID:]]*</code>.
   * In 1.5 mode, it also detects <code>@\s*[[:IDS:]][[:ID:]]*</code>. Character class definitions:
   * <dl>
   * <dt>[[:IDS:]]</dt>
   * <dd>a java identifier start character</dd>
   * <dt>[[:ID:]]</dt>
   * <dd>a java identifier part character</dd>
   * <dt>\s</dt>
   * <dd>a white space character</dd>
   * <dt>@</dt>
   * <dd>the at symbol</dd>
   * </dl>
   */
  private static final class AtJavaIdentifierDetector implements ISourceVersionDependent {

    private boolean fSelectAnnotations;

    private static final int UNKNOWN = -1;

    /* states */
    private static final int WS = 0;
    private static final int ID = 1;
    private static final int IDS = 2;
    private static final int AT = 3;

    /* directions */
    private static final int FORWARD = 0;
    private static final int BACKWARD = 1;

    /** The current state. */
    private int fState;
    /**
     * The state at the anchor (if already detected by going the other way), or <code>UNKNOWN</code>
     * .
     */
    private int fAnchorState;
    /** The current direction. */
    private int fDirection;
    /** The start of the detected word. */
    private int fStart;
    /** The end of the word. */
    private int fEnd;

    /**
     * Returns the region containing <code>anchor</code> that is a java word.
     * 
     * @param document the document from which to read characters
     * @param anchor the offset around which to select a word
     * @return the region describing a java word around <code>anchor</code>
     */
    public IRegion getWordSelection(IDocument document, int anchor) {

      try {

        final int min = 0;
        final int max = document.getLength();
        setAnchor(anchor);

        char c;

        int offset = anchor;
        while (offset < max) {
          c = document.getChar(offset);
          if (!forward(c, offset)) {
            break;
          }
          ++offset;
        }

        offset = anchor; // use to not select the previous word when right behind it
//      offset = anchor - 1; // use to select the previous word when right behind it
        while (offset >= min) {
          c = document.getChar(offset);
          if (!backward(c, offset)) {
            break;
          }
          --offset;
        }

        return new Region(fStart, fEnd - fStart + 1);

      } catch (BadLocationException x) {
        return new Region(anchor, 0);
      }
    }

    @Override
    public void setSourceVersion(String version) {
      if (JavaScriptCore.VERSION_1_5.compareTo(version) <= 0) {
        fSelectAnnotations = true;
      } else {
        fSelectAnnotations = false;
      }
    }

    /**
     * Try to add a character to the word going backward. Only call after forward calls!
     * 
     * @param c the character to add
     * @param offset the offset of the character
     * @return <code>true</code> if further characters may be added to the word
     */
    private boolean backward(char c, int offset) {
      checkDirection(BACKWARD);
      switch (fState) {
        case AT:
          return false;
        case IDS:
          if (isAt(c)) {
            fStart = offset;
            fState = AT;
            return false;
          }
          if (isWhitespace(c)) {
            fState = WS;
            return true;
          }
          //$FALL-THROUGH$
        case ID:
          if (isIdentifierStart(c)) {
            fStart = offset;
            fState = IDS;
            return true;
          }
          if (isIdentifierPart(c)) {
            fStart = offset;
            fState = ID;
            return true;
          }
          return false;
        case WS:
          if (isWhitespace(c)) {
            return true;
          }
          if (isAt(c)) {
            fStart = offset;
            fState = AT;
            return false;
          }
          return false;
        default:
          return false;
      }
    }

    /**
     * If the direction changes, set state to be the previous anchor state.
     * 
     * @param direction the new direction
     */
    private void checkDirection(int direction) {
      if (fDirection == direction) {
        return;
      }

      if (direction == FORWARD) {
        if (fStart <= fEnd) {
          fState = fAnchorState;
        } else {
          fState = UNKNOWN;
        }
      } else if (direction == BACKWARD) {
        if (fEnd >= fStart) {
          fState = fAnchorState;
        } else {
          fState = UNKNOWN;
        }
      }

      fDirection = direction;
    }

    /**
     * Try to add a character to the word going forward.
     * 
     * @param c the character to add
     * @param offset the offset of the character
     * @return <code>true</code> if further characters may be added to the word
     */
    private boolean forward(char c, int offset) {
      checkDirection(FORWARD);
      switch (fState) {
        case WS:
        case AT:
          if (isWhitespace(c)) {
            fState = WS;
            return true;
          }
          if (isIdentifierStart(c)) {
            fEnd = offset;
            fState = IDS;
            return true;
          }
          return false;
        case IDS:
        case ID:
          if (isIdentifierStart(c)) {
            fEnd = offset;
            fState = IDS;
            return true;
          }
          if (isIdentifierPart(c)) {
            fEnd = offset;
            fState = ID;
            return true;
          }
          return false;
        case UNKNOWN:
          if (isIdentifierStart(c)) {
            fEnd = offset;
            fState = IDS;
            fAnchorState = fState;
            return true;
          }
          if (isIdentifierPart(c)) {
            fEnd = offset;
            fState = ID;
            fAnchorState = fState;
            return true;
          }
          if (isWhitespace(c)) {
            fState = WS;
            fAnchorState = fState;
            return true;
          }
          if (isAt(c)) {
            fStart = offset;
            fState = AT;
            fAnchorState = fState;
            return true;
          }
          return false;
        default:
          return false;
      }
    }

    private boolean isAt(char c) {
      return fSelectAnnotations && c == '@';
    }

    private boolean isIdentifierPart(char c) {
      return Character.isJavaIdentifierPart(c);
    }

    private boolean isIdentifierStart(char c) {
      return Character.isJavaIdentifierStart(c);
    }

    private boolean isWhitespace(char c) {
      return fSelectAnnotations && Character.isWhitespace(c);
    }

    /**
     * Initializes the detector at offset <code>anchor</code>.
     * 
     * @param anchor the offset of the double click
     */
    private void setAnchor(int anchor) {
      fState = UNKNOWN;
      fAnchorState = UNKNOWN;
      fDirection = UNKNOWN;
      fStart = anchor;
      fEnd = anchor - 1;
    }

  }

  protected static final char[] BRACKETS = {'{', '}', '(', ')', '[', ']', '<', '>'};
  protected DartPairMatcher fPairMatcher = new DartPairMatcher(BRACKETS);
  protected final AtJavaIdentifierDetector fWordDetector = new AtJavaIdentifierDetector();

  public DartDoubleClickSelector_OLD() {
    super();
  }

  /**
   * @see ITextDoubleClickStrategy#doubleClicked
   */
  @Override
  public void doubleClicked(ITextViewer textViewer) {

    int offset = textViewer.getSelectedRange().x;

    if (offset < 0) {
      return;
    }

    IDocument document = textViewer.getDocument();

    IRegion region = fPairMatcher.match(document, offset);

    if (region != null && region.getLength() >= 2) {
      textViewer.setSelectedRange(region.getOffset() + 1, region.getLength() - 2);
    } else if (textViewer instanceof CompilationUnitEditor.AdaptedSourceViewer) {
      CompilationUnitEditor editor = ((CompilationUnitEditor.AdaptedSourceViewer) textViewer).getEditor();
      NodeLocator locator = new NodeLocator(offset);
      AstNode node = locator.searchWithin(editor.getInputUnit());
      if (node instanceof SimpleIdentifier) {
        region = new Region(node.getOffset(), node.getLength());
      } else if (node instanceof InterpolationString) {
        region = computeStringRegion(node);
        if (region == null) {
          region = selectWord(document, offset);
        }
      } else if (node instanceof InterpolationExpression) {
        region = new Region(node.getOffset(), node.getLength());
      } else {
        region = selectWord(document, offset);
      }
      textViewer.setSelectedRange(region.getOffset(), region.getLength());
    } else {
      region = selectWord(document, offset);
      textViewer.setSelectedRange(region.getOffset(), region.getLength());
    }
  }

  @Override
  public void setSourceVersion(String version) {
    fWordDetector.setSourceVersion(version);
  }

  protected IRegion computeStringRegion(AstNode node) {
    int start = node.getOffset();
    int originalStart = start;
    int end = node.getEnd();
    InterpolationString str = (InterpolationString) node;
    String chars = str.getContents().getLexeme();
    if (chars != null && chars.length() > 0) {
      char ch = chars.charAt(0);
      if (ch == '\'' || ch == '"') {
        start += 1;
      }
      if (start == originalStart || chars.length() > 1) {
        ch = chars.charAt(chars.length() - 1);
        if (ch == '\'' || ch == '"') {
          end -= 1;
        }
      }
      return new Region(start, end - start);
    }
    return null; // should not happen
  }

  protected void selectExpression(AstNode node, ITextViewer textViewer) {
    textViewer.setSelectedRange(node.getOffset(), node.getLength());
  }

  protected IRegion selectWord(IDocument document, int anchor) {
    return fWordDetector.getWordSelection(document, anchor);
  }
}
