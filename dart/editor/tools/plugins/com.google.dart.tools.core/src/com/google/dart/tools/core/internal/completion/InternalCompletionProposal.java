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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.completion.CompletionRequestor;
import com.google.dart.tools.core.internal.search.scope.WorkspaceSearchScope;
import com.google.dart.tools.core.internal.util.CharOperation;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModifiers;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchException;
import com.google.dart.tools.core.search.SearchFilter;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.search.SearchPatternFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.util.List;

/**
 * Instances of the class <code>InternalCompletionProposal</code> implement a completion proposal.
 */
public class InternalCompletionProposal extends CompletionProposal {
  protected CompletionEngine completionEngine;
  protected char[] declarationTypeName;
  protected char[] typeName;
  protected char[][] parameterTypeNames;
  protected char[] originalSignature;
  private boolean updateCompletion = false;
  protected boolean isConstructor = false;
  protected boolean isInterface = false;

  /**
   * Kind of completion request.
   */
  private int completionKind;

  /**
   * Offset in original buffer where ICodeAssist.codeComplete() was requested.
   */
  private int completionLocation;

  /**
   * Start position (inclusive) of source range in original buffer containing the relevant token
   * defaults to empty subrange at [0,0).
   */
  private int tokenStart = 0;

  /**
   * End position (exclusive) of source range in original buffer containing the relevant token;
   * defaults to empty subrange at [0,0).
   */
  private int tokenEnd = 0;

  /**
   * Completion string; defaults to empty string.
   */
  private char[] completion = CharOperation.NO_CHAR;

  /**
   * Start position (inclusive) of source range in original buffer to be replaced by completion
   * string; defaults to empty subrange at [0,0).
   */
  private int replaceStart = 0;

  /**
   * End position (exclusive) of source range in original buffer to be replaced by completion
   * string; defaults to empty subrange at [0,0).
   */
  private int replaceEnd = 0;

  /**
   * Relevance rating; positive; higher means better; defaults to minimum rating.
   */
  private int relevance = 1;

  /**
   * Signature of the relevant package or type declaration in the context, or <code>null</code> if
   * none. Defaults to null.
   */
  private char[] declarationSignature = null;

  /**
   * Simple name of the method, field, member, or variable relevant in the context, or
   * <code>null</code> if none. Defaults to null.
   */
  private char[] name = null;

  /**
   * Signature of the method, field type, member type, relevant in the context, or <code>null</code>
   * if none. Defaults to null.
   */
  private char[] signature = null;

  /**
   * Array of required completion proposals, or <code>null</code> if none. The proposal can not be
   * applied if the required proposals aren't applied. Defaults to <code>null</code>.
   */
  private CompletionProposal[] requiredProposals;

  /**
   * Parameter names (for method completions), or <code>null</code> if none. Lazily computed.
   * Defaults to <code>null</code>.
   */
  private char[][] parameterNames = null;

  /**
   * Indicates whether parameter names have been computed.
   */
  private boolean parameterNamesComputed = false;

  /**
   * Modifiers declared on a method or field. NB: Replaces flags.
   */
  private DartModifiers modifiers = null;

  private int receiverStart;

  private int receiverEnd;

  private char[] receiverSignature;
  private boolean isSetter;
  private boolean isGetter;

  /**
   * Creates a basic completion proposal. All instance field have plausible default values unless
   * otherwise noted.
   * <p>
   * Note that the constructors for this class are internal to the Dart model implementation.
   * Clients cannot directly create CompletionProposal objects.
   * </p>
   * 
   * @param kind one of the kind constants declared on this class
   * @param completionLocation original offset of code completion request
   */
  public InternalCompletionProposal(int kind, int completionLocation) {
    if ((kind < FIRST_KIND) || (kind > LAST_KIND)) {
      throw new IllegalArgumentException();
    }
    if (this.completion == null || completionLocation < 0) {
      // completionLocation can be -1 if the completion occur at the start of a
      // file or the start of a code snippet but this API isn't design to
      // support negative position.
      if (this.completion == null || completionLocation != -1) {
        throw new IllegalArgumentException();
      }
      completionLocation = 0;
    }
    this.completionKind = kind;
    this.completionLocation = completionLocation;
  }

