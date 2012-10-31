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

import com.google.dart.compiler.parser.DartParser;
import com.google.dart.tools.ui.DartUiDebug;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightings;
import com.google.dart.tools.ui.internal.text.functions.AbstractDartScanner;
import com.google.dart.tools.ui.internal.text.functions.CombinedWordRule;
import com.google.dart.tools.ui.internal.text.functions.DartWhitespaceDetector;
import com.google.dart.tools.ui.internal.text.functions.DartWordDetector;
import com.google.dart.tools.ui.internal.text.functions.ISourceVersionDependent;
import com.google.dart.tools.ui.text.IColorManager;
import com.google.dart.tools.ui.text.IDartColorConstants;

import static com.google.dart.compiler.parser.Token.AS;
import static com.google.dart.compiler.parser.Token.BREAK;
import static com.google.dart.compiler.parser.Token.IS;
import static com.google.dart.compiler.parser.Token.LIBRARY;
import static com.google.dart.compiler.parser.Token.NATIVE;
import static com.google.dart.compiler.parser.Token.WHILE;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.util.PropertyChangeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A Dart code scanner.
 */
public final class DartCodeScanner extends AbstractDartScanner {

  /**
   * Rule to detect Dart brackets.
   */
  private static class BracketRule implements IRule {

    /** Dart brackets */
    private static final char[] DART_BRACKETS = {'(', ')', '{', '}', '[', ']'};

    /** Token to return for this rule */
    private final IToken fToken;

    /**
     * Creates a new bracket rule.
     * 
     * @param token Token to use for this rule
     */
    public BracketRule(IToken token) {
      fToken = token;
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner) {

      int character = scanner.read();
      if (isBracket((char) character)) {
        do {
          character = scanner.read();
        } while (isBracket((char) character));
        scanner.unread();
        return fToken;
      } else {
        scanner.unread();
        return Token.UNDEFINED;
      }
    }

