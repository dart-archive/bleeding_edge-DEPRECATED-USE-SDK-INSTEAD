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
package com.google.dart.tools.ui.text.dart;

import com.google.dart.engine.element.Element;
import com.google.dart.tools.core.completion.CompletionContext;
import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.ui.DartElementImageDescriptor;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.Flags;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.util.TypeLabelUtil;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;
import com.google.dart.tools.ui.text.editor.tmp.Signature;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledString;

import java.util.Arrays;

/**
 * Provides labels for Dart content assist proposals. The functionality is similar to the one
 * provided by {@link com.google.dart.tools.ui.DartElementLabels}, but based on signatures and
 * {@link CompletionProposal}s.
 */
public class CompletionProposalLabelProvider {
  private static final String VOID_INDICATOR = "  void"; //$NON-NLS-1$
  private static final String DYNAMIC_INDICATOR = "  dynamic"; //$NON-NLS-1$

  /**
   * The completion context.
   */
  private CompletionContext fContext;

  /**
   * Creates a new label provider.
   */
  public CompletionProposalLabelProvider() {
  }

  /**
   * Creates and returns a decorated image descriptor for a completion proposal.
   * 
   * @param proposal the proposal for which to create an image descriptor
   * @return the created image descriptor, or <code>null</code> if no image is available
   */
  @SuppressWarnings({"unused", "deprecation"})
  public ImageDescriptor createImageDescriptor(CompletionProposal proposal) {
    // char[] compUnit = proposal.getDeclarationTypeName();
    // char[] propType = proposal.getName();
    // DartProject project = proposal.getJavaProject();
    //
    // IJsGlobalScopeContainerInitializerExtension init = null;
    // Type type = proposal.getNameLookup().findType(new String(compUnit), true,
    // NameLookup.ACCEPT_ALL);
    // IPackageFragment frag = type.getPackageFragment();
    //
    // if(compUnit!=null && propType!=null)
    // init = JSDScopeUiUtil.findLibraryUiInitializer(new Path(new
    // String(compUnit)),project);
    // if(init!=null) {
    // ImageDescriptor description = init.getImage(new Path(new
    // String(compUnit)),new String(propType), project);
    // if( description!=null) return description;
    // }

    final int flags = proposal.getFlags();

    ImageDescriptor descriptor;
    switch (proposal.getKind()) {
      case CompletionProposal.METHOD_DECLARATION:
      case CompletionProposal.METHOD_NAME_REFERENCE:
      case CompletionProposal.METHOD_REF:
      case CompletionProposal.ARGUMENT_LIST:
      case CompletionProposal.OPTIONAL_ARGUMENT:
      case CompletionProposal.NAMED_ARGUMENT:
      case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
        descriptor = DartElementImageProvider.getMethodImageDescriptor(false, proposal.isPrivate());
        break;
      case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
      case CompletionProposal.TYPE_REF:
        descriptor = DartElementImageProvider.getTypeImageDescriptor(proposal.isInterface(), false);
        break;
      case CompletionProposal.FIELD_REF:
        descriptor = DartElementImageProvider.getFieldImageDescriptor(false, proposal.isPrivate());
        break;
      case CompletionProposal.LOCAL_VARIABLE_REF:
      case CompletionProposal.VARIABLE_DECLARATION:
        descriptor = DartPluginImages.DESC_OBJS_LOCAL_VARIABLE;
        break;
      case CompletionProposal.LIBRARY_PREFIX:
        descriptor = DartPluginImages.DESC_OBJS_LIBRARY;
        break;
      case CompletionProposal.TYPE_IMPORT:
        descriptor = DartPluginImages.DESC_OBJS_PACKAGE;
        break;
      case CompletionProposal.KEYWORD:
      case CompletionProposal.LABEL_REF:
        descriptor = null;
        break;
      case CompletionProposal.JAVADOC_METHOD_REF:
      case CompletionProposal.JAVADOC_TYPE_REF:
      case CompletionProposal.JAVADOC_FIELD_REF:
      case CompletionProposal.JAVADOC_BLOCK_TAG:
      case CompletionProposal.JAVADOC_INLINE_TAG:
      case CompletionProposal.JAVADOC_PARAM_REF:
        descriptor = DartPluginImages.DESC_OBJS_JAVADOCTAG;
        break;
      default:
        descriptor = null;
        Assert.isTrue(false);
    }

    if (descriptor == null) {
      return null;
    }
    return decorateImageDescriptor(descriptor, proposal);
  }