  /**
   * Finds the method parameter names. This information is relevant to method reference (and method
   * declaration proposals). Returns <code>null</code> if not available or not relevant.
   * <p>
   * The client must not modify the array returned.
   * </p>
   * <p>
   * <b>Note that this is an expensive thing to compute, which may require parsing Dart source
   * files, etc. Use sparingly.</b>
   * </p>
   * 
   * @param monitor the progress monitor, or <code>null</code> if none
   * @return the parameter names, or <code>null</code> if none or not available or not relevant
   */
  @SuppressWarnings("deprecation")
  @Override
  public char[][] findParameterNames(IProgressMonitor monitor) {
    if (!this.parameterNamesComputed) {
      this.parameterNamesComputed = true;

      switch (this.completionKind) {
        case ANONYMOUS_CLASS_DECLARATION:
          try {
            this.parameterNames = findMethodParameterNames(
                this.declarationTypeName,
                CharOperation.lastSegment(this.declarationTypeName, '.'),
                getParameterTypes(this.originalSignature == null ? this.signature
                    : this.originalSignature));
          } catch (IllegalArgumentException e) {
            // protection for invalid signature
            if (this.parameterTypeNames != null) {
              this.parameterNames = CompletionEngine.createDefaultParameterNames(this.parameterTypeNames.length);
            } else {
              this.parameterNames = null;
            }
          }
          break;
        case ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION:
          try {
            this.parameterNames = findConstructorParameterNames(
                this.declarationTypeName,
                CharOperation.lastSegment(this.declarationTypeName, '.'),
                getParameterTypes(this.originalSignature == null ? this.signature
                    : this.originalSignature));
          } catch (IllegalArgumentException e) {
            // protection for invalid signature
            if (this.parameterTypeNames != null) {
              this.parameterNames = CompletionEngine.createDefaultParameterNames(this.parameterTypeNames.length);
            } else {
              this.parameterNames = null;
            }
          }
          break;
        case METHOD_REF:
        case METHOD_REF_WITH_CASTED_RECEIVER:
          try {
            this.parameterNames = findMethodParameterNames(
                this.declarationTypeName,
                this.name,
                getParameterTypes(this.originalSignature == null ? this.signature
                    : this.originalSignature));
          } catch (IllegalArgumentException e) {
            // protection for invalid signature
            if (this.parameterTypeNames != null) {
              this.parameterNames = CompletionEngine.createDefaultParameterNames(this.parameterTypeNames.length);
            } else {
              this.parameterNames = null;
            }
          }
          break;
        case CONSTRUCTOR_INVOCATION:
          try {
            this.parameterNames = findConstructorParameterNames(
                this.declarationTypeName,
                this.name,
                getParameterTypes(this.originalSignature == null ? this.signature
                    : this.originalSignature));
          } catch (IllegalArgumentException e) {
            // protection for invalid signature
            if (this.parameterTypeNames != null) {
              this.parameterNames = CompletionEngine.createDefaultParameterNames(this.parameterTypeNames.length);
            } else {
              this.parameterNames = null;
            }
          }
          break;
        case METHOD_DECLARATION:
          try {
            this.parameterNames = findMethodParameterNames(
                this.declarationTypeName,
                this.name,
                getParameterTypes(this.originalSignature == null ? this.signature
                    : this.originalSignature));
          } catch (IllegalArgumentException e) {
            // protection for invalid signature
            if (this.parameterTypeNames != null) {
              this.parameterNames = CompletionEngine.createDefaultParameterNames(this.parameterTypeNames.length);
            } else {
              this.parameterNames = null;
            }
          }
          if (this.parameterNames != null) {
            this.updateCompletion = true;
          }
          break;
      }
    }
    return this.parameterNames;
  }

  /**
   * Returns the proposed sequence of characters to insert into the source file buffer, replacing
   * the characters at the specified source range. The string can be arbitrary; for example, it
   * might include not only the name of a method but a set of parentheses.
   * <p>
   * The client must not modify the array returned.
   * </p>
   * 
   * @return the completion string
   */
  @Override
  public char[] getCompletion() {
    if (this.completionKind == METHOD_DECLARATION) {
      findParameterNames(null);
      if (this.updateCompletion) {
        this.updateCompletion = false;

        if (this.parameterNames != null) {
          int length = this.parameterNames.length;
          StringBuffer completionBuffer = new StringBuffer(this.completion.length);

          int start = 0;
          int end = CharOperation.indexOf('%', this.completion);

          completionBuffer.append(this.completion, start, end - start);

          for (int i = 0; i < length; i++) {
            completionBuffer.append(this.parameterNames[i]);
            start = end + 1;
            end = CharOperation.indexOf('%', this.completion, start);
            if (end > -1) {
              completionBuffer.append(this.completion, start, end - start);
            } else {
              completionBuffer.append(this.completion, start, this.completion.length - start);
            }
          }
          int nameLength = completionBuffer.length();
          this.completion = new char[nameLength];
          completionBuffer.getChars(0, nameLength, this.completion, 0);
        }
      }
    }
    return this.completion;
  }

  /**
   * Returns the character index in the source file buffer where source completion was requested
   * (the <code>offset</code> parameter to <code>ICodeAssist.codeComplete</code> minus one).
   * 
   * @return character index in source file buffer
   * @see ICodeAssist#codeComplete(int,CompletionRequestor)
   */
  @Override
  public int getCompletionLocation() {
    return this.completionLocation;
  }

  /**
   * Returns the type signature or package name of the relevant declaration in the context, or
   * <code>null</code> if none.
   * <p>
   * This field is available for the following kinds of completion proposals:
   * <ul>
   * <li><code>ANNOTATION_ATTRIBUT_REF</code> - type signature of the annotation that declares the
   * attribute that is referenced</li>
   * <li><code>ANONYMOUS_CLASS_DECLARATION</code> - type signature of the type that is being
   * subclassed or implemented</li>
   * <li><code>FIELD_IMPORT</code> - type signature of the type that declares the field that is
   * imported</li>
   * <li><code>FIELD_REF</code> - type signature of the type that declares the field that is
   * referenced</li>
   * <li><code>FIELD_REF_WITH_CASTED_RECEIVER</code> - type signature of the type that declares the
   * field that is referenced</li>
   * <li><code>METHOD_IMPORT</code> - type signature of the type that declares the method that is
   * imported</li>
   * <li><code>METHOD_REF</code> - type signature of the type that declares the method that is
   * referenced</li>
   * <li><code>METHOD_REF_WITH_CASTED_RECEIVER</code> - type signature of the type that declares the
   * method that is referenced</li>
   * <li><code>METHOD_DECLARATION</code> - type signature of the type that declares the method that
   * is being implemented or overridden</li>
   * <li><code>PACKAGE_REF</code> - dot-based package name of the package that is referenced</li>
   * <li><code>TYPE_IMPORT</code> - dot-based package name of the package containing the type that
   * is imported</li>
   * <li><code>TYPE_REF</code> - dot-based package name of the package containing the type that is
   * referenced</li>
   * <li><code>POTENTIAL_METHOD_DECLARATION</code> - type signature of the type that declares the
   * method that is being created</li>
   * </ul>
   * For kinds of completion proposals, this method returns <code>null</code>. Clients must not
   * modify the array returned.
   * </p>
   * 
   * @return a type signature or a package name (depending on the kind of completion), or
   *         <code>null</code> if none
   */
  @Override
  public char[] getDeclarationSignature() {
    return this.declarationSignature;
  }