    /**
     * Is this character a bracket character?
     * 
     * @param character Character to determine whether it is a bracket character
     * @return <code>true</code> iff the character is a bracket, <code>false</code> otherwise.
     */
    public boolean isBracket(char character) {
      for (int index = 0; index < DART_BRACKETS.length; index++) {
        if (DART_BRACKETS[index] == character) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Rule to detect Dart directives.
   */
  private static class DirectiveRule implements IRule {
    char[] sequence;
    Token token;

    DirectiveRule(String name, Token token) {
      this.sequence = name.toCharArray();
      this.token = token;
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner) {
      int c = scanner.read();
      if (c == '#') {
        c = scanner.read();
        int readCount = 2;
        if (c == sequence[0]) {
          for (int i = 1; i < sequence.length; i++) {
            if (sequence[i] != scanner.read()) {
              while (readCount-- > 0) {
                scanner.unread();
              }
              return Token.UNDEFINED;
            }
            readCount += 1;
          }
          return token;
        } else {
          scanner.unread();
        }
      }
      scanner.unread();
      return Token.UNDEFINED;
    }
  }

  /**
   * Rule to detect Dart operators.
   */
  private static class OperatorRule implements IRule {

    /** Dart operators */
    private static final char[] DART_OPERATORS = {
        ';', '.', '=', '/', '\\', '+', '-', '*', '<', '>', ':', '?', '!', ',', '|', '&', '^', '%',
        '~'};
    /** Token to return for this rule */
    private final IToken fToken;

    /**
     * Creates a new operator rule.
     * 
     * @param token Token to use for this rule
     */
    public OperatorRule(IToken token) {
      fToken = token;
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner) {

      int character = scanner.read();
      if (isOperator((char) character)) {
        do {
          character = scanner.read();
        } while (isOperator((char) character));
        scanner.unread();
        return fToken;
      } else {
        scanner.unread();
        return Token.UNDEFINED;
      }
    }

    /**
     * Is this character an operator character?
     * 
     * @param character Character to determine whether it is an operator character
     * @return <code>true</code> iff the character is an operator, <code>false</code> otherwise.
     */
    public boolean isOperator(char character) {
      for (int index = 0; index < DART_OPERATORS.length; index++) {
        if (DART_OPERATORS[index] == character) {
          return true;
        }
      }
      return false;
    }
  }

  @SuppressWarnings("unused")
  private static class VersionedWordMatcher extends CombinedWordRule.WordMatcher implements
      ISourceVersionDependent {

    private final IToken fDefaultToken;
    private final String fVersion;
    private boolean fIsVersionMatch;

    public VersionedWordMatcher(IToken defaultToken, String version, String currentVersion) {
      fDefaultToken = defaultToken;
      fVersion = version;
      setSourceVersion(currentVersion);
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner, CombinedWordRule.CharacterBuffer word) {
      IToken token = super.evaluate(scanner, word);

      if (fIsVersionMatch || token.isUndefined()) {
        return token;
      }

      return fDefaultToken;
    }

    @Override
    public void setSourceVersion(String version) {
      fIsVersionMatch = fVersion.compareTo(version) <= 0;
    }
  }

  public static final String[] DIRECTIVES;
  public static final String[] KEYWORDS;

  public static final String[] OPERATORS;

  static {
    ArrayList<String> directives = new ArrayList<String>();
    ArrayList<String> keywords = new ArrayList<String>();
    ArrayList<String> operators = new ArrayList<String>();
    com.google.dart.compiler.parser.Token[] tokens = com.google.dart.compiler.parser.Token.values();
    for (int i = 0; i < tokens.length; i++) {
      com.google.dart.compiler.parser.Token token = tokens[i];
      if ((BREAK.ordinal() <= token.ordinal() && token.ordinal() <= WHILE.ordinal())
          || token.ordinal() == AS.ordinal() || token.ordinal() == IS.ordinal()) {
        keywords.add(token.getSyntax());
      }
      if (!DartUiDebug.USE_ONLY_SEMANTIC_HIGHLIGHTER) {
        if ((LIBRARY.ordinal() <= token.ordinal()) && (token.ordinal() <= NATIVE.ordinal())) {
          String name = token.getSyntax();
          directives.add(name.substring(1));
        }
      }
      if (token.isBinaryOperator() || token.isUnaryOperator()) {
        operators.add(token.getSyntax());
      }
    }
    if (!DartUiDebug.USE_ONLY_SEMANTIC_HIGHLIGHTER) {
      for (String kw : DartParser.PSEUDO_KEYWORDS) {
        keywords.add(kw);
      }
    }
    DIRECTIVES = directives.toArray(new String[directives.size()]);
    KEYWORDS = keywords.toArray(new String[keywords.size()]);
    OPERATORS = operators.toArray(new String[operators.size()]);
  }

  private static final String RETURN = "return"; //$NON-NLS-1$

  private static String[] fgConstants = {"false", "null", "true"}; //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

  private static final String ANNOTATION_BASE_KEY = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
      + SemanticHighlightings.ANNOTATION;
  private static final String ANNOTATION_COLOR_KEY = ANNOTATION_BASE_KEY
      + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_COLOR_SUFFIX;

  private static String[] fgTokenProperties = {
      IDartColorConstants.JAVA_KEYWORD, IDartColorConstants.JAVA_STRING,
      IDartColorConstants.JAVA_DEFAULT, IDartColorConstants.JAVA_KEYWORD_RETURN,
      IDartColorConstants.JAVA_OPERATOR, IDartColorConstants.JAVA_BRACKET, ANNOTATION_COLOR_KEY,};

  /**
   * Creates a Dart code scanner
   * 
   * @param manager the color manager
   * @param store the preference store
   */
  public DartCodeScanner(IColorManager manager, IPreferenceStore store) {
    super(manager, store);
    initialize();
  }

  @Override
  public void adaptToPreferenceChange(PropertyChangeEvent event) {
    DartX.todo();
    super.adaptToPreferenceChange(event);
  }

  @Override
  public boolean affectsBehavior(PropertyChangeEvent event) {
    DartX.todo();
    return super.affectsBehavior(event);
  }

  @Override
  protected List<IRule> createRules() {

    List<IRule> rules = new ArrayList<IRule>();
    Token token;

    // Add rule for character constants.
    if (!DartUiDebug.USE_ONLY_SEMANTIC_HIGHLIGHTER) {
      token = getToken(IDartColorConstants.JAVA_STRING);
      rules.add(new SingleLineRule("'", "'", token, '\\')); //$NON-NLS-2$ //$NON-NLS-1$
    }

    // Add generic whitespace rule.
    rules.add(new WhitespaceRule(new DartWhitespaceDetector()));

    // Add word rule for new keywords, 4077
    DartWordDetector wordDetector = new DartWordDetector();
    token = getToken(IDartColorConstants.JAVA_DEFAULT);
    CombinedWordRule combinedWordRule = new CombinedWordRule(wordDetector, token);

    // Add rule for operators
    token = getToken(IDartColorConstants.JAVA_OPERATOR);
    rules.add(new OperatorRule(token));

    // Add rule for brackets
    token = getToken(IDartColorConstants.JAVA_BRACKET);
    rules.add(new BracketRule(token));

    // Add word rule for keyword 'return'.
    if (!DartUiDebug.USE_ONLY_SEMANTIC_HIGHLIGHTER) {
      CombinedWordRule.WordMatcher returnWordRule = new CombinedWordRule.WordMatcher();
      token = getToken(IDartColorConstants.JAVA_KEYWORD_RETURN);
      returnWordRule.addWord(RETURN, token);
      combinedWordRule.addWordMatcher(returnWordRule);
    }

    // Add word rule for keywords and constants.
    CombinedWordRule.WordMatcher wordRule = new CombinedWordRule.WordMatcher();
    token = getToken(IDartColorConstants.JAVA_KEYWORD);
    for (int i = 0; i < KEYWORDS.length; i++) {
      wordRule.addWord(KEYWORDS[i], token);
    }
    for (int i = 0; i < fgConstants.length; i++) {
      wordRule.addWord(fgConstants[i], token);
    }
    if (!DartUiDebug.USE_ONLY_SEMANTIC_HIGHLIGHTER) {
      for (int i = 0; i < DIRECTIVES.length; i++) {
        rules.add(new DirectiveRule(DIRECTIVES[i], token));
      }
      rules.add(new DirectiveRule("!", token));
    }

    combinedWordRule.addWordMatcher(wordRule);

    rules.add(combinedWordRule);

    setDefaultReturnToken(getToken(IDartColorConstants.JAVA_DEFAULT));
    return rules;
  }

  @Override
  protected String getBoldKey(String colorKey) {
    if ((ANNOTATION_COLOR_KEY).equals(colorKey)) {
      return ANNOTATION_BASE_KEY + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX;
    }
    return super.getBoldKey(colorKey);
  }

  @Override
  protected String getItalicKey(String colorKey) {
    if ((ANNOTATION_COLOR_KEY).equals(colorKey)) {
      return ANNOTATION_BASE_KEY + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX;
    }
    return super.getItalicKey(colorKey);
  }

  @Override
  protected String getStrikethroughKey(String colorKey) {
    if ((ANNOTATION_COLOR_KEY).equals(colorKey)) {
      return ANNOTATION_BASE_KEY
          + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_STRIKETHROUGH_SUFFIX;
    }
    return super.getStrikethroughKey(colorKey);
  }

  @Override
  protected String[] getTokenProperties() {
    return fgTokenProperties;
  }

  @Override
  protected String getUnderlineKey(String colorKey) {
    if ((ANNOTATION_COLOR_KEY).equals(colorKey)) {
      return ANNOTATION_BASE_KEY
          + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_UNDERLINE_SUFFIX;
    }
    return super.getUnderlineKey(colorKey);
  }
}
