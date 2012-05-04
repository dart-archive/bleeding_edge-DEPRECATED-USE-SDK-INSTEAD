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
package com.google.dart.tools.core.internal.formatter;

import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.parser.Token;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.formatter.CodeFormatter;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.TextEdit;

import java.util.HashMap;
import java.util.Map;

public class DefaultCodeFormatter extends CodeFormatter {
  public static final String UTF_8 = "UTF-8"; //$NON-NLS-1$
  public static final String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$
  public static final String EMPTY_STRING = ""; //$NON-NLS-1$
  public static final int[] EMPTY_INT_ARRAY = new int[0];

  /**
   * Debug trace
   */
  public static boolean DEBUG = false;

  // Mask for code formatter kinds
  private static final int K_MASK = K_UNKNOWN | K_EXPRESSION | K_STATEMENTS
      | K_CLASS_BODY_DECLARATIONS | K_COMPILATION_UNIT | K_SINGLE_LINE_COMMENT
      | K_MULTI_LINE_COMMENT | K_JAVA_DOC;

  // Scanner use to probe the kind of the source given to the formatter
  private static Scanner PROBING_SCANNER;

  private CodeSnippetParsingUtil codeSnippetParsingUtil;
  private Map<String, String> defaultCompilerOptions;

  private CodeFormatterVisitor newCodeFormatter;
  private Map<String, String> options;

  private DefaultCodeFormatterOptions preferences;

  public DefaultCodeFormatter() {
    this(
        new DefaultCodeFormatterOptions(DefaultCodeFormatterConstants.getDartConventionsSettings()),
        null);
  }

  public DefaultCodeFormatter(DefaultCodeFormatterOptions preferences) {
    this(preferences, null);
  }

  public DefaultCodeFormatter(DefaultCodeFormatterOptions defaultCodeFormatterOptions,
      Map<String, String> options) {
    if (options != null) {
      this.options = options;
      this.preferences = new DefaultCodeFormatterOptions(options);
    } else {
      this.options = DartCore.getOptions();
      this.preferences = new DefaultCodeFormatterOptions(
          DefaultCodeFormatterConstants.getDartConventionsSettings());
    }
    this.defaultCompilerOptions = getDefaultCompilerOptions();
    if (defaultCodeFormatterOptions != null) {
      this.preferences.set(defaultCodeFormatterOptions.getMap());
    }
  }

  public DefaultCodeFormatter(Map<String, String> options) {
    this(null, options);
  }

  @Override
  public String createIndentationString(final int indentationLevel) {
    if (indentationLevel < 0) {
      throw new IllegalArgumentException();
    }

    int tabs = 0;
    int spaces = 0;
    switch (this.preferences.tab_char) {
      case DefaultCodeFormatterOptions.SPACE:
        spaces = indentationLevel * this.preferences.tab_size;
        break;
      case DefaultCodeFormatterOptions.TAB:
        tabs = indentationLevel;
        break;
      case DefaultCodeFormatterOptions.MIXED:
        int tabSize = this.preferences.tab_size;
        if (tabSize != 0) {
          int spaceEquivalents = indentationLevel * this.preferences.indentation_size;
          tabs = spaceEquivalents / tabSize;
          spaces = spaceEquivalents % tabSize;
        }
        break;
      default:
        return EMPTY_STRING;
    }
    if (tabs == 0 && spaces == 0) {
      return EMPTY_STRING;
    }
    StringBuffer buffer = new StringBuffer(tabs + spaces);
    for (int i = 0; i < tabs; i++) {
      buffer.append('\t');
    }
    for (int i = 0; i < spaces; i++) {
      buffer.append(' ');
    }
    return buffer.toString();
  }

