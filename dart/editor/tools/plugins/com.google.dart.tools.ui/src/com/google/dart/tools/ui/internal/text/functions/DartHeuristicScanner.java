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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.text.DartPartitions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedRegion;

import java.util.Arrays;

/**
 * Utility methods for heuristic based Dart manipulations in an incomplete Dart source file.
 * <p>
 * An instance holds some internal position in the document and is therefore not thread-safe.
 * </p>
 */
public final class DartHeuristicScanner implements Symbols {
  /**
   * Stops upon a character in the default partition that matches the given character list.
   */
  private final class CharacterMatch extends StopCondition {
    private final char[] fChars;

    /**
     * Creates a new instance.
     * 
     * @param ch the single character to match
     */
    public CharacterMatch(char ch) {
      this(new char[] {ch});
    }

    /**
     * Creates a new instance.
     * 
     * @param chars the chars to match.
     */
    public CharacterMatch(char[] chars) {
      Assert.isNotNull(chars);
      Assert.isTrue(chars.length > 0);
      fChars = chars;
      Arrays.sort(chars);
    }

    @Override
    public int nextPosition(int position, boolean forward) {
      ITypedRegion partition = getPartition(position);
      if (fPartition.equals(partition.getType())) {
        return super.nextPosition(position, forward);
      }

      if (forward) {
        int end = partition.getOffset() + partition.getLength();
        if (position < end) {
          return end;
        }
      } else {
        int offset = partition.getOffset();
        if (position > offset) {
          return offset - 1;
        }
      }
      return super.nextPosition(position, forward);
    }

    @Override
    public boolean stop(char ch, int position, boolean forward) {
      return Arrays.binarySearch(fChars, ch) >= 0 && isDefaultPartition(position);
    }
  }

  /**
   * Stops upon a non-identifier (as defined by {@link Character#isJavaIdentifierPart(char)})
   * character.
   */
  private static class NonDartIdentifierPart extends StopCondition {
    @Override
    public boolean stop(char ch, int position, boolean forward) {
      return !Character.isJavaIdentifierPart(ch);
    }
  }

  /**
   * Stops upon a non-identifier character in the default partition.
   * 
   * @see DartHeuristicScanner.NonDartIdentifierPart
   */
  private final class NonDartIdentifierPartDefaultPartition extends NonDartIdentifierPart {
    @Override
    public int nextPosition(int position, boolean forward) {
      ITypedRegion partition = getPartition(position);
      if (fPartition.equals(partition.getType())) {
        return super.nextPosition(position, forward);
      }

      if (forward) {
        int end = partition.getOffset() + partition.getLength();
        if (position < end) {
          return end;
        }
      } else {
        int offset = partition.getOffset();
        if (position > offset) {
          return offset - 1;
        }
      }
      return super.nextPosition(position, forward);
    }

    @Override
    public boolean stop(char ch, int position, boolean forward) {
      return super.stop(ch, position, true) || !isDefaultPartition(position);
    }
  }
  /**
   * Stops upon a non-whitespace (as defined by {@link Character#isWhitespace(char)}) character.
   */
  private static class NonWhitespace extends StopCondition {
    @Override
    public boolean stop(char ch, int position, boolean forward) {
      return !Character.isWhitespace(ch);
    }
  }
  /**
   * Stops upon a non-whitespace character in the default partition.
   * 
   * @see DartHeuristicScanner.NonWhitespace
   */
  private final class NonWhitespaceDefaultPartition extends NonWhitespace {
    @Override
    public int nextPosition(int position, boolean forward) {
      ITypedRegion partition = getPartition(position);
      if (fPartition.equals(partition.getType())) {
        return super.nextPosition(position, forward);
      }

      if (forward) {
        int end = partition.getOffset() + partition.getLength();
        if (position < end) {
          return end;
        }
      } else {
        int offset = partition.getOffset();
        if (position > offset) {
          return offset - 1;
        }
      }
      return super.nextPosition(position, forward);
    }

    @Override
    public boolean stop(char ch, int position, boolean forward) {
      return super.stop(ch, position, true) && isDefaultPartition(position);
    }
  }
  /**
   * Specifies the stop condition, upon which the <code>scanXXX</code> methods will decide whether
   * to keep scanning or not. This interface may implemented by clients.
   */
  private static abstract class StopCondition {
    /**
     * Asks the condition to return the next position to query. The default is to return the
     * next/previous position.
     * 
     * @return the next position to scan
     */
    public int nextPosition(int position, boolean forward) {
      return forward ? position + 1 : position - 1;
    }

