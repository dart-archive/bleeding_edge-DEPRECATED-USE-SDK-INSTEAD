/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.text;

// taken from package org.eclipse.jdt.ui.text;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ICharacterPairMatcher;

import java.io.IOException;

class JavaPairMatcher implements ICharacterPairMatcher {

  protected char[] fPairs;
  protected IDocument fDocument;
  protected int fOffset;

  protected int fStartPos;
  protected int fEndPos;
  protected int fAnchor;

  protected JavaCodeReader fReader = new JavaCodeReader();
  /**
   * Stores the source version state.
   * 
   * @see Eclipse 3.1
   */
  private boolean fHighlightAngularBrackets = false;

  public JavaPairMatcher(char[] pairs) {
    fPairs = pairs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.source.ICharacterPairMatcher#match(org.eclipse.jface.text.IDocument,
   * int)
   */
  public IRegion match(IDocument document, int offset) {
    fOffset = offset;

    if (offset < 0 || offset >= document.getLength())
      return null;

    fDocument = document;

    if (fDocument != null && matchPairsAt() && fStartPos != fEndPos)
      return new Region(fStartPos, fEndPos - fStartPos + 1);

    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.source.ICharacterPairMatcher#getAnchor()
   */
  public int getAnchor() {
    return fAnchor;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.source.ICharacterPairMatcher#dispose()
   */
  public void dispose() {
    clear();
    fDocument = null;
    fReader = null;
  }

  /*
   * @see org.eclipse.jface.text.source.ICharacterPairMatcher#clear()
   */
  public void clear() {
    if (fReader != null) {
      try {
        fReader.close();
      } catch (IOException x) {
        // ignore
      }
    }
  }

  protected boolean matchPairsAt() {

    int i;
    int pairIndex1 = fPairs.length;
    int pairIndex2 = fPairs.length;

    fStartPos = -1;
    fEndPos = -1;

    // get the chars preceding and following the start position
    try {

      char prevChar = fDocument.getChar(Math.max(fOffset - 1, 0));
      // modified behavior for
      // http://dev.eclipse.org/bugs/show_bug.cgi?id=16879
      // char nextChar= fDocument.getChar(fOffset);

      // search for opening peer character next to the activation point
      for (i = 0; i < fPairs.length; i = i + 2) {
        // if (nextChar == fPairs[i]) {
        // fStartPos= fOffset;
        // pairIndex1= i;
        // } else
        if (prevChar == fPairs[i]) {
          fStartPos = fOffset - 1;
          pairIndex1 = i;
        }
      }

      // search for closing peer character next to the activation point
      for (i = 1; i < fPairs.length; i = i + 2) {
        if (prevChar == fPairs[i]) {
          fEndPos = fOffset - 1;
          pairIndex2 = i;
        }
        // else if (nextChar == fPairs[i]) {
        // fEndPos= fOffset;
        // pairIndex2= i;
        // }
      }

      if (fEndPos > -1) {
        fAnchor = RIGHT;
        fStartPos = searchForOpeningPeer(fEndPos, fPairs[pairIndex2 - 1], fPairs[pairIndex2],
            fDocument);
        if (fStartPos > -1)
          return true;
        else
          fEndPos = -1;
      } else if (fStartPos > -1) {
        fAnchor = LEFT;
        fEndPos = searchForClosingPeer(fStartPos, fPairs[pairIndex1], fPairs[pairIndex1 + 1],
            fDocument);
        if (fEndPos > -1)
          return true;
        else
          fStartPos = -1;
      }

    } catch (BadLocationException x) {
    } catch (IOException x) {
    }

    return false;
  }

  protected int searchForClosingPeer(int offset, int openingPeer, int closingPeer,
      IDocument document) throws IOException {
    if (openingPeer == '<'
        && !(fHighlightAngularBrackets && isTypeParameterBracket(offset, document)))
      return -1;

    fReader.configureForwardReader(document, offset + 1, document.getLength(), true, true);

    int stack = 1;
    int c = fReader.read();
    while (c != JavaCodeReader.EOF) {
      if (c == openingPeer && c != closingPeer)
        stack++;
      else if (c == closingPeer)
        stack--;

      if (stack == 0)
        return fReader.getOffset();

      c = fReader.read();
    }

    return -1;
  }

  protected int searchForOpeningPeer(int offset, int openingPeer, int closingPeer,
      IDocument document) throws IOException {
    if (openingPeer == '<' && !fHighlightAngularBrackets)
      return -1;

    fReader.configureBackwardReader(document, offset, true, true);

    int stack = 1;
    int c = fReader.read();
    while (c != JavaCodeReader.EOF) {
      if (c == closingPeer && c != openingPeer)
        stack++;
      else if (c == openingPeer)
        stack--;

      if (stack == 0) {
        if (closingPeer == '>' && !isTypeParameterBracket(fReader.getOffset(), document))
          return -1;
        return fReader.getOffset();
      }

      c = fReader.read();
    }

    return -1;
  }

  /**
   * Checks if the angular bracket at <code>offset</code> is a type parameter bracket.
   * 
   * @param offset the offset of the opening bracket
   * @param document the document
   * @return <code>true</code> if the bracket is part of a type parameter, <code>false</code>
   *         otherwise
   * @see Eclipse 3.1
   */
  private boolean isTypeParameterBracket(int offset, IDocument document) {
    /*
     * type parameter come after braces (closing or opening), semicolons, or after a Type name
     * (heuristic: starts with capital character, or after a modifier keyword in a method
     * declaration (visibility, static, synchronized, final)
     */

    try {
      IRegion line = document.getLineInformationOfOffset(offset);

      JavaHeuristicScanner scanner = new JavaHeuristicScanner(document);
      int prevToken = scanner.previousToken(offset - 1, line.getOffset());
      int prevTokenOffset = scanner.getPosition() + 1;
      String previous = prevToken == Symbols.TokenEOF ? null : document.get(prevTokenOffset,
          offset - prevTokenOffset).trim();

      if (prevToken == Symbols.TokenLBRACE || prevToken == Symbols.TokenRBRACE
          || prevToken == Symbols.TokenSEMICOLON || prevToken == Symbols.TokenSYNCHRONIZED
          || prevToken == Symbols.TokenSTATIC
          || (prevToken == Symbols.TokenIDENT && isTypeParameterIntroducer(previous))
          || prevToken == Symbols.TokenEOF)
        return true;
    } catch (BadLocationException e) {
      return false;
    }

    return false;
  }

  /**
   * Returns <code>true</code> if <code>identifier</code> is an identifier that could come right
   * before a type parameter list. It uses a heuristic: if the identifier starts with an upper case,
   * it is assumed a type name. Also, if <code>identifier</code> is a method modifier, it is assumed
   * that the angular bracket is part of the generic type parameter of a method.
   * 
   * @param identifier the identifier to check
   * @return <code>true</code> if the identifier could introduce a type parameter list
   * @see Eclipse 3.1
   */
  private boolean isTypeParameterIntroducer(String identifier) {
    return identifier.length() > 0
        && (Character.isUpperCase(identifier.charAt(0)) || identifier.startsWith("final") //$NON-NLS-1$
            || identifier.startsWith("public") //$NON-NLS-1$
            || identifier.startsWith("public") //$NON-NLS-1$
            || identifier.startsWith("protected") //$NON-NLS-1$
        || identifier.startsWith("private")); //$NON-NLS-1$
  }
}