  /**
   * @see org.eclipse.jdt.core.formatter.CodeFormatter#format(int, java.lang.String, int, int, int,
   *      java.lang.String)
   */
  @Override
  public TextEdit format(int kind, String source, int offset, int length, int indentationLevel,
      String lineSeparator) {
    if (offset < 0 || length < 0 || length > source.length()) {
      throw new IllegalArgumentException();
    }

    switch (kind & K_MASK) {
      case K_JAVA_DOC:
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=102780
        // use the integrated comment formatter to format comment
        return formatComment(
            kind & K_MASK,
            source,
            indentationLevel,
            lineSeparator,
            new IRegion[] {new Region(offset, length)});
        // $FALL-THROUGH$ - fall through next case when old comment formatter is
        // activated
      case K_MULTI_LINE_COMMENT:
      case K_SINGLE_LINE_COMMENT:
        return formatComment(
            kind & K_MASK,
            source,
            indentationLevel,
            lineSeparator,
            new IRegion[] {new Region(offset, length)});
    }

    return format(
        kind,
        source,
        new IRegion[] {new Region(offset, length)},
        indentationLevel,
        lineSeparator);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TextEdit format(int kind, String source, IRegion[] regions, int indentationLevel,
      String lineSeparator) {
    if (!regionsSatisfiesPreconditions(regions, source.length())) {
      throw new IllegalArgumentException();
    }

    this.codeSnippetParsingUtil = new CodeSnippetParsingUtil();
    boolean includeComments = (kind & F_INCLUDE_COMMENTS) != 0;
    switch (kind & K_MASK) {
      case K_CLASS_BODY_DECLARATIONS:
        return formatClassBodyDeclarations(
            source,
            indentationLevel,
            lineSeparator,
            regions,
            includeComments);
      case K_COMPILATION_UNIT:
        return formatCompilationUnit(
            source,
            indentationLevel,
            lineSeparator,
            regions,
            includeComments);
      case K_EXPRESSION:
        return formatExpression(source, indentationLevel, lineSeparator, regions, includeComments);
      case K_STATEMENTS:
        return formatStatements(source, indentationLevel, lineSeparator, regions, includeComments);
      case K_UNKNOWN:
        return probeFormatting(source, indentationLevel, lineSeparator, regions, includeComments);
      case K_JAVA_DOC:
      case K_MULTI_LINE_COMMENT:
      case K_SINGLE_LINE_COMMENT:
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=204091
        throw new IllegalArgumentException();
    }
    return null;
  }

  public String getDebugOutput() {
    return this.newCodeFormatter.scribe.toString();
  }

  private TextEdit formatClassBodyDeclarations(String source, int indentationLevel,
      String lineSeparator, IRegion[] regions, boolean includeComments) {
    DartNode[] bodyDeclarations = this.codeSnippetParsingUtil.parseClassBodyDeclarations(
        source.toCharArray(),
        getDefaultCompilerOptions(),
        true);

    if (bodyDeclarations == null) {
      // a problem occurred while parsing the source
      return null;
    }
    return internalFormatClassBodyDeclarations(
        source,
        indentationLevel,
        lineSeparator,
        bodyDeclarations,
        regions,
        includeComments);
  }

  /*
   * Format a javadoc comment. Since bug 102780 this is done by a specific method when new javadoc
   * formatter is activated.
   */
  private TextEdit formatComment(int kind, String source, int indentationLevel,
      String lineSeparator, IRegion[] regions) {
    Object oldOption = oldCommentFormatOption();
    boolean isFormattingComments = false;
    if (oldOption == null) {
      switch (kind & K_MASK) {
        case K_SINGLE_LINE_COMMENT:
          isFormattingComments = DefaultCodeFormatterConstants.TRUE.equals(this.options.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT));
          break;
        case K_MULTI_LINE_COMMENT:
          isFormattingComments = DefaultCodeFormatterConstants.TRUE.equals(this.options.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT));
          break;
        case K_JAVA_DOC:
          isFormattingComments = DefaultCodeFormatterConstants.TRUE.equals(this.options.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT));
      }
    } else {
      isFormattingComments = DefaultCodeFormatterConstants.TRUE.equals(oldOption);
    }
    if (isFormattingComments) {
      if (lineSeparator != null) {
        this.preferences.line_separator = lineSeparator;
      } else {
        this.preferences.line_separator = LINE_SEPARATOR;
      }
      this.preferences.initial_indentation_level = indentationLevel;
      if (this.codeSnippetParsingUtil == null) {
        this.codeSnippetParsingUtil = new CodeSnippetParsingUtil();
      }
      this.codeSnippetParsingUtil.parseCompilationUnit(
          source.toCharArray(),
          getDefaultCompilerOptions(),
          true);
      this.newCodeFormatter = new CodeFormatterVisitor(
          this.preferences,
          regions,
          this.codeSnippetParsingUtil,
          true);
      IRegion coveredRegion = getCoveredRegion(regions);
      int start = coveredRegion.getOffset();
      int end = start + coveredRegion.getLength();
      this.newCodeFormatter.formatComment(kind, source, start, end, indentationLevel);
      return this.newCodeFormatter.scribe.getRootEdit();
    }
    return null;
  }

  private TextEdit formatCompilationUnit(String source, int indentationLevel, String lineSeparator,
      IRegion[] regions, boolean includeComments) {
    DartUnit compilationUnitDeclaration = this.codeSnippetParsingUtil.parseCompilationUnit(
        source.toCharArray(),
        getDefaultCompilerOptions(),
        true);

    if (lineSeparator != null) {
      this.preferences.line_separator = lineSeparator;
    } else {
      this.preferences.line_separator = LINE_SEPARATOR;
    }
    this.preferences.initial_indentation_level = indentationLevel;

    this.newCodeFormatter = new CodeFormatterVisitor(
        this.preferences,
        regions,
        this.codeSnippetParsingUtil,
        includeComments);

    return this.newCodeFormatter.format(source, compilationUnitDeclaration);
  }

  private TextEdit formatExpression(String source, int indentationLevel, String lineSeparator,
      IRegion[] regions, boolean includeComments) {
    DartExpression expression = this.codeSnippetParsingUtil.parseExpression(
        source.toCharArray(),
        getDefaultCompilerOptions(),
        true);

    if (expression == null) {
      // a problem occurred while parsing the source
      return null;
    }
    return internalFormatExpression(
        source,
        indentationLevel,
        lineSeparator,
        expression,
        regions,
        includeComments);
  }

  private TextEdit formatStatements(String source, int indentationLevel, String lineSeparator,
      IRegion[] regions, boolean includeComments) {
    DartCore.notYetImplemented();
    // TODO why do we use a constructor here?
    DartMethodDefinition constructorDeclaration = this.codeSnippetParsingUtil.parseStatements(
        source.toCharArray(),
        getDefaultCompilerOptions(),
        true,
        false);

    try {
      if (constructorDeclaration.getFunction().getBody().getStatements() == null) {
        // a problem occured while parsing the source
        return null;
      }
    } catch (NullPointerException ex) {
      return null; // a problem occured while parsing the source
    }
    return internalFormatStatements(
        source,
        indentationLevel,
        lineSeparator,
        constructorDeclaration,
        regions,
        includeComments);
  }

  private IRegion getCoveredRegion(IRegion[] regions) {
    int length = regions.length;
    if (length == 1) {
      return regions[0];
    }

    int offset = regions[0].getOffset();
    IRegion lastRegion = regions[length - 1];

    return new Region(offset, lastRegion.getOffset() + lastRegion.getLength() - offset);
  }

  private Map<String, String> getDefaultCompilerOptions() {
    if (this.defaultCompilerOptions == null) {
      Map<String, String> optionsMap = new HashMap<String, String>(30);
      DartCore.notYetImplemented();
//      optionsMap.put(CompilerOptions.OPTION_LocalVariableAttribute,
//          CompilerOptions.DO_NOT_GENERATE);
//      optionsMap.put(CompilerOptions.OPTION_LineNumberAttribute,
//          CompilerOptions.DO_NOT_GENERATE);
//      optionsMap.put(CompilerOptions.OPTION_SourceFileAttribute,
//          CompilerOptions.DO_NOT_GENERATE);
//      optionsMap.put(CompilerOptions.OPTION_PreserveUnusedLocal,
//          CompilerOptions.PRESERVE);
//      optionsMap.put(CompilerOptions.OPTION_DocCommentSupport,
//          CompilerOptions.DISABLED);
//      optionsMap.put(CompilerOptions.OPTION_ReportMethodWithConstructorName,
//          CompilerOptions.IGNORE);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportOverridingPackageDefaultMethod,
//          CompilerOptions.IGNORE);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportOverridingMethodWithoutSuperInvocation,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportDeprecation,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportDeprecationInDeprecatedCode,
//          CompilerOptions.DISABLED);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportDeprecationWhenOverridingDeprecatedMethod,
//          CompilerOptions.DISABLED);
//      optionsMap.put(CompilerOptions.OPTION_ReportHiddenCatchBlock,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportUnusedLocal,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportUnusedObjectAllocation,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportUnusedParameter,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportUnusedImport,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportSyntheticAccessEmulation,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportNoEffectAssignment,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportNonExternalizedStringLiteral,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportNoImplicitStringConversion,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportNonStaticAccessToStatic,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportIndirectStaticAccess,
//          CompilerOptions.IGNORE);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportIncompatibleNonInheritedInterfaceMethod,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportUnusedPrivateMember,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportLocalVariableHiding,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportFieldHiding,
//          CompilerOptions.IGNORE);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportPossibleAccidentalBooleanAssignment,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportEmptyStatement,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportAssertIdentifier,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportEnumIdentifier,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportUndocumentedEmptyBlock,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportUnnecessaryTypeCheck,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportInvalidJavadoc,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportInvalidJavadocTagsVisibility,
//          CompilerOptions.PUBLIC);
//      optionsMap.put(CompilerOptions.OPTION_ReportInvalidJavadocTags,
//          CompilerOptions.DISABLED);
//      optionsMap.put(CompilerOptions.OPTION_ReportMissingJavadocTagDescription,
//          CompilerOptions.RETURN_TAG);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportInvalidJavadocTagsDeprecatedRef,
//          CompilerOptions.DISABLED);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportInvalidJavadocTagsNotVisibleRef,
//          CompilerOptions.DISABLED);
//      optionsMap.put(CompilerOptions.OPTION_ReportMissingJavadocTags,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportMissingJavadocTagsVisibility,
//          CompilerOptions.PUBLIC);
//      optionsMap.put(CompilerOptions.OPTION_ReportMissingJavadocTagsOverriding,
//          CompilerOptions.DISABLED);
//      optionsMap.put(CompilerOptions.OPTION_ReportMissingJavadocComments,
//          CompilerOptions.IGNORE);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportMissingJavadocCommentsVisibility,
//          CompilerOptions.IGNORE);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportMissingJavadocCommentsOverriding,
//          CompilerOptions.DISABLED);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportFinallyBlockNotCompletingNormally,
//          CompilerOptions.IGNORE);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportUnusedDeclaredThrownException,
//          CompilerOptions.IGNORE);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportUnusedDeclaredThrownExceptionWhenOverriding,
//          CompilerOptions.DISABLED);
//      optionsMap.put(CompilerOptions.OPTION_ReportUnqualifiedFieldAccess,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_Compliance,
//          CompilerOptions.VERSION_1_4);
//      optionsMap.put(CompilerOptions.OPTION_TargetPlatform,
//          CompilerOptions.VERSION_1_2);
//      optionsMap.put(CompilerOptions.OPTION_TaskTags, Util.EMPTY_STRING);
//      optionsMap.put(CompilerOptions.OPTION_TaskPriorities, Util.EMPTY_STRING);
//      optionsMap.put(CompilerOptions.OPTION_TaskCaseSensitive,
//          CompilerOptions.DISABLED);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportUnusedParameterWhenImplementingAbstract,
//          CompilerOptions.DISABLED);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportUnusedParameterWhenOverridingConcrete,
//          CompilerOptions.DISABLED);
//      optionsMap.put(CompilerOptions.OPTION_ReportSpecialParameterHidingField,
//          CompilerOptions.DISABLED);
//      optionsMap.put(
//          CompilerOptions.OPTION_ReportUnavoidableGenericTypeProblems,
//          CompilerOptions.ENABLED);
//      optionsMap.put(CompilerOptions.OPTION_MaxProblemPerUnit,
//          String.valueOf(100));
//      optionsMap.put(CompilerOptions.OPTION_InlineJsr, CompilerOptions.DISABLED);
//      optionsMap.put(CompilerOptions.OPTION_ReportMethodCanBeStatic,
//          CompilerOptions.IGNORE);
//      optionsMap.put(CompilerOptions.OPTION_ReportMethodCanBePotentiallyStatic,
//          CompilerOptions.IGNORE);
      this.defaultCompilerOptions = optionsMap;
    }
//    Object sourceOption = this.options.get(CompilerOptions.OPTION_Source);
//    if (sourceOption != null) {
//      this.defaultCompilerOptions.put(CompilerOptions.OPTION_Source,
//          sourceOption);
//    } else {
//      this.defaultCompilerOptions.put(CompilerOptions.OPTION_Source,
//          CompilerOptions.VERSION_1_3);
//    }
    return this.defaultCompilerOptions;
  }

  private TextEdit internalFormatClassBodyDeclarations(String source, int indentationLevel,
      String lineSeparator, DartNode[] bodyDeclarations, IRegion[] regions, boolean includeComments) {
    if (lineSeparator != null) {
      this.preferences.line_separator = lineSeparator;
    } else {
      this.preferences.line_separator = LINE_SEPARATOR;
    }
    this.preferences.initial_indentation_level = indentationLevel;

    this.newCodeFormatter = new CodeFormatterVisitor(
        this.preferences,
        regions,
        this.codeSnippetParsingUtil,
        includeComments);
    return this.newCodeFormatter.format(source, bodyDeclarations);
  }

  private TextEdit internalFormatExpression(String source, int indentationLevel,
      String lineSeparator, DartExpression expression, IRegion[] regions, boolean includeComments) {
    if (lineSeparator != null) {
      this.preferences.line_separator = lineSeparator;
    } else {
      this.preferences.line_separator = LINE_SEPARATOR;
    }
    this.preferences.initial_indentation_level = indentationLevel;

    this.newCodeFormatter = new CodeFormatterVisitor(
        this.preferences,
        regions,
        this.codeSnippetParsingUtil,
        includeComments);

    TextEdit textEdit = this.newCodeFormatter.format(source, expression);
    return textEdit;
  }

  private TextEdit internalFormatStatements(String source, int indentationLevel,
      String lineSeparator, DartMethodDefinition constructorDeclaration, IRegion[] regions,
      boolean includeComments) {
    if (lineSeparator != null) {
      this.preferences.line_separator = lineSeparator;
    } else {
      this.preferences.line_separator = LINE_SEPARATOR;
    }
    this.preferences.initial_indentation_level = indentationLevel;

    this.newCodeFormatter = new CodeFormatterVisitor(
        this.preferences,
        regions,
        this.codeSnippetParsingUtil,
        includeComments);

    return this.newCodeFormatter.format(source, constructorDeclaration);
  }

  /**
   * Deprecated as using old option constant
   * 
   * @deprecated
   */
  @Deprecated
  private Object oldCommentFormatOption() {
    return null;
  }

  private TextEdit probeFormatting(String source, int indentationLevel, String lineSeparator,
      IRegion[] regions, boolean includeComments) {
    if (PROBING_SCANNER == null) {
      // scanner use to check if the kind could be K_JAVA_DOC,
      // K_MULTI_LINE_COMMENT or K_SINGLE_LINE_COMMENT
      // do not tokenize white spaces to get single comments even with spaces
      // before...
      PROBING_SCANNER = new Scanner();
    }
    PROBING_SCANNER.setSource(source.toCharArray());

    IRegion coveredRegion = getCoveredRegion(regions);
    int offset = coveredRegion.getOffset();
    int length = coveredRegion.getLength();

    PROBING_SCANNER.resetTo(offset, offset + length - 1);
    try {
      if (PROBING_SCANNER.peekNextToken() == Token.WHITESPACE) {
        PROBING_SCANNER.getNextToken();
      }
      int kind = -1;
      if (PROBING_SCANNER.getNextToken() == Token.COMMENT) {
        switch (PROBING_SCANNER.getCommentStyle()) {
          case BLOCK:
            if (PROBING_SCANNER.getNextToken() == Token.EOS) {
              kind = K_MULTI_LINE_COMMENT;
            }
            break;
          case END_OF_LINE:
            if (PROBING_SCANNER.getNextToken() == Token.EOS) {
              kind = K_SINGLE_LINE_COMMENT;
            }
            break;
          case DART_DOC:
            if (PROBING_SCANNER.getNextToken() == Token.EOS) {
              kind = K_JAVA_DOC;
            }
            break;
        }
      }
      if (kind != -1) {
        return formatComment(kind, source, indentationLevel, lineSeparator, regions);
      }
    } catch (InvalidInputException e) {
      // ignore
    }
    PROBING_SCANNER.setSource((char[]) null);

    // probe for expression
    DartExpression expression = this.codeSnippetParsingUtil.parseExpression(
        source.toCharArray(),
        getDefaultCompilerOptions(),
        true);
    if (expression != null) {
      return internalFormatExpression(
          source,
          indentationLevel,
          lineSeparator,
          expression,
          regions,
          includeComments);
    }

    // probe for body declarations (fields, methods, constructors)
    DartNode[] bodyDeclarations = this.codeSnippetParsingUtil.parseClassBodyDeclarations(
        source.toCharArray(),
        getDefaultCompilerOptions(),
        true);
    if (bodyDeclarations != null) {
      return internalFormatClassBodyDeclarations(
          source,
          indentationLevel,
          lineSeparator,
          bodyDeclarations,
          regions,
          includeComments);
    }

    // probe for statements
    DartMethodDefinition constructorDeclaration = this.codeSnippetParsingUtil.parseStatements(
        source.toCharArray(),
        getDefaultCompilerOptions(),
        true,
        false);
    try {
      if (constructorDeclaration.getFunction().getBody().getStatements() != null) {
        return internalFormatStatements(
            source,
            indentationLevel,
            lineSeparator,
            constructorDeclaration,
            regions,
            includeComments);
      }
    } catch (NullPointerException ex) {
      // fall through
    }

    // this has to be a compilation unit
    return formatCompilationUnit(source, indentationLevel, lineSeparator, regions, includeComments);
  }

  /**
   * True if 1. All regions are within maxLength 2. regions are sorted 3. regions are not
   * overlapping
   */
  private boolean regionsSatisfiesPreconditions(IRegion[] regions, int maxLength) {
    int regionsLength = regions == null ? 0 : regions.length;
    if (regionsLength == 0) {
      return false;
    }

    IRegion first = regions[0];
    if (first.getOffset() < 0 || first.getLength() < 0
        || first.getOffset() + first.getLength() > maxLength) {
      return false;
    }

    int lastOffset = first.getOffset() + first.getLength() - 1;
    for (int i = 1; i < regionsLength; i++) {
      IRegion current = regions[i];
      if (lastOffset > current.getOffset()) {
        return false;
      }

      if (current.getOffset() < 0 || current.getLength() < 0
          || current.getOffset() + current.getLength() > maxLength) {
        return false;
      }

      lastOffset = current.getOffset() + current.getLength() - 1;
    }

    return true;
  }
}