  /**
   * Returns the kind of completion being proposed.
   * <p>
   * The set of different kinds of completion proposals is expected to change over time. It is
   * strongly recommended that clients do <b>not</b> assume that the kind is one of the ones they
   * know about, and code defensively for the possibility of unexpected future growth.
   * </p>
   * 
   * @return the kind; one of the kind constants declared on this class, or possibly a kind unknown
   *         to the caller
   */
  @Override
  public int getKind() {
    return this.completionKind;
  }

  /**
   * Returns the modifier flags relevant in the context, or <code>Flags.AccDefault</code> if none.
   * <p>
   * This field is available for the following kinds of completion proposals:
   * <ul>
   * <li><code>ANNOTATION_ATTRIBUT_REF</code> - modifier flags of the attribute that is referenced;
   * <li><code>ANONYMOUS_CLASS_DECLARATION</code> - modifier flags of the constructor that is
   * referenced</li>
   * <li><code>FIELD_IMPORT</code> - modifier flags of the field that is imported.</li>
   * <li><code>FIELD_REF</code> - modifier flags of the field that is referenced;
   * <code>Flags.AccEnum</code> can be used to recognize references to enum constants</li>
   * <li><code>FIELD_REF_WITH_CASTED_RECEIVER</code> - modifier flags of the field that is
   * referenced.</li>
   * <li><code>KEYWORD</code> - modifier flag corresponding to the modifier keyword</li>
   * <li><code>LOCAL_VARIABLE_REF</code> - modifier flags of the local variable that is referenced</li>
   * <li><code>METHOD_IMPORT</code> - modifier flags of the method that is imported;</li>
   * <li><code>METHOD_REF</code> - modifier flags of the method that is referenced;
   * <code>Flags.AccAnnotation</code> can be used to recognize references to annotation type members
   * </li>
   * <li><code>METHOD_REF_WITH_CASTED_RECEIVER</code> - modifier flags of the method that is
   * referenced.</li>
   * <li><code>METHOD_DECLARATION</code> - modifier flags for the method that is being implemented
   * or overridden</li>
   * <li><code>TYPE_IMPORT</code> - modifier flags of the type that is imported;
   * <code>Flags.AccInterface</code> can be used to recognize references to interfaces,
   * <code>Flags.AccEnum</code> enum types, and <code>Flags.AccAnnotation</code> annotation types</li>
   * <li><code>TYPE_REF</code> - modifier flags of the type that is referenced;
   * <code>Flags.AccInterface</code> can be used to recognize references to interfaces,
   * <code>Flags.AccEnum</code> enum types, and <code>Flags.AccAnnotation</code> annotation types</li>
   * <li><code>VARIABLE_DECLARATION</code> - modifier flags for the variable being declared</li>
   * <li><code>POTENTIAL_METHOD_DECLARATION</code> - modifier flags for the method that is being
   * created</li>
   * </ul>
   * For other kinds of completion proposals, this method returns <code>Flags.AccDefault</code>.
   * </p>
   * 
   * @return the modifier flags, or <code>null</code> if none
   */
  public DartModifiers getModifiers() {
    return this.modifiers;
  }

  /**
   * Returns the simple name of the method, field, member, or variable relevant in the context, or
   * <code>null</code> if none.
   * <p>
   * This field is available for the following kinds of completion proposals:
   * <ul>
   * <li><code>ANNOTATION_ATTRIBUT_REF</code> - the name of the attribute</li>
   * <li><code>FIELD_IMPORT</code> - the name of the field</li>
   * <li><code>FIELD_REF</code> - the name of the field</li>
   * <li><code>FIELD_REF_WITH_CASTED_RECEIVER</code> - the name of the field</li>
   * <li><code>KEYWORD</code> - the keyword</li>
   * <li><code>LABEL_REF</code> - the name of the label</li>
   * <li><code>LOCAL_VARIABLE_REF</code> - the name of the local variable</li>
   * <li><code>METHOD_IMPORT</code> - the name of the method</li>
   * <li><code>METHOD_REF</code> - the name of the method (the type simple name for constructor)</li>
   * <li><code>METHOD_REF_WITH_CASTED_RECEIVER</code> - the name of the method</li>
   * <li><code>METHOD_DECLARATION</code> - the name of the method (the type simple name for
   * constructor)</li>
   * <li><code>VARIABLE_DECLARATION</code> - the name of the variable</li>
   * <li><code>POTENTIAL_METHOD_DECLARATION</code> - the name of the method</li>
   * </ul>
   * For kinds of completion proposals, this method returns <code>null</code>. Clients must not
   * modify the array returned.
   * </p>
   * 
   * @return the keyword, field, method, local variable, or member name, or <code>null</code> if
   *         none
   */
  @Override
  public char[] getName() {
    return this.name;
  }

  @Override
  public char[][] getParameterNames() {
    return this.parameterNames;
  }

  @Override
  public char[][] getParameterTypeNames() {
    return this.parameterTypeNames;
  }

