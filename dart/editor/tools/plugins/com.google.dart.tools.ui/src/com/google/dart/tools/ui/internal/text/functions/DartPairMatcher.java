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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.text.DartPartitions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;

/**
 * Helper class for match pairs of characters.
 */
public final class DartPairMatcher extends DefaultCharacterPairMatcher {
  public DartPairMatcher(char[] pairs) {
    super(pairs, DartPartitions.DART_PARTITIONING);
  }

  /* @see ICharacterPairMatcher#match(IDocument, int) */
  @Override
  public IRegion match(IDocument document, int offset) {
    try {
      return performMatch(document, offset);
    } catch (BadLocationException ble) {
      return null;
    }
  }

  /**
   * Returns true if the character at the specified offset is a less-than sign, rather than an type
   * parameter list open angle bracket.
   * 
   * @param document a document
   * @param offset an offset within the document
   * @return true if the character at the specified offset is not a type parameter start bracket
   * @throws BadLocationException
   */
  private boolean isLessThanOperator(IDocument document, int offset) throws BadLocationException {
    if (offset < 0) {
      return false;
    }
    DartHeuristicScanner scanner = new DartHeuristicScanner(
        document,
        DartPartitions.DART_PARTITIONING,
        TextUtilities.getContentType(document, DartPartitions.DART_PARTITIONING, offset, false));
    return !isTypeParameterBracket(offset, document, scanner);
  }

  /**
   * Checks if the angular bracket at <code>offset</code> is a type parameter bracket.
   * 
   * @param offset the offset of the opening bracket
   * @param document the document
   * @param scanner a java heuristic scanner on <code>document</code>
   * @return <code>true</code> if the bracket is part of a type parameter, <code>false</code>
   *         otherwise
   */
  private boolean isTypeParameterBracket(int offset, IDocument document,
      DartHeuristicScanner scanner) {
    /*
     * type parameter come after braces (closing or opening), semicolons, or after a Type name
     * (heuristic: starts with capital character, or after a modifier keyword in a method
     * declaration (visibility, static, synchronized, final)
     */

    try {
      IRegion line = document.getLineInformationOfOffset(offset);

      int prevToken = scanner.previousToken(offset - 1, line.getOffset());
      int prevTokenOffset = scanner.getPosition() + 1;
      String previous = prevToken == Symbols.TokenEOF ? null : document.get(
          prevTokenOffset,
          offset - prevTokenOffset).trim();

      if (prevToken == Symbols.TokenLBRACE
          || prevToken == Symbols.TokenRBRACE
          || prevToken == Symbols.TokenSEMICOLON
//          || prevToken == Symbols.TokenSYNCHRONIZED
          || prevToken == Symbols.TokenSTATIC
          || (prevToken == Symbols.TokenIDENT && isTypeParameterIntroducer(previous))
          || prevToken == Symbols.TokenEOF) {
        return true;
      }
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
   */
  private boolean isTypeParameterIntroducer(String identifier) {
    return identifier.length() > 0
        && (Character.isUpperCase(identifier.charAt(0)) || identifier.startsWith("final") //$NON-NLS-1$
            || identifier.startsWith("public") //$NON-NLS-1$
            || identifier.startsWith("public") //$NON-NLS-1$
            || identifier.startsWith("protected") //$NON-NLS-1$
        || identifier.startsWith("private")); //$NON-NLS-1$
  }

  /*
   * Performs the actual work of matching for #match(IDocument, int).
   */
  private IRegion performMatch(IDocument document, int offset) throws BadLocationException {
    if (offset < 0 || document == null) {
      return null;
    }
    final char prevChar = document.getChar(Math.max(offset - 1, 0));
    if (prevChar == '<' && isLessThanOperator(document, offset - 1)) {
      return null;
    }
    final IRegion region = super.match(document, offset);
    if (region == null) {
      return region;
    }
    if (prevChar == '>') {
      final int peer = region.getOffset();
      if (isLessThanOperator(document, peer)) {
        return null;
      }
    }
    return region;
  }
}