  /**
   * Creates the display label for a given <code>CompletionProposal</code>.
   * 
   * @param proposal the completion proposal to create the display label for
   * @return the display label for <code>proposal</code>
   */
  @SuppressWarnings("deprecation")
  public String createLabel(CompletionProposal proposal) {
    switch (proposal.getKind()) {
      case CompletionProposal.METHOD_NAME_REFERENCE:
      case CompletionProposal.METHOD_REF:
      case CompletionProposal.ARGUMENT_LIST:
      case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
        if (fContext != null && fContext.isInJavadoc()) {
          return createJavadocMethodProposalLabel(proposal);
        }
        return createMethodProposalLabel(proposal).getString();
      case CompletionProposal.METHOD_DECLARATION:
        return createOverrideMethodProposalLabel(proposal);
      case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
        return createAnonymousTypeLabel(proposal);
      case CompletionProposal.TYPE_REF:
        return createTypeProposalLabel(proposal);
      case CompletionProposal.JAVADOC_TYPE_REF:
        return createJavadocTypeProposalLabel(proposal);
      case CompletionProposal.JAVADOC_FIELD_REF:
      case CompletionProposal.JAVADOC_BLOCK_TAG:
      case CompletionProposal.JAVADOC_INLINE_TAG:
      case CompletionProposal.JAVADOC_PARAM_REF:
        return createJavadocSimpleProposalLabel(proposal);
      case CompletionProposal.JAVADOC_METHOD_REF:
        return createJavadocMethodProposalLabel(proposal);
      case CompletionProposal.LIBRARY_PREFIX:
        return createLibraryPrefixProposalLabel(proposal);
      case CompletionProposal.FIELD_REF:
        return createSimpleLabelWithType(proposal);
      case CompletionProposal.LOCAL_VARIABLE_REF:
      case CompletionProposal.VARIABLE_DECLARATION:
        return createSimpleLabelWithType(proposal);
      case CompletionProposal.KEYWORD:
      case CompletionProposal.LABEL_REF:
      case CompletionProposal.TYPE_IMPORT:
        return createSimpleLabel(proposal);
      default:
        Assert.isTrue(false);
        return null;
    }
  }

  /**
   * Creates and returns a parameter list of the given method or type proposal suitable for display.
   * The list does not include parentheses. The lower bound of parameter types is returned.
   * <p>
   * Examples:
   * 
   * <pre>
   *   &quot;void method(int i, Strings)&quot; -&gt; &quot;int i, String s&quot;
   *   &quot;? extends Number method(String s, ? super Number n)&quot; -&gt; &quot;String s, Number n&quot;
   * </pre>
   * </p>
   * 
   * @param proposal the proposal to create the parameter list for. Must be of kind
   *          {@link CompletionProposal#METHOD_REF} or {@link CompletionProposal#TYPE_REF}.
   * @return the list of comma-separated parameters suitable for display
   */
  public String createParameterList(CompletionProposal proposal) {
    int kind = proposal.getKind();
    switch (kind) {
      case CompletionProposal.METHOD_REF:
      case CompletionProposal.ARGUMENT_LIST:
        return appendUnboundedParameterList(new StyledString(), proposal).toString();
      default:
        Assert.isLegal(false);
        return null; // dummy
    }
  }

  @SuppressWarnings("deprecation")
  public StyledString createStyledLabel(CompletionProposal proposal) {
    // TODO(messick) rewrite to create styled labels from the beginning
    switch (proposal.getKind()) {
      case CompletionProposal.ARGUMENT_LIST:
        return createArgumentList(proposal);
      case CompletionProposal.METHOD_NAME_REFERENCE:
      case CompletionProposal.METHOD_REF:
      case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
        if (fContext != null && fContext.isInJavadoc()) {
          return new StyledString(createLabel(proposal));
        }
        return createMethodProposalLabel(proposal);
      case CompletionProposal.FIELD_REF:
        return createLabelWithType(proposal);
      case CompletionProposal.LOCAL_VARIABLE_REF:
      case CompletionProposal.VARIABLE_DECLARATION:
        return createLabelWithType(proposal);
      default:
        return new StyledString(createLabel(proposal));
    }
  }