  /**
   * Returns the character index of the end (exclusive) of the subrange in the source file buffer
   * containing the relevant receiver of the member being completed. *
   * <p>
   * This field is available for the following kinds of completion proposals:
   * <ul>
   * <li><code>FIELD_REF_WITH_CASTED_RECEIVER</code></li>
   * <li><code>METHOD_REF_WITH_CASTED_RECEIVER</code></li>
   * </ul>
   * For kinds of completion proposals, this method returns <code>0</code>.
   * </p>
   * 
   * @return character index of receiver end position (exclusive)
   * @since 3.4
   */
  @Override
  public int getReceiverEnd() {
    return this.receiverEnd;
  }

  /**
   * Returns the type signature or package name of the relevant receiver in the context, or
   * <code>null</code> if none.
   * <p>
   * This field is available for the following kinds of completion proposals:
   * <ul>
   * <li><code>FIELD_REF_WITH_CASTED_RECEIVER</code> - type signature of the type that cast the
   * receiver of the field that is referenced</li>
   * <li><code>METHOD_REF_WITH_CASTED_RECEIVER</code> - type signature of the type that cast the
   * receiver of the method that is referenced</li>
   * </ul>
   * For kinds of completion proposals, this method returns <code>null</code>. Clients must not
   * modify the array returned.
   * </p>
   * 
   * @return a type signature or a package name (depending on the kind of completion), or
   *         <code>null</code> if none
   * @since 3.4
   */
  @Override
  public char[] getReceiverSignature() {
    return this.receiverSignature;
  }

  /**
   * Returns the character index of the start of the subrange in the source file buffer containing
   * the relevant receiver of the member being completed. This receiver is an expression.
   * <p>
   * This field is available for the following kinds of completion proposals:
   * <ul>
   * <li><code>FIELD_REF_WITH_CASTED_RECEIVER</code></li>
   * <li><code>METHOD_REF_WITH_CASTED_RECEIVER</code></li>
   * </ul>
   * For kinds of completion proposals, this method returns <code>0</code>.
   * </p>
   * 
   * @return character index of receiver start position (inclusive)
   * @since 3.4
   */
  @Override
  public int getReceiverStart() {
    return this.receiverStart;
  }

  /**
   * Returns the relative relevance rating of this proposal.
   * 
   * @return relevance rating of this proposal; ratings are positive; higher means better
   */
  @Override
  public int getRelevance() {
    return this.relevance;
  }

  /**
   * Returns the character index of the end of the subrange in the source file buffer to be replaced
   * by the completion string. If the subrange is empty (
   * <code>getReplaceEnd() == getReplaceStart()</code>), the completion string is to be inserted at
   * this index.
   * 
   * @return replacement end position (exclusive)
   */
  @Override
  public int getReplaceEnd() {
    return this.replaceEnd;
  }

  /**
   * Returns the character index of the start of the subrange in the source file buffer to be
   * replaced by the completion string. If the subrange is empty (
   * <code>getReplaceEnd() == getReplaceStart()</code>), the completion string is to be inserted at
   * this index.
   * <p>
   * Note that while the token subrange is precisely specified, the replacement range is loosely
   * constrained and may not bear any direct relation to the original request offset. For example,
   * it would be possible for a type completion to propose inserting an import declaration at the
   * top of the compilation unit; or the completion might include trailing parentheses and
   * punctuation for a method completion.
   * </p>
   * 
   * @return replacement start position (inclusive)
   */
  @Override
  public int getReplaceStart() {
    return this.replaceStart;
  }

  /**
   * Returns the required completion proposals. The proposal can be apply only if these required
   * completion proposals are also applied. If the required proposal aren't applied the completion
   * could create completion problems.
   * <p>
   * This field is available for the following kinds of completion proposals:
   * <ul>
   * <li><code>FIELD_REF</code> - The allowed required proposals for this kind are:
   * <ul>
   * <li><code>TYPE_REF</code></li>
   * <li><code>TYPE_IMPORT</code></li>
   * <li><code>FIELD_IMPORT</code></li>
   * </ul>
   * </li>
   * <li><code>METHOD_REF</code> - The allowed required proposals for this kind are:
   * <ul>
   * <li><code>TYPE_REF</code></li>
   * <li><code>TYPE_IMPORT</code></li>
   * <li><code>METHOD_IMPORT</code></li>
   * </ul>
   * </li> </li>
   * <li><code>TYPE_REF</code> - The allowed required proposals for this kind are:
   * <ul>
   * <li><code>TYPE_REF</code></li>
   * </ul>
   * </li>
   * </ul>
   * </p>
   * <p>
   * Other kinds of required proposals will be returned in the future, therefore clients of this API
   * must allow with {@link CompletionRequestor#setAllowsRequiredProposals(int, int, boolean)} only
   * kinds which are in this list to avoid unexpected results in the future.
   * </p>
   * <p>
   * A required proposal of a given kind is proposed even if
   * {@link CompletionRequestor#isIgnored(int)} return <code>true</code> for that kind.
   * </p>
   * <p>
   * A required completion proposal cannot have required completion proposals.
   * </p>
   * 
   * @return the required completion proposals, or <code>null</code> if none.
   * @see CompletionRequestor#setAllowsRequiredProposals(int, int,boolean)
   * @since 3.3
   */
  @Override
  public CompletionProposal[] getRequiredProposals() {
    return this.requiredProposals;
  }

  @Override
  public char[] getReturnTypeName() {
    return getTypeName();
  }