    /**
     * Instructs the scanner to return the current position.
     * 
     * @param ch the char at the current position
     * @param position the current position
     * @param forward the iteration direction
     * @return <code>true</code> if the stop condition is met.
     */
    public abstract boolean stop(char ch, int position, boolean forward);
  }

  /**
   * Returned by all methods when the requested position could not be found, or if a
   * {@link BadLocationException} was thrown while scanning.
   */
  public static final int NOT_FOUND = -1;
  /**
   * Special bound parameter that means either -1 (backward scanning) or
   * <code>fDocument.getLength()</code> (forward scanning).
   */
  public static final int UNBOUND = -2;
  /* character constants */
  private static final char LBRACE = '{';
  private static final char RBRACE = '}';
  private static final char LPAREN = '(';
  private static final char RPAREN = ')';
  private static final char SEMICOLON = ';';
  private static final char COLON = ':';
  private static final char COMMA = ',';

  private static final char LBRACKET = '[';

  private static final char RBRACKET = ']';

  private static final char QUESTIONMARK = '?';

  private static final char EQUAL = '=';

  private static final char LANGLE = '<';

  private static final char RANGLE = '>';

  /** The document being scanned. */
  private final IDocument fDocument;
  /** The partitioning being used for scanning. */
  private final String fPartitioning;
  /** The partition to scan in. */
  private final String fPartition;

  /* internal scan state */

  /** the most recently read character. */
  private char fChar;
  /** the most recently read position. */
  private int fPos;
  /**
   * The most recently used partition.
   */
  private ITypedRegion fCachedPartition = new TypedRegion(-1, 0, "__no_partition_at_all"); //$NON-NLS-1$

  /* preset stop conditions */
  private final StopCondition fNonWSDefaultPart = new NonWhitespaceDefaultPartition();
  private final static StopCondition fNonWS = new NonWhitespace();