  String createAnonymousTypeLabel(CompletionProposal proposal) {
    char[] declaringTypeSignature = proposal.getDeclarationSignature();

    StyledString buffer = new StyledString();
    buffer.append(Signature.getSignatureSimpleName(declaringTypeSignature));
    buffer.append('(');
    appendUnboundedParameterList(buffer, proposal);
    buffer.append(')');
    buffer.append("  "); //$NON-NLS-1$
    buffer.append(DartTextMessages.ResultCollector_anonymous_type);

    return buffer.toString();
  }

  StyledString createArgumentList(CompletionProposal methodProposal) {
    StyledString nameBuffer = new StyledString();
    appendUnboundedParameterList(nameBuffer, methodProposal);
    return nameBuffer;
  }

  ImageDescriptor createFieldImageDescriptor(CompletionProposal proposal) {
    return decorateImageDescriptor(
        DartElementImageProvider.getFieldImageDescriptor(false, proposal.isPrivate()),
        proposal);
  }

  /**
   * Creates a display label for the given method proposal. The display label consists of:
   * <ul>
   * <li>the method name</li>
   * <li>the raw simple name of the declaring type</li>
   * </ul>
   * <p>
   * Examples: For the <code>get(int)</code> method of a variable of type
   * <code>List<? extends Number></code>, the following display name is returned
   * <code>get(int) - List</code>.<br>
   * For the <code>add(E)</code> method of a variable of type <code>List</code>, the following
   * display name is returned: <code>add(Object) - List</code>.<br>
   * </p>
   * 
   * @param methodProposal the method proposal to display
   * @return the display label for the given method proposal
   */
  String createJavadocMethodProposalLabel(CompletionProposal methodProposal) {
    StringBuffer nameBuffer = new StringBuffer();

    // method name
    nameBuffer.append(methodProposal.getCompletion());

    // declaring type
    nameBuffer.append(" - "); //$NON-NLS-1$
    String declaringType = extractDeclaringTypeFQN(methodProposal);
    declaringType = Signature.getSimpleName(declaringType);
    nameBuffer.append(declaringType);

    return nameBuffer.toString();
  }

  String createJavadocSimpleProposalLabel(CompletionProposal proposal) {
    // TODO get rid of this
    return createSimpleLabel(proposal);
  }

  String createJavadocTypeProposalLabel(char[] fullName) {
    // only display innermost type name as type name, using any
    // enclosing types as qualification
    int qIndex = findSimpleNameStart(fullName);

    StringBuffer buf = new StringBuffer("{@link "); //$NON-NLS-1$
    buf.append(fullName, qIndex, fullName.length - qIndex);
    buf.append('}');
    if (qIndex > 0) {
      buf.append(DartElementLabels.CONCAT_STRING);
      buf.append(fullName, 0, qIndex - 1);
    }
    return buf.toString();
  }

  String createJavadocTypeProposalLabel(CompletionProposal typeProposal) {
    char[] fullName = Signature.toCharArray(typeProposal.getSignature());
    return createJavadocTypeProposalLabel(fullName);
  }

  StyledString createLabelWithType(CompletionProposal proposal) {
    StyledString buf = new StyledString();
    buf.append(proposal.getCompletion());
    char[] typeName = proposal.getReturnTypeName();

    if (typeName.length > 0) {
      if (isDynamic(typeName)) {
        buf.append(DYNAMIC_INDICATOR, StyledString.QUALIFIER_STYLER);
      } else {
        buf.append(Element.RIGHT_ARROW, StyledString.QUALIFIER_STYLER);
        TypeLabelUtil.insertTypeLabel(typeName, buf);
      }
    }
    if (proposal.isPotentialMatch()) {
      potentialize(buf, proposal);
    }
    return buf;
  }

