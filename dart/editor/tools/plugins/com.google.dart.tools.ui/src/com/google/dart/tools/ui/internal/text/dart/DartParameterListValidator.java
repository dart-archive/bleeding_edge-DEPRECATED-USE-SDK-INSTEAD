/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.ui.internal.text.functions.DartHeuristicScanner;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

import java.util.ArrayList;
import java.util.List;

public class DartParameterListValidator implements IContextInformationValidator,
    IContextInformationPresenter {

  private int fPosition;
  private ITextViewer fViewer;
  private IContextInformation fInformation;

  private int fCurrentParameter;

  public DartParameterListValidator() {
  }

  /**
   * @see IContextInformationValidator#install(IContextInformation, ITextViewer, int)
   * @see IContextInformationPresenter#install(IContextInformation, ITextViewer, int)
   */
  @Override
  public void install(IContextInformation info, ITextViewer viewer, int documentPosition) {
    fPosition = documentPosition;
    fViewer = viewer;
    fInformation = info;

    fCurrentParameter = -1;
  }

  /**
   * @see IContextInformationValidator#isContextInformationValid(int)
   */
  @Override
  public boolean isContextInformationValid(int position) {

    try {
      if (position < fPosition) {
        return false;
      }

      IDocument document = fViewer.getDocument();
      IRegion line = document.getLineInformationOfOffset(fPosition);

      if (position < line.getOffset() || position >= document.getLength()) {
        return false;
      }

      return getCharCount(document, fPosition, position, "(<", ")>", false) >= 0; //$NON-NLS-1$ //$NON-NLS-2$

    } catch (BadLocationException x) {
      return false;
    }
  }

  /**
   * @see IContextInformationPresenter#updatePresentation(int, TextPresentation)
   */
  @Override
  public boolean updatePresentation(int position, TextPresentation presentation) {

    int currentParameter = -1;

    try {
      currentParameter = getCharCount(fViewer.getDocument(), fPosition, position, ",", "", true); //$NON-NLS-1$//$NON-NLS-2$
    } catch (BadLocationException x) {
      return false;
    }

    if (fCurrentParameter != -1) {
      if (currentParameter == fCurrentParameter) {
        return false;
      }
    }

    presentation.clear();
    fCurrentParameter = currentParameter;

    String s = fInformation.getInformationDisplayString();
    int[] commas = computeCommaPositions(s);

    if (commas.length - 2 < fCurrentParameter) {
      presentation.addStyleRange(new StyleRange(0, s.length(), null, null, SWT.NORMAL));
      return true;
    }

    int start = commas[fCurrentParameter] + 1;
    int end = commas[fCurrentParameter + 1];
    if (start > 0) {
      presentation.addStyleRange(new StyleRange(0, start, null, null, SWT.NORMAL));
    }

    if (end > start) {
      presentation.addStyleRange(new StyleRange(start, end - start, null, null, SWT.BOLD));
    }

    if (end < s.length()) {
      presentation.addStyleRange(new StyleRange(end, s.length() - end, null, null, SWT.NORMAL));
    }

    return true;
  }

  private boolean checkGenericsHeuristic(IDocument document, int end, int bound)
      throws BadLocationException {
    DartHeuristicScanner scanner = new DartHeuristicScanner(document);
    return scanner.looksLikeClassInstanceCreationBackward(end, bound);
  }

  private int[] computeCommaPositions(String code) {
    final int length = code.length();
    int pos = 0;
    List positions = new ArrayList();
    positions.add(new Integer(-1));
    while (pos < length && pos != -1) {
      char ch = code.charAt(pos);
      switch (ch) {
        case ',':
          positions.add(new Integer(pos));
          break;
        case '<':
          pos = code.indexOf('>', pos);
          break;
        case '[':
          pos = code.indexOf(']', pos);
          break;
        default:
          break;
      }
      if (pos != -1) {
        pos++;
      }
    }
    positions.add(new Integer(length));

    int[] fields = new int[positions.size()];
    for (int i = 0; i < fields.length; i++) {
      fields[i] = ((Integer) positions.get(i)).intValue();
    }
    return fields;
  }

  private int getCharCount(IDocument document, final int start, final int end, String increments,
      String decrements, boolean considerNesting) throws BadLocationException {

    Assert.isTrue((increments.length() != 0 || decrements.length() != 0)
        && !increments.equals(decrements));

    final int NONE = 0;
    final int BRACKET = 1;
    final int BRACE = 2;
    final int PAREN = 3;
    final int ANGLE = 4;

    int nestingMode = NONE;
    int nestingLevel = 0;

    int charCount = 0;
    int offset = start;
    while (offset < end) {
      char curr = document.getChar(offset++);
      switch (curr) {
        case '/':
          if (offset < end) {
            char next = document.getChar(offset);
            if (next == '*') {
              // a comment starts, advance to the comment end
              offset = getCommentEnd(document, offset + 1, end);
            } else if (next == '/') {
              // '//'-comment: nothing to do anymore on this line
              offset = end;
            }
          }
          break;
        case '*':
          if (offset < end) {
            char next = document.getChar(offset);
            if (next == '/') {
              // we have been in a comment: forget what we read before
              charCount = 0;
              ++offset;
            }
          }
          break;
        case '"':
        case '\'':
          offset = getStringEnd(document, offset, end, curr);
          break;
        case '[':
          if (considerNesting) {
            if (nestingMode == BRACKET || nestingMode == NONE) {
              nestingMode = BRACKET;
              nestingLevel++;
            }
            break;
          }
        case ']':
          if (considerNesting) {
            if (nestingMode == BRACKET) {
              if (--nestingLevel == 0) {
                nestingMode = NONE;
              }
            }
            break;
          }
        case '(':
          if (considerNesting) {
            if (nestingMode == ANGLE) {
              // generics heuristic failed
              nestingMode = PAREN;
              nestingLevel = 1;
            }
            if (nestingMode == PAREN || nestingMode == NONE) {
              nestingMode = PAREN;
              nestingLevel++;
            }
            break;
          }
        case ')':
          if (considerNesting) {
            if (nestingMode == PAREN) {
              if (--nestingLevel == 0) {
                nestingMode = NONE;
              }
            }
            break;
          }
        case '{':
          if (considerNesting) {
            if (nestingMode == ANGLE) {
              // generics heuristic failed
              nestingMode = BRACE;
              nestingLevel = 1;
            }
            if (nestingMode == BRACE || nestingMode == NONE) {
              nestingMode = BRACE;
              nestingLevel++;
            }
            break;
          }
        case '}':
          if (considerNesting) {
            if (nestingMode == BRACE) {
              if (--nestingLevel == 0) {
                nestingMode = NONE;
              }
            }
            break;
          }
        case '<':
          if (considerNesting) {
            if (nestingMode == ANGLE || nestingMode == NONE
                && checkGenericsHeuristic(document, offset - 1, start - 1)) {
              nestingMode = ANGLE;
              nestingLevel++;
            }
            break;
          }
        case '>':
          if (considerNesting) {
            if (nestingMode == ANGLE) {
              if (--nestingLevel == 0) {
                nestingMode = NONE;
              }
            }
            break;
          }

        default:
          if (nestingLevel != 0) {
            continue;
          }

          if (increments.indexOf(curr) >= 0) {
            ++charCount;
          }

          if (decrements.indexOf(curr) >= 0) {
            --charCount;
          }
      }
    }

    return charCount;
  }

  private int getCommentEnd(IDocument d, int pos, int end) throws BadLocationException {
    while (pos < end) {
      char curr = d.getChar(pos);
      pos++;
      if (curr == '*') {
        if (pos < end && d.getChar(pos) == '/') {
          return pos + 1;
        }
      }
    }
    return end;
  }

  private int getStringEnd(IDocument d, int pos, int end, char ch) throws BadLocationException {
    while (pos < end) {
      char curr = d.getChar(pos);
      pos++;
      if (curr == '\\') {
        // ignore escaped characters
        pos++;
      } else if (curr == ch) {
        return pos;
      }
    }
    return end;
  }

}