  /**
   * Returns the signature of the method or type relevant in the context, or <code>null</code> if
   * none.
   * <p>
   * This field is available for the following kinds of completion proposals:
   * <ul>
   * <li><code>ANNOTATION_ATTRIBUT_REF</code> - the type signature of the referenced attribute's
   * type</li>
   * <li><code>ANONYMOUS_CLASS_DECLARATION</code> - method signature of the constructor that is
   * being invoked</li>
   * <li><code>FIELD_IMPORT</code> - the type signature of the referenced field's type</li>
   * <li><code>FIELD_REF</code> - the type signature of the referenced field's type</li>
   * <li><code>FIELD_REF_WITH_CASTED_RECEIVER</code> - the type signature of the referenced field's
   * type</li>
   * <li><code>LOCAL_VARIABLE_REF</code> - the type signature of the referenced local variable's
   * type</li>
   * <li><code>METHOD_IMPORT</code> - method signature of the method that is imported</li>
   * <li><code>METHOD_REF</code> - method signature of the method that is referenced</li>
   * <li><code>METHOD_REF_WITH_CASTED_RECEIVER</code> - method signature of the method that is
   * referenced</li>
   * <li><code>METHOD_DECLARATION</code> - method signature of the method that is being implemented
   * or overridden</li>
   * <li><code>TYPE_IMPORT</code> - type signature of the type that is imported</li>
   * <li><code>TYPE_REF</code> - type signature of the type that is referenced</li>
   * <li><code>VARIABLE_DECLARATION</code> - the type signature of the type of the variable being
   * declared</li>
   * <li><code>POTENTIAL_METHOD_DECLARATION</code> - method signature of the method that is being
   * created</li>
   * </ul>
   * For kinds of completion proposals, this method returns <code>null</code>. Clients must not
   * modify the array returned.
   * </p>
   * 
   * @return the signature, or <code>null</code> if none
   */
  @Override
  public char[] getSignature() {
    return this.signature;
  }

  /**
   * Returns the character index of the end (exclusive) of the subrange in the source file buffer
   * containing the relevant token. When there is no relevant token, the range is empty (
   * <code>getEndToken() == getStartToken()</code>).
   * 
   * @return character index of token end position (exclusive)
   */
  @Override
  public int getTokenEnd() {
    return this.tokenEnd;
  }

  /**
   * Returns the character index of the start of the subrange in the source file buffer containing
   * the relevant token being completed. This token is either the identifier or Dart language
   * keyword under, or immediately preceding, the original request offset. If the original request
   * offset is not within or immediately after an identifier or keyword, then the position returned
   * is original request offset and the token range is empty.
   * 
   * @return character index of token start position (inclusive)
   */
  @Override
  public int getTokenStart() {
    return this.tokenStart;
  }

  /**
   * Returns whether this proposal is a constructor.
   * <p>
   * This field is available for the following kinds of completion proposals:
   * <ul>
   * <li><code>METHOD_REF</code> - return <code>true</code> if the referenced method is a
   * constructor</li>
   * <li><code>METHOD_DECLARATION</code> - return <code>true</code> if the declared method is a
   * constructor</li>
   * </ul>
   * For kinds of completion proposals, this method returns <code>false</code>.
   * </p>
   * 
   * @return <code>true</code> if the proposal is a constructor.
   * @since 3.1
   */
  @Override
  public boolean isConstructor() {
    return this.isConstructor;
  }

  @Override
  public boolean isGetOrSet() {
    return isGetter || isSetter;
  }

  @Override
  public boolean isInterface() {
    return isInterface;
  }

  @Override
  public boolean isPrivate() {
    char[] n = getName();
    if (n == null || n.length == 0) {
      return false;
    }
    return n[0] == '_';
  }

  /**
   * Sets the proposed sequence of characters to insert into the source file buffer, replacing the
   * characters at the specified source range. The string can be arbitrary; for example, it might
   * include not only the name of a method but a set of parentheses.
   * <p>
   * If not set, defaults to an empty character array.
   * </p>
   * <p>
   * The completion engine creates instances of this class and sets its properties; this method is
   * not intended to be used by other clients.
   * </p>
   * 
   * @param completion the completion string
   */
  @Override
  public void setCompletion(char[] completion) {
    this.completion = completion;
  }

  /**
   * Sets the type or package signature of the relevant declaration in the context, or
   * <code>null</code> if none.
   * <p>
   * If not set, defaults to none.
   * </p>
   * <p>
   * The completion engine creates instances of this class and sets its properties; this method is
   * not intended to be used by other clients.
   * </p>
   * 
   * @param signature the type or package signature, or <code>null</code> if none
   */
  @Override
  public void setDeclarationSignature(char[] signature) {
    this.declarationSignature = signature;
  }

  /**
   * Sets the modifier flags relevant in the context.
   * <p>
   * If not set, defaults to none.
   * <p>
   * The completion engine creates instances of this class and sets its properties; this method is
   * not intended to be used by other clients.
   * 
   * @param modifiers the modifier flags, or <code>null</code> if none
   */
  @Override
  public void setModifiers(DartModifiers modifiers) {
    this.modifiers = modifiers;
  }

  /**
   * Sets the simple name of the method (type simple name for constructor), field, member, or
   * variable relevant in the context, or <code>null</code> if none.
   * <p>
   * If not set, defaults to none.
   * </p>
   * <p>
   * The completion engine creates instances of this class and sets its properties; this method is
   * not intended to be used by other clients.
   * </p>
   * 
   * @param name the keyword, field, method, local variable, or member name, or <code>null</code> if
   *          none
   */
  @Override
  public void setName(char[] name) {
    this.name = name;
  }

  public void setOriginalSignature(char[] originalSignature) {
    this.originalSignature = originalSignature;
  }

  /**
   * Sets the method parameter names. This information is relevant to method reference (and method
   * declaration proposals).
   * <p>
   * The completion engine creates instances of this class and sets its properties; this method is
   * not intended to be used by other clients.
   * </p>
   * 
   * @param parameterNames the parameter names, or <code>null</code> if none
   */
  @Override
  public void setParameterNames(char[][] parameterNames) {
    this.parameterNames = parameterNames;
    this.parameterNamesComputed = true;
  }