  // TODO(messick) Delete this unused method.
  String createLabelWithTypeAndDeclaration(CompletionProposal proposal) {
    char[] name = proposal.getCompletion();
    if (!isThisPrefix(name)) {
      name = proposal.getName();
    }

    StringBuffer buf = new StringBuffer();
    buf.append(name);
    char[] typeName = Signature.getSignatureSimpleName(proposal.getSignature());
    if (typeName.length > 0 && !(Arrays.equals(Signature.ANY, typeName))) {
      buf.append(" : "); //$NON-NLS-1$
      TypeLabelUtil.insertTypeLabel(typeName, buf);
    }
    char[] declaration = proposal.getDeclarationSignature();
    if (declaration != null) {
      declaration = Signature.getSignatureSimpleName(declaration);
      if (declaration.length > 0) {
        buf.append(" - "); //$NON-NLS-1$
        TypeLabelUtil.insertTypeLabel(declaration, buf);
      }
    }

    return buf.toString();
  }

  ImageDescriptor createLibraryImageDescriptor(CompletionProposal proposal) {
    return decorateImageDescriptor(DartPluginImages.DESC_OBJS_LIBRARY, proposal);
  }

  String createLibraryPrefixProposalLabel(CompletionProposal proposal) {
    Assert.isTrue(proposal.getKind() == CompletionProposal.LIBRARY_PREFIX);
    return String.valueOf(proposal.getDeclarationSignature());
  }

  ImageDescriptor createLocalImageDescriptor(CompletionProposal proposal) {
    return decorateImageDescriptor(DartPluginImages.DESC_OBJS_LOCAL_VARIABLE, proposal);
  }

  ImageDescriptor createMethodImageDescriptor(CompletionProposal proposal) {
    return decorateImageDescriptor(
        DartElementImageProvider.getMethodImageDescriptor(false, proposal.isPrivate()),
        proposal);
  }

  /**
   * Creates a display label for the given method proposal. The display label consists of:
   * <ul>
   * <li>the method name</li>
   * <li>the parameter list (see {@link #createParameterList(CompletionProposal)})</li>
   * <li>the upper bound of the return type (see {@link SignatureUtil#getUpperBound(String)})</li>
   * <li>the raw simple name of the declaring type</li>
   * </ul>
   * <p>
   * Examples: For the <code>get(int)</code> method of a variable of type
   * <code>List<? extends Number></code>, the following display name is returned:
   * <code>get(int index)  Number - List</code>.<br>
   * For the <code>add(E)</code> method of a variable of type <code>List<? super Number></code>, the
   * following display name is returned: <code>add(Number o)  void - List</code>.<br>
   * </p>
   * 
   * @param methodProposal the method proposal to display
   * @return the display label for the given method proposal
   */
  StyledString createMethodProposalLabel(CompletionProposal methodProposal) {
    StyledString buffer = new StyledString();

    // method name
    buffer.append(methodProposal.getName());

    boolean hasParameters = methodProposal.getKind() != CompletionProposal.METHOD_NAME_REFERENCE;

    // parameters
    if (hasParameters && Character.isJavaIdentifierStart(methodProposal.getName()[0])) {
      if (!methodProposal.isGetOrSet()) {
        int start = buffer.length();
        buffer.append('(');
        appendUnboundedParameterList(buffer, methodProposal);
        buffer.append(')');
        buffer.setStyle(start, buffer.length() - start, StyledString.DECORATIONS_STYLER);
      }
    }

    // return type
    if (!methodProposal.isConstructor()) {
      char[] returnType = createTypeDisplayName(methodProposal.getReturnTypeName());
      if (!Arrays.equals(Signature.ANY, returnType)) {
        if (isVoid(returnType)) {
          buffer.append(VOID_INDICATOR, StyledString.QUALIFIER_STYLER);
        } else if (isDynamic(returnType)) {
          buffer.append(DYNAMIC_INDICATOR, StyledString.QUALIFIER_STYLER);
        } else {
          buffer.append(Element.RIGHT_ARROW, StyledString.QUALIFIER_STYLER);
          TypeLabelUtil.insertTypeLabel(returnType, buffer);
        }
      }
    }
    if (methodProposal.isPotentialMatch()) {
      potentialize(buffer, methodProposal);
    }
    return buffer;
  }