  /**
   * Returns <code>true</code> if <code>identifier</code> is probably a type variable or type name,
   * <code>false</code> if it is rather not. This is a heuristic.
   * 
   * @param identifier the identifier to check
   * @return <code>true</code> if <code>identifier</code> is probably a type variable or type name,
   *         <code>false</code> if not
   */
  public static boolean isGenericStarter(CharSequence identifier) {
    /*
     * This heuristic allows any identifiers if they start with an upper case. This will fail when a
     * comparison is made with constants: if (MAX > foo) will try to find the matching '<' which
     * will never come Also, it will fail on lower case types and type variables
     */
    int length = identifier.length();
    if (length > 0
        && (Character.isUpperCase(identifier.charAt(0)) || Character.isUpperCase(identifier.charAt(0)))) {
      for (int i = 1; i < length; i++) { // start at 1 to allow private types
        if (identifier.charAt(i) == '_') {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private final StopCondition fNonIdent = new NonDartIdentifierPartDefaultPartition();

  /**
   * Calls
   * <code>this(document, DartPartitions.DART_PARTITIONING, IDocument.DEFAULT_CONTENT_TYPE)</code>
   * 
   * @param document the document to scan.
   */
  public DartHeuristicScanner(IDocument document) {
    this(document, DartPartitions.DART_PARTITIONING, IDocument.DEFAULT_CONTENT_TYPE);
  }

  /**
   * Creates a new instance.
   * 
   * @param document the document to scan
   * @param partitioning the partitioning to use for scanning
   * @param partition the partition to scan in
   */
  public DartHeuristicScanner(IDocument document, String partitioning, String partition) {
    Assert.isLegal(document != null);
    Assert.isLegal(partitioning != null);
    Assert.isLegal(partition != null);
    fDocument = document;
    fPartitioning = partitioning;
    fPartition = partition;
  }

  /**
   * Returns the position of the closing peer character (forward search). Any scopes introduced by
   * opening peers are skipped. All peers accounted for must reside in the default partition.
   * <p>
   * Note that <code>start</code> must not point to the opening peer, but to the first character
   * being searched.
   * </p>
   * 
   * @param start the start position
   * @param openingPeer the opening peer character (e.g. '{')
   * @param closingPeer the closing peer character (e.g. '}')
   * @return the matching peer character position, or <code>NOT_FOUND</code>
   */
  public int findClosingPeer(int start, final char openingPeer, final char closingPeer) {
    return findClosingPeer(start, UNBOUND, openingPeer, closingPeer);
  }

  /**
   * Returns the position of the closing peer character (forward search). Any scopes introduced by
   * opening peers are skipped. All peers accounted for must reside in the default partition.
   * <p>
   * Note that <code>start</code> must not point to the opening peer, but to the first character
   * being searched.
   * </p>
   * 
   * @param start the start position
   * @param bound the bound
   * @param openingPeer the opening peer character (e.g. '{')
   * @param closingPeer the closing peer character (e.g. '}')
   * @return the matching peer character position, or <code>NOT_FOUND</code>
   */
  public int findClosingPeer(int start, int bound, final char openingPeer, final char closingPeer) {
    Assert.isLegal(start >= 0);

    try {
      CharacterMatch match = new CharacterMatch(new char[] {openingPeer, closingPeer});
      int depth = 1;
      start -= 1;
      while (true) {
        start = scanForward(start + 1, bound, match);
        if (start == NOT_FOUND) {
          return NOT_FOUND;
        }

        if (fDocument.getChar(start) == openingPeer) {
          depth++;
        } else {
          depth--;
        }

        if (depth == 0) {
          return start;
        }
      }

    } catch (BadLocationException e) {
      return NOT_FOUND;
    }
  }

  /**
   * Finds the highest position in <code>fDocument</code> such that the position is &lt;=
   * <code>position</code> and &gt; <code>bound</code> and
   * <code>Character.isWhitespace(fDocument.getChar(pos))</code> evaluates to <code>false</code> and
   * the position is in the default partition.
   * 
   * @param position the first character position in <code>fDocument</code> to be considered
   * @param bound the first position in <code>fDocument</code> to not consider any more, with
   *          <code>bound</code> &lt; <code>position</code>, or <code>UNBOUND</code>
   * @return the highest position of a non-whitespace character in ( <code>bound</code>,
   *         <code>position</code>] that resides in a Dart partition, or <code>NOT_FOUND</code> if
   *         none can be found
   */
  public int findNonWhitespaceBackward(int position, int bound) {
    return scanBackward(position, bound, fNonWSDefaultPart);
  }

  /**
   * Finds the smallest position in <code>fDocument</code> such that the position is &gt;=
   * <code>position</code> and &lt; <code>bound</code> and
   * <code>Character.isWhitespace(fDocument.getChar(pos))</code> evaluates to <code>false</code> and
   * the position is in the default partition.
   * 
   * @param position the first character position in <code>fDocument</code> to be considered
   * @param bound the first position in <code>fDocument</code> to not consider any more, with
   *          <code>bound</code> &gt; <code>position</code>, or <code>UNBOUND</code>
   * @return the smallest position of a non-whitespace character in [ <code>position</code>,
   *         <code>bound</code>) that resides in a Dart partition, or <code>NOT_FOUND</code> if none
   *         can be found
   */
  public int findNonWhitespaceForward(int position, int bound) {
    return scanForward(position, bound, fNonWSDefaultPart);
  }

  /**
   * Finds the smallest position in <code>fDocument</code> such that the position is &gt;=
   * <code>position</code> and &lt; <code>bound</code> and
   * <code>Character.isWhitespace(fDocument.getChar(pos))</code> evaluates to <code>false</code>.
   * 
   * @param position the first character position in <code>fDocument</code> to be considered
   * @param bound the first position in <code>fDocument</code> to not consider any more, with
   *          <code>bound</code> &gt; <code>position</code>, or <code>UNBOUND</code>
   * @return the smallest position of a non-whitespace character in [ <code>position</code>,
   *         <code>bound</code>), or <code>NOT_FOUND</code> if none can be found
   */
  public int findNonWhitespaceForwardInAnyPartition(int position, int bound) {
    return scanForward(position, bound, fNonWS);
  }

  /**
   * Returns the position of the opening peer character (backward search). Any scopes introduced by
   * closing peers are skipped. All peers accounted for must reside in the default partition.
   * <p>
   * Note that <code>start</code> must not point to the closing peer, but to the first character
   * being searched.
   * </p>
   * 
   * @param start the start position
   * @param openingPeer the opening peer character (e.g. '{')
   * @param closingPeer the closing peer character (e.g. '}')
   * @return the matching peer character position, or <code>NOT_FOUND</code>
   */
  public int findOpeningPeer(int start, char openingPeer, char closingPeer) {
    return findOpeningPeer(start, UNBOUND, openingPeer, closingPeer);
  }

  /**
   * Returns the position of the opening peer character (backward search). Any scopes introduced by
   * closing peers are skipped. All peers accounted for must reside in the default partition.
   * <p>
   * Note that <code>start</code> must not point to the closing peer, but to the first character
   * being searched.
   * </p>
   * 
   * @param start the start position
   * @param bound the bound
   * @param openingPeer the opening peer character (e.g. '{')
   * @param closingPeer the closing peer character (e.g. '}')
   * @return the matching peer character position, or <code>NOT_FOUND</code>
   */
  public int findOpeningPeer(int start, int bound, char openingPeer, char closingPeer) {
    Assert.isLegal(start < fDocument.getLength());

    try {
      final CharacterMatch match = new CharacterMatch(new char[] {openingPeer, closingPeer});
      int depth = 1;
      start += 1;
      while (true) {
        start = scanBackward(start - 1, bound, match);
        if (start == NOT_FOUND) {
          return NOT_FOUND;
        }

        if (fDocument.getChar(start) == closingPeer) {
          depth++;
        } else {
          depth--;
        }

        if (depth == 0) {
          return start;
        }
      }

    } catch (BadLocationException e) {
      return NOT_FOUND;
    }
  }

  /**
   * Computes the surrounding block around <code>offset</code>. The search is started at the
   * beginning of <code>offset</code>, i.e. an opening brace at <code>offset</code> will not be part
   * of the surrounding block, but a closing brace will.
   * 
   * @param offset the offset for which the surrounding block is computed
   * @return a region describing the surrounding block, or <code>null</code> if none can be found
   */
  public IRegion findSurroundingBlock(int offset) {
    if (offset < 1 || offset >= fDocument.getLength()) {
      return null;
    }

    int begin = findOpeningPeer(offset - 1, LBRACE, RBRACE);
    int end = findClosingPeer(offset, LBRACE, RBRACE);
    if (begin == NOT_FOUND || end == NOT_FOUND) {
      return null;
    }
    return new Region(begin, end + 1 - begin);
  }

  /**
   * Returns the most recent internal scan position.
   * 
   * @return the most recent internal scan position.
   */
  public int getPosition() {
    return fPos;
  }

  /**
   * Checks if the line seems to be an open condition not followed by a block (i.e. an if, while, or
   * for statement with just one following statement, see example below).
   * 
   * <pre>
   * if (condition)
   *   doStuff();
   * </pre>
   * <p>
   * Algorithm: if the last non-WS, non-Comment code on the line is an if (condition), while
   * (condition), for( expression), do, else, and there is no statement after that
   * </p>
   * 
   * @param position the insert position of the new character
   * @param bound the lowest position to consider
   * @return <code>true</code> if the code is a conditional statement or loop without a block,
   *         <code>false</code> otherwise
   */
  public boolean isBracelessBlockStart(int position, int bound) {
    if (position < 1) {
      return false;
    }

    switch (previousToken(position, bound)) {
      case TokenDO:
      case TokenELSE:
        return true;
      case TokenRPAREN:
        position = findOpeningPeer(fPos, LPAREN, RPAREN);
        if (position > 0) {
          switch (previousToken(position - 1, bound)) {
            case TokenIF:
            case TokenFOR:
            case TokenWHILE:
              return true;
          }
        }
    }

    return false;
  }

  /**
   * Checks whether <code>position</code> resides in a default (Dart) partition of
   * <code>fDocument</code>.
   * 
   * @param position the position to be checked
   * @return <code>true</code> if <code>position</code> is in the default partition of
   *         <code>fDocument</code>, <code>false</code> otherwise
   */
  public boolean isDefaultPartition(int position) {
    return fPartition.equals(getPartition(position).getType());
  }

  /**
   * Returns <code>true</code> if the document, when scanned backwards from <code>start</code>
   * appears to contain a class instance creation, i.e. a possibly qualified name preceded by a
   * <code>new</code> keyword. The <code>start</code> must be at the end of the type name, and
   * before any generic signature or constructor parameter list. The heuristic will return
   * <code>true</code> if <code>start</code> is at the following positions (|):
   * 
   * <pre>
   *  new core. List|&lt;String&gt;(10)
   *  new List |(10)
   *  new  / * comment  * / List |(10)
   * </pre>
   * 
   * but not the following:
   * 
   * <pre>
   *  new core. List&lt;String&gt;(10)|
   *  new core. List&lt;String&gt;|(10)
   *  new List (10)|
   *  List |(10)
   * </pre>
   * 
   * @param start the position where the type name of the class instance creation supposedly ends
   * @param bound the first position in <code>fDocument</code> to not consider any more, with
   *          <code>bound</code> &lt; <code>start</code>, or <code>UNBOUND</code>
   * @return <code>true</code> if the current position looks like after the type name of a class
   *         instance creation
   */
  public boolean looksLikeClassInstanceCreationBackward(int start, int bound) {
    int token = previousToken(start - 1, bound);
    if (token == Symbols.TokenIDENT) { // type name
      token = previousToken(getPosition(), bound);
      while (token == Symbols.TokenOTHER) { // dot of qualification
        token = previousToken(getPosition(), bound);
        if (token != Symbols.TokenIDENT) {
          return false;
        }
        token = previousToken(getPosition(), bound);
      }
      return token == Symbols.TokenNEW;
    }
    return false;
  }

  /**
   * Returns the next token in forward direction, starting at <code>start</code> , and not extending
   * further than <code>bound</code>. The return value is one of the constants defined in
   * {@link Symbols}. After a call, {@link #getPosition()} will return the position just after the
   * scanned token (i.e. the next position that will be scanned).
   * 
   * @param start the first character position in the document to consider
   * @param bound the first position not to consider any more
   * @return a constant from {@link Symbols} describing the next token
   */
  public int nextToken(int start, int bound) {
    int pos = scanForward(start, bound, fNonWSDefaultPart);
    if (pos == NOT_FOUND) {
      return TokenEOF;
    }

    fPos++;

    switch (fChar) {
      case LBRACE:
        return TokenLBRACE;
      case RBRACE:
        return TokenRBRACE;
      case LBRACKET:
        return TokenLBRACKET;
      case RBRACKET:
        return TokenRBRACKET;
      case LPAREN:
        return TokenLPAREN;
      case RPAREN:
        return TokenRPAREN;
      case SEMICOLON:
        return TokenSEMICOLON;
      case COMMA:
        return TokenCOMMA;
      case QUESTIONMARK:
        return TokenQUESTIONMARK;
      case EQUAL:
        if (scanForward(pos + 1, pos + 2, '>') != NOT_FOUND) {
          return TokenDEFUN;
        }
        return TokenEQUAL;
      case LANGLE:
        return TokenLESSTHAN;
      case RANGLE:
        return TokenGREATERTHAN;
    }

    // else
    if (Character.isJavaIdentifierPart(fChar)) {
      // assume an identifier or keyword
      int from = pos, to;
      pos = scanForward(pos + 1, bound, fNonIdent);
      if (pos == NOT_FOUND) {
        to = bound == UNBOUND ? fDocument.getLength() : bound;
      } else {
        to = pos;
      }

      String identOrKeyword;
      try {
        identOrKeyword = fDocument.get(from, to - from);
      } catch (BadLocationException e) {
        return TokenEOF;
      }

      return getToken(identOrKeyword);

    } else {
      // operators, number literals etc
      return TokenOTHER;
    }
  }

  /**
   * Returns the next token in backward direction, starting at <code>start</code>, and not extending
   * further than <code>bound</code>. The return value is one of the constants defined in
   * {@link Symbols}. After a call, {@link #getPosition()} will return the position just before the
   * scanned token starts (i.e. the next position that will be scanned).
   * 
   * @param start the first character position in the document to consider
   * @param bound the first position not to consider any more
   * @return a constant from {@link Symbols} describing the previous token
   */
  public int previousToken(int start, int bound) {
    int pos = scanBackward(start, bound, fNonWSDefaultPart);
    if (pos == NOT_FOUND) {
      return TokenEOF;
    }

    fPos--;

    switch (fChar) {
      case LBRACE:
        return TokenLBRACE;
      case RBRACE:
        return TokenRBRACE;
      case LBRACKET:
        return TokenLBRACKET;
      case RBRACKET:
        return TokenRBRACKET;
      case LPAREN:
        return TokenLPAREN;
      case RPAREN:
        return TokenRPAREN;
      case SEMICOLON:
        return TokenSEMICOLON;
      case COLON:
        return TokenCOLON;
      case COMMA:
        return TokenCOMMA;
      case QUESTIONMARK:
        return TokenQUESTIONMARK;
      case EQUAL:
        return TokenEQUAL;
      case LANGLE:
        return TokenLESSTHAN;
      case RANGLE:
        if (scanBackward(pos - 1, pos - 2, '=') != NOT_FOUND) {
          return TokenDEFUN;
        }
        return TokenGREATERTHAN;
    }

    // else
    if (Character.isJavaIdentifierPart(fChar)) {
      // assume an ident or keyword
      int from, to = pos + 1;
      pos = scanBackward(pos - 1, bound, fNonIdent);
      if (pos == NOT_FOUND) {
        from = bound == UNBOUND ? 0 : bound + 1;
      } else {
        from = pos + 1;
      }

      String identOrKeyword;
      try {
        identOrKeyword = fDocument.get(from, to - from);
      } catch (BadLocationException e) {
        return TokenEOF;
      }

      return getToken(identOrKeyword);

    } else {
      // operators, number literals etc
      return TokenOTHER;
    }

  }

  /**
   * Finds the highest position in <code>fDocument</code> such that the position is &lt;=
   * <code>position</code> and &gt; <code>bound</code> and
   * <code>fDocument.getChar(position) == ch</code> evaluates to <code>true</code> for at least one
   * ch in <code>chars</code> and the position is in the default partition.
   * 
   * @param position the first character position in <code>fDocument</code> to be considered
   * @param bound the first position in <code>fDocument</code> to not consider any more, with
   *          <code>bound</code> &lt; <code>position</code>, or <code>UNBOUND</code>
   * @param ch the <code>char</code> to search for
   * @return the highest position of one element in <code>chars</code> in ( <code>bound</code>,
   *         <code>position</code>] that resides in a Dart partition, or <code>NOT_FOUND</code> if
   *         none can be found
   */
  public int scanBackward(int position, int bound, char ch) {
    return scanBackward(position, bound, new CharacterMatch(ch));
  }

  /**
   * Finds the highest position in <code>fDocument</code> such that the position is &lt;=
   * <code>position</code> and &gt; <code>bound</code> and
   * <code>fDocument.getChar(position) == ch</code> evaluates to <code>true</code> for at least one
   * ch in <code>chars</code> and the position is in the default partition.
   * 
   * @param position the first character position in <code>fDocument</code> to be considered
   * @param bound the first position in <code>fDocument</code> to not consider any more, with
   *          <code>bound</code> &lt; <code>position</code>, or <code>UNBOUND</code>
   * @param chars an array of <code>char</code> to search for
   * @return the highest position of one element in <code>chars</code> in ( <code>bound</code>,
   *         <code>position</code>] that resides in a Dart partition, or <code>NOT_FOUND</code> if
   *         none can be found
   */
  public int scanBackward(int position, int bound, char[] chars) {
    return scanBackward(position, bound, new CharacterMatch(chars));
  }

  /**
   * Finds the highest position <code>p</code> in <code>fDocument</code> such that
   * <code>bound</code> &lt; <code>p</code> &lt;= <code>start</code> and
   * <code>condition.stop(fDocument.getChar(p), p)</code> evaluates to <code>true</code>.
   * 
   * @param start the first character position in <code>fDocument</code> to be considered
   * @param bound the first position in <code>fDocument</code> to not consider any more, with
   *          <code>bound</code> &lt; <code>start</code>, or <code>UNBOUND</code>
   * @param condition the <code>StopCondition</code> to check
   * @return the highest position in (<code>bound</code>, <code>start</code> for which
   *         <code>condition</code> holds, or <code>NOT_FOUND</code> if none can be found
   */
  public int scanBackward(int start, int bound, StopCondition condition) {
    if (bound == UNBOUND) {
      bound = -1;
    }

    Assert.isLegal(bound >= -1);
    Assert.isLegal(start < fDocument.getLength());

    try {
      fPos = start;
      while (fPos > bound) {

        fChar = fDocument.getChar(fPos);
        if (condition.stop(fChar, fPos, false)) {
          return fPos;
        }

        fPos = condition.nextPosition(fPos, false);
      }
    } catch (BadLocationException e) {
    }
    return NOT_FOUND;
  }

  /**
   * Finds the lowest position in <code>fDocument</code> such that the position is &gt;=
   * <code>position</code> and &lt; <code>bound</code> and
   * <code>fDocument.getChar(position) == ch</code> evaluates to <code>true</code> and the position
   * is in the default partition.
   * 
   * @param position the first character position in <code>fDocument</code> to be considered
   * @param bound the first position in <code>fDocument</code> to not consider any more, with
   *          <code>bound</code> &gt; <code>position</code>, or <code>UNBOUND</code>
   * @param ch the <code>char</code> to search for
   * @return the lowest position of <code>ch</code> in (<code>bound</code>, <code>position</code>]
   *         that resides in a Dart partition, or <code>NOT_FOUND</code> if none can be found
   */
  public int scanForward(int position, int bound, char ch) {
    return scanForward(position, bound, new CharacterMatch(ch));
  }

  /**
   * Finds the lowest position in <code>fDocument</code> such that the position is &gt;=
   * <code>position</code> and &lt; <code>bound</code> and
   * <code>fDocument.getChar(position) == ch</code> evaluates to <code>true</code> for at least one
   * ch in <code>chars</code> and the position is in the default partition.
   * 
   * @param position the first character position in <code>fDocument</code> to be considered
   * @param bound the first position in <code>fDocument</code> to not consider any more, with
   *          <code>bound</code> &gt; <code>position</code>, or <code>UNBOUND</code>
   * @param chars an array of <code>char</code> to search for
   * @return the lowest position of a non-whitespace character in [ <code>position</code>,
   *         <code>bound</code>) that resides in a Dart partition, or <code>NOT_FOUND</code> if none
   *         can be found
   */
  public int scanForward(int position, int bound, char[] chars) {
    return scanForward(position, bound, new CharacterMatch(chars));
  }

  /**
   * Finds the lowest position <code>p</code> in <code>fDocument</code> such that <code>start</code>
   * &lt;= p &lt; <code>bound</code> and <code>condition.stop(fDocument.getChar(p), p)</code>
   * evaluates to <code>true</code>.
   * 
   * @param start the first character position in <code>fDocument</code> to be considered
   * @param bound the first position in <code>fDocument</code> to not consider any more, with
   *          <code>bound</code> &gt; <code>start</code>, or <code>UNBOUND</code>
   * @param condition the <code>StopCondition</code> to check
   * @return the lowest position in [<code>start</code>, <code>bound</code>) for which
   *         <code>condition</code> holds, or <code>NOT_FOUND</code> if none can be found
   */
  public int scanForward(int start, int bound, StopCondition condition) {
    Assert.isLegal(start >= 0);

    if (bound == UNBOUND) {
      bound = fDocument.getLength();
    }

    Assert.isLegal(bound <= fDocument.getLength());

    try {
      fPos = start;
      while (fPos < bound) {

        fChar = fDocument.getChar(fPos);
        if (condition.stop(fChar, fPos, true)) {
          return fPos;
        }

        fPos = condition.nextPosition(fPos, true);
      }
    } catch (BadLocationException e) {
    }
    return NOT_FOUND;
  }

  /**
   * Returns <code>true</code> if <code>region</code> contains <code>position</code>.
   * 
   * @param region a region
   * @param position an offset
   * @return <code>true</code> if <code>region</code> contains <code>position</code>
   */
  private boolean contains(IRegion region, int position) {
    int offset = region.getOffset();
    return offset <= position && position < offset + region.getLength();
  }

  /**
   * Returns the partition at <code>position</code>.
   * 
   * @param position the position to get the partition for
   * @return the partition at <code>position</code> or a dummy zero-length partition if accessing
   *         the document fails
   */
  private ITypedRegion getPartition(int position) {
    if (!contains(fCachedPartition, position)) {
      Assert.isTrue(position >= 0);
      Assert.isTrue(position <= fDocument.getLength());

      try {
        fCachedPartition = TextUtilities.getPartition(fDocument, fPartitioning, position, false);
      } catch (BadLocationException e) {
        fCachedPartition = new TypedRegion(position, 0, "__no_partition_at_all"); //$NON-NLS-1$
      }
    }

    return fCachedPartition;
  }

  /**
   * Returns one of the keyword constants or <code>TokenIDENT</code> for a scanned identifier.
   * 
   * @param s a scanned identifier
   * @return one of the constants defined in {@link Symbols}
   */
  private int getToken(String s) {
    Assert.isNotNull(s);
    switch (s.length()) {
      case 2:
        if ("if".equals(s)) {
          return TokenIF;
        }
        if ("do".equals(s)) {
          return TokenDO;
        }
        if ("is".equals(s)) {
          return TokenINSTANCEOF;
        }
        if ("in".equals(s)) {
          return TokenIN;
        }
        break;
      case 3:
        if ("for".equals(s)) {
          return TokenFOR;
        }
        if ("get".equals(s)) {
          return TokenGET;
        }
        if ("try".equals(s)) {
          return TokenTRY;
        }
        if ("new".equals(s)) {
          return TokenNEW;
        }
        if ("var".equals(s)) {
          return TokenVAR;
        }
        if ("set".equals(s)) {
          return TokenSET;
        }
        break;
      case 4:
        if ("case".equals(s)) {
          return TokenCASE;
        }
        if ("call".equals(s)) {
          return TokenCALL;
        }
        if ("const".equals(s)) {
          return TokenCONST;
        }
        if ("else".equals(s)) {
          return TokenELSE;
        }
        if ("null".equals(s)) {
          return TokenNULL;
        }
        if ("this".equals(s)) {
          return TokenTHIS;
        }
        if ("true".equals(s)) {
          return TokenTRUE;
        }
        if ("void".equals(s)) {
          return TokenVOID;
        }
        break;
      case 5:
        if ("break".equals(s)) {
          return TokenBREAK;
        }
        if ("catch".equals(s)) {
          return TokenCATCH;
        }
        if ("class".equals(s)) {
          return TokenCLASS;
        }
        if ("const".equals(s)) {
          return TokenCONST;
        }
        if ("false".equals(s)) {
          return TokenFALSE;
        }
        if ("final".equals(s)) {
          return TokenFINAL;
        }
        if ("super".equals(s)) {
          return TokenSUPER;
        }
        if ("throw".equals(s)) {
          return TokenTHROW;
        }
        if ("while".equals(s)) {
          return TokenWHILE;
        }
        break;
      case 6:
        if ("assert".equals(s)) {
          return TokenASSERT;
        }
        if ("import".equals(s)) {
          return TokenIMPORT;
        }
        if ("native".equals(s)) {
          return TokenNATIVE;
        }
        if ("negate".equals(s)) {
          return TokenNEGATE;
        }
        if ("prefix".equals(s)) {
          return TokenPREFIX;
        }
        if ("return".equals(s)) {
          return TokenRETURN;
        }
        if ("source".equals(s)) {
          return TokenSOURCE;
        }
        if ("static".equals(s)) {
          return TokenSTATIC;
        }
        if ("switch".equals(s)) {
          return TokenSWITCH;
        }
        break;
      case 7:
        if ("default".equals(s)) {
          return TokenDEFAULT;
        }
        if ("extends".equals(s)) {
          return TokenEXTENDS;
        }
        if ("factory".equals(s)) {
          return TokenFACTORY;
        }
        if ("finally".equals(s)) {
          return TokenFINALLY;
        }
        if ("library".equals(s)) {
          return TokenLIBRARY;
        }
        if ("typedef".equals(s)) {
          return TokenTYPEDEF;
        }
        break;
      case 8:
        if ("abstract".equals(s)) {
          return TokenABSTRACT;
        }
        if ("continue".equals(s)) {
          return TokenCONTINUE;
        }
        if ("operator".equals(s)) {
          return TokenOPERATOR;
        }
        if ("resource".equals(s)) {
          return TokenRESOURCE;
        }
        break;
      case 9:
        if ("interface".equals(s)) {
          return TokenINTERFACE;
        }
        break;
      case 10:
        if ("implements".equals(s)) {
          return TokenIMPLEMENTS;
        }
        break;
    }
    return TokenIDENT;
  }

}