  /**
   * Sets the character indices of the subrange in the source file buffer containing the relevant
   * receiver of the member being completed.
   * <p>
   * If not set, defaults to empty subrange at [0,0).
   * </p>
   * 
   * @param startIndex character index of receiver start position (inclusive)
   * @param endIndex character index of receiver end position (exclusive)
   * @since 3.4
   */
  @Override
  public void setReceiverRange(int startIndex, int endIndex) {
    this.receiverStart = startIndex;
    this.receiverEnd = endIndex;
  }

  /**
   * Sets the type or package signature of the relevant receiver in the context, or
   * <code>null</code> if none.
   * <p>
   * If not set, defaults to none.
   * </p>
   * <p>
   * The completion engine creates instances of this class and sets its properties; this method is
   * not intended to be used by other clients.
   * </p>
   * 
   * @param signature the type or package signature, or <code>null</code> if none
   * @since 3.4
   */
  @Override
  public void setReceiverSignature(char[] signature) {
    this.receiverSignature = signature;
  }

  /**
   * Sets the relative relevance rating of this proposal.
   * <p>
   * If not set, defaults to the lowest possible rating (1).
   * </p>
   * <p>
   * The completion engine creates instances of this class and sets its properties; this method is
   * not intended to be used by other clients.
   * </p>
   * 
   * @param rating relevance rating of this proposal; ratings are positive; higher means better
   */
  @Override
  public void setRelevance(int rating) {
    if (rating <= 0) {
      throw new IllegalArgumentException();
    }
    this.relevance = rating;
    if (name != null && name.length > 4 && name[0] == '$' && name[1] == 'd' && name[2] == 'o'
        && name[3] == 'm') {
      this.relevance = 0;
    }
  }

  /**
   * Sets the character indices of the subrange in the source file buffer to be replaced by the
   * completion string. If the subrange is empty ( <code>startIndex == endIndex</code>), the
   * completion string is to be inserted at this index.
   * <p>
   * If not set, defaults to empty subrange at [0,0).
   * </p>
   * <p>
   * The completion engine creates instances of this class and sets its properties; this method is
   * not intended to be used by other clients.
   * </p>
   * 
   * @param startIndex character index of replacement start position (inclusive)
   * @param endIndex character index of replacement end position (exclusive)
   */
  @Override
  public void setReplaceRange(int startIndex, int endIndex) {
    if (startIndex < 0 || endIndex < startIndex) {
      throw new IllegalArgumentException();
    }
    this.replaceStart = startIndex;
    this.replaceEnd = endIndex;
  }

  /**
   * Sets the list of required completion proposals, or <code>null</code> if none.
   * <p>
   * If not set, defaults to none.
   * </p>
   * <p>
   * The completion engine creates instances of this class and sets its properties; this method is
   * not intended to be used by other clients.
   * </p>
   * 
   * @param proposals the list of required completion proposals, or <code>null</code> if none
   * @since 3.3
   */
  @Override
  public void setRequiredProposals(CompletionProposal[] proposals) {
    this.requiredProposals = proposals;
  }

  /**
   * Sets the signature of the method, field type, member type, relevant in the context, or
   * <code>null</code> if none.
   * <p>
   * If not set, defaults to none.
   * </p>
   * <p>
   * The completion engine creates instances of this class and sets its properties; this method is
   * not intended to be used by other clients.
   * </p>
   * 
   * @param signature the signature, or <code>null</code> if none
   */
  @Override
  public void setSignature(char[] signature) {
    this.signature = signature;
  }

  /**
   * Sets the character indices of the subrange in the source file buffer containing the relevant
   * token being completed. This token is either the identifier or Dart language keyword under, or
   * immediately preceding, the original request offset. If the original request offset is not
   * within or immediately after an identifier or keyword, then the source range begins at original
   * request offset and is empty.
   * <p>
   * If not set, defaults to empty subrange at [0,0).
   * </p>
   * 
   * @param startIndex character index of token start position (inclusive)
   * @param endIndex character index of token end position (exclusive)
   */
  @Override
  public void setTokenRange(int startIndex, int endIndex) {
    if (startIndex < 0 || endIndex < startIndex) {
      throw new IllegalArgumentException();
    }
    this.tokenStart = startIndex;
    this.tokenEnd = endIndex;
  }