  String createOverrideMethodProposalLabel(CompletionProposal methodProposal) {
    StyledString nameBuffer = new StyledString();

    // method name
    nameBuffer.append(methodProposal.getName());

    // parameters
    nameBuffer.append('(');
    appendUnboundedParameterList(nameBuffer, methodProposal);
    nameBuffer.append(")  "); //$NON-NLS-1$

    // return type
//    char[] returnType = createTypeDisplayName(Signature.getReturnType(methodProposal.getSignature()));
//    nameBuffer.append(returnType);

    // declaring type
    nameBuffer.append(" - "); //$NON-NLS-1$
    String declaringType = new String(methodProposal.getDeclarationSignature());//extractDeclaringTypeFQN(methodProposal);
    nameBuffer.append(Messages.format(
        DartTextMessages.ResultCollector_overridingmethod,
        new String(declaringType)));

    return nameBuffer.toString();
  }

  String createSimpleLabel(CompletionProposal proposal) {
    String label = String.valueOf(proposal.getCompletion());
    return StringUtils.remove(label, CompletionProposal.CURSOR_MARKER);
  }

  String createSimpleLabelWithType(CompletionProposal proposal) {
    StringBuffer buf = new StringBuffer();
    buf.append(proposal.getCompletion());
    char[] typeName = Signature.getSignatureSimpleName(proposal.getSignature());

    if (typeName.length > 0) {
      buf.append(Element.RIGHT_ARROW);
      TypeLabelUtil.insertTypeLabel(typeName, buf);
    }
    return buf.toString();
  }

  ImageDescriptor createTypeImageDescriptor(CompletionProposal proposal) {
    return decorateImageDescriptor(
        DartElementImageProvider.getTypeImageDescriptor(true, false),
        proposal);
  }

  String createTypeProposalLabel(char[] fullName) {
    // only display innermost type name as type name, using any
    // enclosing types as qualification
    // int qIndex= findSimpleNameStart(fullName);
    //
    StringBuffer buf = new StringBuffer();
    // buf.append(fullName, qIndex, fullName.length - qIndex);
    // if (qIndex > 0) {
    // buf.append(DartElementLabels.CONCAT_STRING);
    // buf.append(fullName, 0, qIndex - 1);
    // }
    buf.append(fullName);
    return buf.toString();
  }

  /**
   * Creates a display label for a given type proposal. The display label consists of:
   * <ul>
   * <li>the simple type name (erased when the context is in Dart doc)</li>
   * <li>the package name</li>
   * </ul>
   * <p>
   * Examples: A proposal for the generic type <code>List&lt;E&gt;</code>, the display label is:
   * <code>List<E></code>.
   * </p>
   * 
   * @param typeProposal the method proposal to display
   * @return the display label for the given type proposal
   */
  String createTypeProposalLabel(CompletionProposal typeProposal) {
    StringBuffer buf = new StringBuffer();
    buf.append(typeProposal.getCompletion());
    char[] declarationSignature = typeProposal.getDeclarationSignature();
    if (declarationSignature != null && declarationSignature.length > 0) {
      buf.append(DartElementLabels.CONCAT_STRING);
      buf.append(declarationSignature);
    }
    return buf.toString();
    // char[] signature;
    // if (fContext != null && fContext.isInJavadoc())
    // signature= Signature.getTypeErasure(typeProposal.getSignature());
    // else
    // signature= typeProposal.getSignature();
    // char[] fullName= Signature.toCharArray(signature);
    // return createTypeProposalLabel(fullName);
  }

  void potentialize(StyledString label, CompletionProposal proposal) {
    label.setStyle(0, label.length(), StyledString.QUALIFIER_STYLER);
    // declaring type
    String declaringType = extractDeclaringTypeFQN(proposal);
    if (!declaringType.isEmpty()) {
      label.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
      label.append(declaringType, StyledString.QUALIFIER_STYLER);
    }
  }

  /**
   * Sets the completion context.
   * 
   * @param context the completion context
   */
  void setContext(CompletionContext context) {
    fContext = context;
  }

  /**
   * Creates a display string of a parameter list (without the parentheses) for the given parameter
   * types and names.
   * 
   * @param buffer the string buffer
   * @param parameterTypes the parameter types
   * @param parameterNames the parameter names
   * @return the display string of the parameter list defined by the passed arguments
   */
  private StyledString appendParameterSignature(StyledString buffer, char[][] parameterTypes,
      char[][] parameterNames, int positionalCount, boolean hasNamed, boolean hasOptional) {
    if (parameterTypes == null) {
      if (parameterNames != null && parameterNames.length > 0) {
        for (int i = 0; i < parameterNames.length - 1; i++) {
          buffer.append(parameterNames[i]);
          buffer.append(',');
          buffer.append(' ');
        }
        buffer.append(parameterNames[parameterNames.length - 1]);
      }
    } else {
      for (int i = 0; i < parameterTypes.length; i++) {
        if (i > 0) {
          buffer.append(',');
          buffer.append(' ');
        }
        if (i == positionalCount) {
          if (hasNamed) { // check hasNamed first because hasOptional implies hasNamed
            buffer.append('{');
          } else if (hasOptional) {
            buffer.append('[');
          }
        }
        if (!Arrays.equals(Signature.ANY, parameterTypes[i])) {
          buffer.append(parameterTypes[i]);
          buffer.append(' ');
        }

        if (parameterNames != null && parameterNames[i] != null) {
          buffer.append(parameterNames[i]);
        }
      }
      if (hasNamed) {
        buffer.append('}');
      } else if (hasOptional) {
        buffer.append(']');
      }
    }
    return buffer;
  }

  /**
   * Appends the parameter list to <code>buffer</code>.
   * 
   * @param buffer the buffer to append to
   * @param methodProposal the method proposal
   * @return the modified <code>buffer</code>
   */
  private StyledString appendUnboundedParameterList(StyledString buffer,
      CompletionProposal methodProposal) {
    char[][] parameterNames = methodProposal.findParameterNames(null);
    char[][] parameterTypes = methodProposal.getParameterTypeNames();
    int positionalCount = methodProposal.getPositionalParameterCount();
    boolean hasNamed = methodProposal.hasNamedParameters();
    boolean hasOptional = methodProposal.hasOptionalParameters();

//    for (int i = 0; i < parameterTypes.length; i++) {
//      parameterTypes[i] = createTypeDisplayName(parameterTypes[i]);
//    }
    return appendParameterSignature(
        buffer,
        parameterTypes,
        parameterNames,
        positionalCount,
        hasNamed,
        hasOptional);
  }

  /**
   * Returns the display string for a Dart type signature.
   * 
   * @param typeSignature the type signature to create a display name for
   * @return the display name for <code>typeSignature</code>
   * @throws IllegalArgumentException if <code>typeSignature</code> is not a valid signature
   * @see Signature#toCharArray(char[])
   * @see Signature#getSimpleName(char[])
   */
  private char[] createTypeDisplayName(char[] typeSignature) throws IllegalArgumentException {
    char[] displayName = Signature.getSimpleName(Signature.toCharArray(typeSignature));

    // XXX see https://bugs.eclipse.org/bugs/show_bug.cgi?id=84675
    boolean useShortGenerics = false;
    if (useShortGenerics) {
      StringBuffer buf = new StringBuffer();
      buf.append(displayName);
      int pos;
      do {
        pos = buf.indexOf("? extends "); //$NON-NLS-1$
        if (pos >= 0) {
          buf.replace(pos, pos + 10, "+"); //$NON-NLS-1$
        } else {
          pos = buf.indexOf("? super "); //$NON-NLS-1$
          if (pos >= 0) {
            buf.replace(pos, pos + 8, "-"); //$NON-NLS-1$
          }
        }
      } while (pos >= 0);
      return buf.toString().toCharArray();
    }
    return displayName;
  }

//  /**
//   * Converts the display name for an array type into a variable arity display name.
//   * <p>
//   * Examples:
//   * <ul>
//   * <li>"int[]" -> "int..."</li>
//   * <li>"Object[][]" -> "Object[]..."</li>
//   * <li>"String" -> "String"</li>
//   * </ul>
//   * </p>
//   * <p>
//   * If <code>typeName</code> does not include the substring "[]", it is returned unchanged.
//   * </p>
//   * 
//   * @param typeName the type name to convert
//   * @return the converted type name
//   */
//  private char[] convertToVararg(char[] typeName) {
//    if (typeName == null) {
//      return typeName;
//    }
//    final int len = typeName.length;
//    if (len < 2) {
//      return typeName;
//    }
//
//    if (typeName[len - 1] != ']') {
//      return typeName;
//    }
//    if (typeName[len - 2] != '[') {
//      return typeName;
//    }
//
//    char[] vararg = new char[len + 1];
//    System.arraycopy(typeName, 0, vararg, 0, len - 2);
//    vararg[len - 2] = '.';
//    vararg[len - 1] = '.';
//    vararg[len] = '.';
//    return vararg;
//  }