  @SuppressWarnings("deprecation")
  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append('[');
    switch (this.completionKind) {
      case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
        buffer.append("ANONYMOUS_CLASS_DECLARATION"); //$NON-NLS-1$
        break;
      case CompletionProposal.FIELD_REF:
        buffer.append("FIELD_REF"); //$NON-NLS-1$
        break;
      case CompletionProposal.KEYWORD:
        buffer.append("KEYWORD"); //$NON-NLS-1$
        break;
      case CompletionProposal.LABEL_REF:
        buffer.append("LABEL_REF"); //$NON-NLS-1$
        break;
      case CompletionProposal.LOCAL_VARIABLE_REF:
        buffer.append("LOCAL_VARIABLE_REF"); //$NON-NLS-1$
        break;
      case CompletionProposal.METHOD_DECLARATION:
        buffer.append("METHOD_DECLARATION"); //$NON-NLS-1$
        if (this.isConstructor) {
          buffer.append("<CONSTRUCTOR>"); //$NON-NLS-1$
        }
        break;
      case CompletionProposal.METHOD_REF:
        buffer.append("METHOD_REF"); //$NON-NLS-1$
        if (this.isConstructor) {
          buffer.append("<CONSTRUCTOR>"); //$NON-NLS-1$
        }
        break;
      case CompletionProposal.LIBRARY_PREFIX:
        buffer.append("LIBRARY_PREFIX"); //$NON-NLS-1$
        break;
      case CompletionProposal.TYPE_REF:
        buffer.append("TYPE_REF"); //$NON-NLS-1$
        break;
      case CompletionProposal.VARIABLE_DECLARATION:
        buffer.append("VARIABLE_DECLARATION"); //$NON-NLS-1$
        break;
      case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
        buffer.append("POTENTIAL_METHOD_DECLARATION"); //$NON-NLS-1$
        break;
      case CompletionProposal.METHOD_NAME_REFERENCE:
        buffer.append("METHOD_IMPORT"); //$NON-NLS-1$
        break;
      case CompletionProposal.ANNOTATION_ATTRIBUTE_REF:
        buffer.append("ANNOTATION_ATTRIBUTE_REF"); //$NON-NLS-1$
        break;
      case CompletionProposal.JAVADOC_BLOCK_TAG:
        buffer.append("JAVADOC_BLOCK_TAG"); //$NON-NLS-1$
        break;
      case CompletionProposal.JAVADOC_INLINE_TAG:
        buffer.append("JAVADOC_INLINE_TAG"); //$NON-NLS-1$
        break;
      case CompletionProposal.JAVADOC_FIELD_REF:
        buffer.append("JAVADOC_FIELD_REF"); //$NON-NLS-1$
        break;
      case CompletionProposal.JAVADOC_METHOD_REF:
        buffer.append("JAVADOC_METHOD_REF"); //$NON-NLS-1$
        break;
      case CompletionProposal.JAVADOC_TYPE_REF:
        buffer.append("JAVADOC_TYPE_REF"); //$NON-NLS-1$
        break;
      case CompletionProposal.JAVADOC_PARAM_REF:
        buffer.append("JAVADOC_PARAM_REF"); //$NON-NLS-1$
        break;
      case CompletionProposal.JAVADOC_VALUE_REF:
        buffer.append("JAVADOC_VALUE_REF"); //$NON-NLS-1$
        break;
      case CompletionProposal.FIELD_IMPORT:
        buffer.append("FIELD_IMPORT"); //$NON-NLS-1$
        break;
      case CompletionProposal.METHOD_IMPORT:
        buffer.append("METHOD_IMPORT"); //$NON-NLS-1$
        break;
      case CompletionProposal.TYPE_IMPORT:
        buffer.append("TYPE_IMPORT"); //$NON-NLS-1$
        break;
      case CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER:
        buffer.append("METHOD_REF_WITH_CASTED_RECEIVER"); //$NON-NLS-1$
        break;
      case CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER:
        buffer.append("FIELD_REF_WITH_CASTED_RECEIVER"); //$NON-NLS-1$
        break;
      case CompletionProposal.CONSTRUCTOR_INVOCATION:
        buffer.append("CONSTRUCTOR_INVOCATION"); //$NON-NLS-1$
        break;
      case CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION:
        buffer.append("ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION"); //$NON-NLS-1$
        break;
      default:
        buffer.append("PROPOSAL"); //$NON-NLS-1$
        break;

    }
    buffer.append("]{completion:"); //$NON-NLS-1$
    if (this.completion != null) {
      buffer.append(this.completion);
    }
    buffer.append(", declSign:"); //$NON-NLS-1$
    if (this.declarationSignature != null) {
      buffer.append(this.declarationSignature);
    }
    buffer.append(", sign:"); //$NON-NLS-1$
    if (this.signature != null) {
      buffer.append(this.signature);
    }
    buffer.append(", name:"); //$NON-NLS-1$
    if (this.name != null) {
      buffer.append(this.name);
    }
    buffer.append(", replace:["); //$NON-NLS-1$
    buffer.append(this.replaceStart);
    buffer.append(',');
    buffer.append(this.replaceEnd);
    buffer.append("], token:["); //$NON-NLS-1$
    buffer.append(this.tokenStart);
    buffer.append(',');
    buffer.append(this.tokenEnd);
    buffer.append("], relevance:"); //$NON-NLS-1$
    buffer.append(this.relevance);
    buffer.append('}');
    return buffer.toString();
  }

  protected char[][] findConstructorParameterNames(char[] declaringTypeName, char[] selector,
      char[][] paramTypeNames) {
    if (paramTypeNames == null || declaringTypeName == null) {
      return null;
    }

    char[][] parameters = null;
    int length = paramTypeNames.length;

//    char[] tName = declaringTypeName;
//    Object cachedType = this.completionEngine.typeCache.get(tName);

    Type type = null;
//    if(cachedType != null) {
//      if(cachedType != NO_ATTACHED_SOURCE && cachedType instanceof BinaryType) {
//        type = (BinaryType)cachedType;
//      }
//    } else {
    type = findTypeNamed(new String(declaringTypeName));
//    NameLookup.Answer answer = this.nameLookup.findType(new String(
//        declaringTypeName), false, NameLookup.ACCEPT_CLASSES
//        & NameLookup.ACCEPT_INTERFACES, true/* consider secondary types */,
//        false/* do NOT wait for indexes */,
//        false/* don't check restrictions */, null);
//    type = answer == null ? null : answer.type;
//      if(type instanceof BinaryType){
//        this.completionEngine.typeCache.put(tName, type);
//      } else {
//    type = null;
//      }
//    }

    if (type != null) {
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=316937
      // BinaryType#getMethod() creates a new instance of BinaryMethod, which is
      // a dummy.
      // Instead we have to use IType#findMethods() to get a handle to the
      // method of our interest.
      try {
        Method method = findMethod(type, selector, paramTypeNames);
//        if (this.hasNoParameterNamesFromIndex) {
//
//          IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot)type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
//          if (packageFragmentRoot.isArchive() ||
//              this.completionEngine.openedBinaryTypes < getOpenedBinaryTypesThreshold()) {
//            SourceMapper mapper = ((JavaElement)method).getSourceMapper();
//            if (mapper != null) {
//              char[][] paramNames = mapper.getMethodParameterNames(method);
//
//              // map source and try to find parameter names
//              if(paramNames == null) {
//                if (!packageFragmentRoot.isArchive()) this.completionEngine.openedBinaryTypes++;
//                IBinaryType info = (IBinaryType) ((BinaryType) type).getElementInfo();
//                char[] source = mapper.findSource(type, info);
//                if (source != null){
//                  mapper.mapSource(type, source, info);
//                }
//                paramNames = mapper.getMethodParameterNames(method);
//              }
//
//              if(paramNames != null) {
//                parameters = paramNames;
//              }
//            }
//          }
//        } else {
        String[] argumentNames = method.getParameterNames();
        if (argumentNames != null && argumentNames.length == length) {
          parameters = new char[argumentNames.length][];
          for (int i = 0; i < argumentNames.length; i++) {
            parameters[i] = argumentNames[i].toCharArray();
          }
          return parameters;
        }

        parameters = new char[length][];
        String[] params = method.getParameterNames();
        for (int i = 0; i < length; i++) {
          parameters[i] = params[i].toCharArray();
        }
//        }
      } catch (DartModelException e) {
        parameters = null;
      }
    }

    // default parameters name
    if (parameters == null) {
      parameters = CompletionEngine.createDefaultParameterNames(length);
    }

    return parameters;
  }

  protected char[][] findMethodParameterNames(char[] declaringTypeName, char[] selector,
      char[][] paramTypeNames) {
    if (paramTypeNames == null || declaringTypeName == null) {
      return null;
    }

    char[][] parameters = null;
    int length = paramTypeNames.length;

//    char[] tName = declaringTypeName;
//    Object cachedType = this.completionEngine.typeCache.get(tName);

    Type type = null;
//    if(cachedType != null) {
//      if(cachedType != NO_ATTACHED_SOURCE && cachedType instanceof BinaryType) {
//        type = (BinaryType)cachedType;
//      }
//    } else {
    type = findTypeNamed(new String(declaringTypeName));
//    NameLookup.Answer answer = this.nameLookup.findType(new String(
//        declaringTypeName), false, NameLookup.ACCEPT_CLASSES
//        & NameLookup.ACCEPT_INTERFACES, true/* consider secondary types */,
//        false/* do NOT wait for indexes */,
//        false/* don't check restrictions */, null);
//    type = answer == null ? null : answer.type;
//      if(type instanceof BinaryType){
//        this.completionEngine.typeCache.put(tName, type);
//      } else {
//    type = null;
//      }
//    }

//    if (type != null) {
//      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=316937
//      // BinaryType#getMethod() creates a new instance of BinaryMethod, which is
//      // a dummy.
//      // Instead we have to use IType#findMethods() to get a handle to the
//      // method of our interest.
    try {
      Method method = findMethod(type, selector, paramTypeNames);
      parameters = new char[length][];
      String[] params = method.getParameterNames();
      for (int i = 0; i < length; i++) {
        parameters[i] = params[i].toCharArray();
      }
    } catch (DartModelException e) {
      parameters = null;
    }
//    }

    // default parameters name
    if (parameters == null) {
      parameters = CompletionEngine.createDefaultParameterNames(length);
    }

    return parameters;
  }

  protected char[] getDeclarationTypeName() {
    return this.declarationTypeName;
  }

  protected char[] getTypeName() {
    return this.typeName;
  }

  protected void setDeclarationTypeName(char[] declarationTypeName) {
    this.declarationTypeName = declarationTypeName;
  }

  protected void setIsContructor(boolean isConstructor) {
    this.isConstructor = isConstructor;
  }

  protected void setIsGetter(boolean isGetter) {
    this.isGetter = isGetter;
  }

  protected void setIsInterface(boolean isInterface) {
    this.isInterface = isInterface;
  }

  protected void setIsSetter(boolean isSetter) {
    this.isSetter = isSetter;
  }

  protected void setParameterTypeNames(char[][] parameterTypeNames) {
    this.parameterTypeNames = parameterTypeNames;
  }

  protected void setTypeName(char[] typeName) {
    this.typeName = typeName;
  }

  private Method findMethod(Type type, char[] selector, char[][] paramTypeNames) {
    Method method = null;
    int startingIndex = 0;
    String[] args;
    args = new String[paramTypeNames.length];
    int length = args.length;
    for (int i = startingIndex; i < length; i++) {
      args[i] = new String(paramTypeNames[i - startingIndex]);
    }
    method = type.getMethod(new String(selector), args);

    Method[] methods = type.findMethods(method);
    if (methods != null && methods.length > 0) {
      method = methods[0];
    }
    return method;
  }

  private Type findTypeNamed(String typeName) {
    SearchEngine engine = SearchEngineFactory.createSearchEngine();
    List<SearchMatch> matches;
    try {
      matches = engine.searchTypeDeclarations(
          new WorkspaceSearchScope(),
          SearchPatternFactory.createExactPattern(typeName, true),
          (SearchFilter) null,
          new NullProgressMonitor());
    } catch (SearchException ex) {
      return null;
    }
    if (matches.isEmpty()) {
      return null;
    }
    return (Type) matches.get(0).getElement();
  }

  private char[][] getParameterTypes(char[] arg) {
    DartCore.notYetImplemented();
    return null;
  }
}