  /**
   * Returns a version of <code>descriptor</code> decorated according to the passed
   * <code>modifier</code> flags.
   * 
   * @param descriptor the image descriptor to decorate
   * @param proposal the proposal
   * @return an image descriptor for a method proposal
   * @see Flags
   */
  @SuppressWarnings("deprecation")
  private ImageDescriptor decorateImageDescriptor(ImageDescriptor descriptor,
      CompletionProposal proposal) {
    int adornments = 0;
    int flags = proposal.getFlags();
    int kind = proposal.getKind();

    if (kind == CompletionProposal.FIELD_REF || kind == CompletionProposal.METHOD_DECLARATION
        || kind == CompletionProposal.METHOD_DECLARATION
        || kind == CompletionProposal.METHOD_NAME_REFERENCE
        || kind == CompletionProposal.METHOD_REF) {
      if (Flags.isStatic(flags)) {
        adornments |= DartElementImageDescriptor.STATIC;
      }
    }

    if (kind == CompletionProposal.TYPE_REF && Flags.isAbstract(flags)) {
      adornments |= DartElementImageDescriptor.ABSTRACT;
    }
    if (proposal.isDeprecated()) {
      adornments |= DartElementImageDescriptor.DEPRECATED;
    }

    return new DartElementImageDescriptor(
        descriptor,
        adornments,
        DartElementImageProvider.SMALL_SIZE);
  }

  /**
   * Extracts the fully qualified name of the declaring type of a method reference.
   * 
   * @param methodProposal a proposed method
   * @return the qualified name of the declaring type
   */
  private String extractDeclaringTypeFQN(CompletionProposal methodProposal) {
    char[] declaringTypeSignature = methodProposal.getDeclarationSignature();
    if (declaringTypeSignature == null) {
      return "Object"; //$NON-NLS-1$
    }
    return String.valueOf(declaringTypeSignature);
  }

  private int findSimpleNameStart(char[] array) {
    int lastDot = 0;
    for (int i = 0, len = array.length; i < len; i++) {
      char ch = array[i];
      if (ch == '<') {
        return lastDot;
      } else if (ch == '.') {
        lastDot = i + 1;
      }
    }
    return lastDot;
  }

  private boolean isDynamic(char[] name) {
    return name != null && name.length == 9 && name[0] == '<' && name[1] == 'd' && name[2] == 'y'
        && name[3] == 'n' && name[4] == 'a' && name[5] == 'm' && name[6] == 'i' && name[7] == 'c'
        && name[8] == '>';
  }

  /**
   * Returns whether the given string starts with "this.".
   * 
   * @param string
   * @return <code>true</code> if the given string starts with "this."
   */
  private boolean isThisPrefix(char[] string) {
    if (string == null || string.length < 5) {
      return false;
    }
    return string[0] == 't' && string[1] == 'h' && string[2] == 'i' && string[3] == 's'
        && string[4] == '.';
  }

  private boolean isVoid(char[] name) {
    return name != null && name.length == 4 && name[0] == 'v' && name[1] == 'o' && name[2] == 'i'
        && name[3] == 'd';
  }

}
