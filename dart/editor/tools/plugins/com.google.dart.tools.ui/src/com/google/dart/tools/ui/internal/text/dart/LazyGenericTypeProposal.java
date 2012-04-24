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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeHierarchy;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.SignatureUtil;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorHighlightingSynchronizer;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import java.util.LinkedList;
import java.util.List;

/**
 * Proposal for generic types.
 */
public final class LazyGenericTypeProposal extends LazyDartTypeCompletionProposal {
  /**
   * Short-lived context information object for generic types. Currently, these are only created
   * after inserting a type proposal, as core doesn't give us the correct type proposal from within
   * SomeType<|>.
   */
  private static class ContextInformation implements IContextInformation,
      IContextInformationExtension {
    private final String fInformationDisplayString;
    private final String fContextDisplayString;
    private final Image fImage;
    private final int fPosition;

    ContextInformation(LazyGenericTypeProposal proposal) {
      // don't cache the proposal as content assistant
      // might hang on to the context info
      fContextDisplayString = proposal.getDisplayString();
      fInformationDisplayString = computeContextString(proposal);
      fImage = proposal.getImage();
      fPosition = proposal.getReplacementOffset() + proposal.getReplacementString().indexOf('<')
          + 1;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ContextInformation) {
        ContextInformation ci = (ContextInformation) obj;
        return getContextInformationPosition() == ci.getContextInformationPosition()
            && getInformationDisplayString().equals(ci.getInformationDisplayString());
      }
      return false;
    }

    @Override
    public String getContextDisplayString() {
      return fContextDisplayString;
    }

    @Override
    public int getContextInformationPosition() {
      return fPosition;
    }

    @Override
    public Image getImage() {
      return fImage;
    }

    @Override
    public String getInformationDisplayString() {
      return fInformationDisplayString;
    }

    @Override
    public int hashCode() {
      int low = fContextDisplayString != null ? fContextDisplayString.hashCode() : 0;
      return fPosition << 24 | fInformationDisplayString.hashCode() << 16 | low;
    }

    private String computeContextString(LazyGenericTypeProposal proposal) {
      try {
        TypeArgumentProposal[] proposals = proposal.computeTypeArgumentProposals();
        if (proposals.length == 0) {
          return null;
        }

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < proposals.length; i++) {
          buf.append(proposals[i].getDisplayName());
          if (i < proposals.length - 1) {
            buf.append(", "); //$NON-NLS-1$
          }
        }
        return buf.toString();

      } catch (DartModelException e) {
        return null;
      }
    }

  }

  private static final class TypeArgumentProposal {
    private final boolean fIsAmbiguous;
    private final String fProposal;
    private final String fTypeDisplayName;

    @SuppressWarnings("unused")
    TypeArgumentProposal(String proposal, boolean ambiguous, String typeDisplayName) {
      fIsAmbiguous = ambiguous;
      fProposal = proposal;
      fTypeDisplayName = typeDisplayName;
    }

    public String getDisplayName() {
      return fTypeDisplayName;
    }

    @Override
    public String toString() {
      return fProposal;
    }

    boolean isAmbiguous() {
      return fIsAmbiguous;
    }
  }

  /** Triggers for types. Do not modify. */
  private final static char[] GENERIC_TYPE_TRIGGERS = new char[] {'.', '\t', '[', '(', '<', ' '};

  private IRegion fSelectedRegion; // initialized by apply()
  private TypeArgumentProposal[] fTypeArgumentProposals;

  public LazyGenericTypeProposal(CompletionProposal typeProposal,
      DartContentAssistInvocationContext context) {
    super(typeProposal, context);
  }

  @Override
  public void apply(IDocument document, char trigger, int offset) {

    if (shouldAppendArguments(document, offset, trigger)) {
      try {
        TypeArgumentProposal[] typeArgumentProposals = computeTypeArgumentProposals();
        if (typeArgumentProposals.length > 0) {

          int[] offsets = new int[typeArgumentProposals.length];
          int[] lengths = new int[typeArgumentProposals.length];
          StringBuffer buffer = createParameterList(typeArgumentProposals, offsets, lengths);

          // set the generic type as replacement string
          boolean insertClosingParenthesis = trigger == '(' && autocloseBrackets();
          if (insertClosingParenthesis) {
            updateReplacementWithParentheses(buffer);
          }
          super.setReplacementString(buffer.toString());

          // add import & remove package, update replacement offset
          super.apply(document, '\0', offset);

          if (getTextViewer() != null) {
            if (hasAmbiguousProposals(typeArgumentProposals)) {
              adaptOffsets(offsets, buffer);
              installLinkedMode(document, offsets, lengths, typeArgumentProposals,
                  insertClosingParenthesis);
            } else {
              if (insertClosingParenthesis) {
                setUpLinkedMode(document, ')');
              } else {
                fSelectedRegion = new Region(getReplacementOffset()
                    + getReplacementString().length(), 0);
              }
            }
          }

          return;
        }
      } catch (DartModelException e) {
        // log and continue
        DartToolsPlugin.log(e);
      }
    }

    // default is to use the super implementation
    // reasons:
    // - not a parameterized type,
    // - already followed by <type arguments>
    // - proposal type does not inherit from expected type
    super.apply(document, trigger, offset);
  }

  @Override
  public Point getSelection(IDocument document) {
    if (fSelectedRegion == null) {
      return super.getSelection(document);
    }

    return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
  }

  @Override
  protected IContextInformation computeContextInformation() {
    try {
      if (hasParameters()) {
        TypeArgumentProposal[] proposals = computeTypeArgumentProposals();
        if (hasAmbiguousProposals(proposals)) {
          return new ContextInformation(this);
        }
      }
    } catch (DartModelException e) {
    }
    return super.computeContextInformation();
  }

  @Override
  protected int computeCursorPosition() {
    if (fSelectedRegion != null) {
      return fSelectedRegion.getOffset() - getReplacementOffset();
    }
    return super.computeCursorPosition();
  }

  @Override
  protected char[] computeTriggerCharacters() {
    return GENERIC_TYPE_TRIGGERS;
  }

  /**
   * Adapt the parameter offsets to any modification of the replacement string done by
   * <code>apply</code>. For example, applying the proposal may add an import instead of inserting
   * the fully qualified name.
   * <p>
   * This assumes that modifications happen only at the beginning of the replacement string and do
   * not touch the type arguments list.
   * </p>
   * 
   * @param offsets the offsets to modify
   * @param buffer the original replacement string
   */
  private void adaptOffsets(int[] offsets, StringBuffer buffer) {
    String replacementString = getReplacementString();
    int delta = buffer.length() - replacementString.length(); // due to using an import instead of package
    for (int i = 0; i < offsets.length; i++) {
      offsets[i] -= delta;
    }
  }

  /**
   * Computes one inheritance path from <code>superType</code> to <code>subType</code> or
   * <code>null</code> if <code>subType</code> does not inherit from <code>superType</code>. Note
   * that there may be more than one inheritance path - this method simply returns one.
   * <p>
   * The returned array contains <code>superType</code> at its first index, and <code>subType</code>
   * at its last index. If <code>subType</code> equals <code>superType</code> , an array of length 1
   * is returned containing that type.
   * </p>
   * 
   * @param subType the sub type
   * @param superType the super type
   * @return an inheritance path from <code>superType</code> to <code>subType</code>, or
   *         <code>null</code> if <code>subType</code> does not inherit from <code>superType</code>
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  @SuppressWarnings("unused")
  private Type[] computeInheritancePath(Type subType, Type superType) throws DartModelException {
    if (superType == null) {
      return null;
    }

    // optimization: avoid building the type hierarchy for the identity case
    if (superType.equals(subType)) {
      return new Type[] {subType};
    }

    TypeHierarchy hierarchy = subType.newSupertypeHierarchy(getProgressMonitor());
    if (!hierarchy.contains(superType)) {
      return null; // no path
    }

    List<Type> path = new LinkedList<Type>();
    path.add(superType);
    do {
      // any sub type must be on a hierarchy chain from superType to subType
      superType = hierarchy.getSubtypes(superType)[0];
      path.add(superType);
    } while (!superType.equals(subType)); // since the equality case is handled above, we can spare one check

    return path.toArray(new Type[path.size()]);
  }

  /**
   * Computes the type argument proposals for this type proposals. If there is an expected type
   * binding that is a super type of the proposed type, the wildcard type arguments of the proposed
   * type that can be mapped through to type the arguments of the expected type binding are bound
   * accordingly.
   * <p>
   * For type arguments that cannot be mapped to arguments in the expected type, or if there is no
   * expected type, the upper bound of the type argument is proposed.
   * </p>
   * <p>
   * The argument proposals have their <code>isAmbiguos</code> flag set to <code>false</code> if the
   * argument can be mapped to a non-wildcard type argument in the expected type, otherwise the
   * proposal is ambiguous.
   * </p>
   * 
   * @return the type argument proposals for the proposed type
   * @throws DartModelException if accessing the Dart model fails
   */
  private TypeArgumentProposal[] computeTypeArgumentProposals() throws DartModelException {
    if (fTypeArgumentProposals == null) {
//
//      Type type = (Type) getDartElement();
//      if (type == null) {
//        return new TypeArgumentProposal[0];
//      }
//
//      TypeParameter[] parameters = type.getTypeParameters();
//      if (parameters.length == 0) {
//        return new TypeArgumentProposal[0];
//      }
//
//      TypeArgumentProposal[] arguments = new TypeArgumentProposal[parameters.length];
//
//      TypeBinding expectedTypeBinding = getExpectedType();
//      if (expectedTypeBinding != null && expectedTypeBinding.isParameterizedType()) {
//        // in this case, the type arguments we propose need to be compatible
//        // with the corresponding type parameters to declared type
//
//        Type expectedType = (Type) expectedTypeBinding.getDartElement();
//
//        Type[] path = computeInheritancePath(type, expectedType);
//        if (path == null) {
//          // proposed type does not inherit from expected type
//          // the user might be looking for an inner type of proposed type
//          // to instantiate -> do not add any type arguments
//          return new TypeArgumentProposal[0];
//        }
//
//        int[] indices = new int[parameters.length];
//        for (int paramIdx = 0; paramIdx < parameters.length; paramIdx++) {
//          indices[paramIdx] = mapTypeParameterIndex(path, path.length - 1, paramIdx);
//        }
//
//        // for type arguments that are mapped through to the expected type's
//        // parameters, take the arguments of the expected type
//        TypeBinding[] typeArguments = expectedTypeBinding.getTypeArguments();
//        for (int paramIdx = 0; paramIdx < parameters.length; paramIdx++) {
//          if (indices[paramIdx] != -1) {
//            // type argument is mapped through
//            TypeBinding binding = typeArguments[indices[paramIdx]];
//            arguments[paramIdx] = computeTypeProposal(binding, parameters[paramIdx]);
//          }
//        }
//      }
//
//      // for type arguments that are not mapped through to the expected type,
//      // take the lower bound of the type parameter
//      for (int i = 0; i < arguments.length; i++) {
//        if (arguments[i] == null) {
//          arguments[i] = computeTypeProposal(parameters[i]);
//        }
//      }
//      fTypeArgumentProposals = arguments;
      fTypeArgumentProposals = new TypeArgumentProposal[0];
    }
    return fTypeArgumentProposals;
  }

//  private String computeTypeParameterDisplayName(TypeParameter parameter, String[] bounds) {
//    if (bounds.length == 0 || bounds.length == 1 && "java.lang.Object".equals(bounds[0])) {
//      return parameter.getElementName();
//    }
//    StringBuffer buf = new StringBuffer(parameter.getElementName());
//    buf.append(" extends "); //$NON-NLS-1$
//    for (int i = 0; i < bounds.length; i++) {
//      buf.append(Signature.getSimpleName(bounds[i]));
//      if (i < bounds.length - 1) {
//        buf.append(" & "); //$NON-NLS-1$
//      }
//    }
//    return buf.toString();
//  }

  /**
   * Returns a type argument proposal for a given type binding. The proposal is:
   * <ul>
   * <li>the simple type name for normal types or type variables (unambigous proposal)</li>
   * <li>for wildcard types (ambigous proposals):
   * <ul>
   * <li>the upper bound for wildcards with an upper bound</li>
   * <li>the {@linkplain #computeTypeProposal(TypeParameter) parameter proposal} for unbounded
   * wildcards or wildcards with a lower bound</li>
   * </ul>
   * </li>
   * </ul>
   * 
   * @param binding the type argument binding in the expected type
   * @param parameter the type parameter of the inserted type
   * @return a type argument proposal for <code>binding</code>
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
//  private TypeArgumentProposal computeTypeProposal(TypeBinding binding, TypeParameter parameter)
//      throws DartModelException {
//    final String name = Bindings.getTypeQualifiedName(binding);
//    if (binding.isWildcardType()) {
//
//      if (binding.isUpperbound()) {
//        // replace the wildcard ? with the type parameter name to get "E extends Bound" instead of "? extends Bound"
//        String contextName = name.replaceFirst("\\?", parameter.getElementName()); //$NON-NLS-1$
//        // upper bound - the upper bound is the bound itself
//        return new TypeArgumentProposal(binding.getBound().getName(), true, contextName);
//      }
//
//      // no or upper bound - use the type parameter of the inserted type, as it may be more
//      // restrictive (eg. List<?> list= new SerializableList<Serializable>())
//      return computeTypeProposal(parameter);
//    }
//
//    // not a wildcard but a type or type variable - this is unambigously the right thing to insert
//    return new TypeArgumentProposal(name, false, name);
//  }

  /**
   * Returns a type argument proposal for a given type parameter. The proposal is:
   * <ul>
   * <li>the type bound for type parameters with a single bound</li>
   * <li>the type parameter name for all other (unbounded or more than one bound) type parameters</li>
   * </ul>
   * Type argument proposals for type parameters are always ambiguous.
   * 
   * @param parameter the type parameter of the inserted type
   * @return a type argument proposal for <code>parameter</code>
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
//  private TypeArgumentProposal computeTypeProposal(TypeParameter parameter)
//      throws DartModelException {
//    String[] bounds = parameter.getBounds();
//    String elementName = parameter.getElementName();
//    String displayName = computeTypeParameterDisplayName(parameter, bounds);
//    if (bounds.length == 1 && !"java.lang.Object".equals(bounds[0])) {
//      return new TypeArgumentProposal(Signature.getSimpleName(bounds[0]), true, displayName);
//    } else {
//      return new TypeArgumentProposal(elementName, true, displayName);
//    }
//  }

  private StringBuffer createParameterList(TypeArgumentProposal[] typeArguments, int[] offsets,
      int[] lengths) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(getReplacementString());

    FormatterPrefs prefs = getFormatterPrefs();
    final char LESS = '<';
    final char GREATER = '>';
    if (prefs.beforeOpeningBracket) {
      buffer.append(SPACE);
    }
    buffer.append(LESS);
    if (prefs.afterOpeningBracket) {
      buffer.append(SPACE);
    }
    StringBuffer separator = new StringBuffer(3);
    if (prefs.beforeTypeArgumentComma) {
      separator.append(SPACE);
    }
    separator.append(COMMA);
    if (prefs.afterTypeArgumentComma) {
      separator.append(SPACE);
    }

    for (int i = 0; i != typeArguments.length; i++) {
      if (i != 0) {
        buffer.append(separator);
      }

      offsets[i] = buffer.length();
      buffer.append(typeArguments[i]);
      lengths[i] = buffer.length() - offsets[i];
    }
    if (prefs.beforeClosingBracket) {
      buffer.append(SPACE);
    }
    buffer.append(GREATER);

    return buffer;
  }

  /**
   * Finds and returns the super type signature in the <code>extends</code> or
   * <code>implements</code> clause of <code>subType</code> that corresponds to
   * <code>superType</code>.
   * 
   * @param subType a direct and true sub type of <code>superType</code>
   * @param superType a direct super type (super class or interface) of <code>subType</code>
   * @return the super type signature of <code>subType</code> referring to <code>superType</code>
   * @throws DartModelException if extracting the super type signatures fails, or if
   *           <code>subType</code> contains no super type signature to <code>superType</code>
   */
  private String findMatchingSuperTypeSignature(Type subType, Type superType)
      throws DartModelException {
    String[] signatures = getSuperTypeSignatures(subType, superType);
    for (int i = 0; i < signatures.length; i++) {
      String signature = signatures[i];
      String qualified = signature;//SignatureUtil.qualifySignature(signature, subType);
      String subFQN = SignatureUtil.stripSignatureToFQN(qualified);

      @SuppressWarnings("deprecation")
      String superFQN = superType.getTypeQualifiedName('.');
      if (subFQN.equals(superFQN)) {
        return signature;
      }

      // TODO handle local types
    }

    throw new DartModelException(new CoreException(new Status(IStatus.ERROR,
        DartToolsPlugin.getPluginId(), IStatus.OK, "Illegal hierarchy", null))); //$NON-NLS-1$
  }

  /**
   * Finds and returns the index of the type argument named <code>argument</code> in the given super
   * type signature.
   * <p>
   * If <code>signature</code> does not contain a corresponding type argument, or if
   * <code>signature</code> has no type parameters (i.e. is a reference to a non-parameterized type
   * or a raw type), -1 is returned.
   * </p>
   * 
   * @param signature the super type signature from a type's <code>extends</code> or
   *          <code>implements</code> clause
   * @param argument the name of the type argument to find
   * @return the index of the given type argument, or -1 if there is none
   */
  @SuppressWarnings("unused")
  private int findMatchingTypeArgumentIndex(String signature, String argument) {
//    String[] typeArguments = Signature.getTypeArguments(signature);
//    for (int i = 0; i < typeArguments.length; i++) {
//      if (Signature.getSignatureSimpleName(typeArguments[i]).equals(argument)) {
//        return i;
//      }
//    }
    return -1;
  }

  /**
   * Returns the type binding of the expected type as it is contained in the code completion
   * context.
   * 
   * @return the binding of the expected type
   */
//  private TypeBinding getExpectedType() {
//    char[][] chKeys = fInvocationContext.getCoreContext().getExpectedTypesKeys();
//    if (chKeys == null || chKeys.length == 0) {
//      return null;
//    }
//
//    String[] keys = new String[chKeys.length];
//    for (int i = 0; i < keys.length; i++) {
//      keys[i] = String.valueOf(chKeys[0]);
//    }
//
//    final ASTParser parser = ASTParser.newParser(AST.JLS3);
//    parser.setProject(fCompilationUnit.getJavaProject());
//    parser.setResolveBindings(true);
//    parser.setStatementsRecovery(true);
//
//    final Map<String, IBinding> bindings = new HashMap<String, IBinding>();
//    ASTRequestor requestor = new ASTRequestor() {
//      @Override
//      public void acceptBinding(String bindingKey, IBinding binding) {
//        bindings.put(bindingKey, binding);
//      }
//    };
//    parser.createASTs(new ICompilationUnit[0], keys, requestor, null);
//
//    if (bindings.size() > 0) {
//      return (TypeBinding) bindings.get(keys[0]);
//    }
//
//    return null;
//  }

  /**
   * Returns the currently active Dart editor, or <code>null</code> if it cannot be determined.
   * 
   * @return the currently active Dart editor, or <code>null</code>
   */
  private DartEditor getDartEditor() {
    IEditorPart part = DartToolsPlugin.getActivePage().getActiveEditor();
    if (part instanceof DartEditor) {
      return (DartEditor) part;
    } else {
      return null;
    }
  }

  private NullProgressMonitor getProgressMonitor() {
    return new NullProgressMonitor();
  }

  /**
   * Returns the super interface signatures of <code>subType</code> if <code>superType</code> is an
   * interface, otherwise returns the super type signature.
   * 
   * @param subType the sub type signature
   * @param superType the super type signature
   * @return the super type signatures of <code>subType</code>
   * @throws DartModelException if any Dart model operation fails
   */
  private String[] getSuperTypeSignatures(Type subType, Type superType) throws DartModelException {
//    if (superType.isInterface()) {
//      return subType.getSuperInterfaceTypeSignatures();
//    } else {
//      return new String[] {subType.getSuperclassTypeSignature()};
//    }
    return superType.getSupertypeNames();
  }

  private boolean hasAmbiguousProposals(TypeArgumentProposal[] typeArgumentProposals) {
    boolean hasAmbiguousProposals = false;
    for (int i = 0; i < typeArgumentProposals.length; i++) {
      if (typeArgumentProposals[i].isAmbiguous()) {
        hasAmbiguousProposals = true;
        break;
      }
    }
    return hasAmbiguousProposals;
  }

  private boolean hasParameters() {
//    try {
//      Type type = (Type) getDartElement();
//      if (type == null) {
//        return false;
//      }
//      return type.getTypeParameters().length > 0;
//    } catch (DartModelException e) {
//      return false;
//    }
    return false;
  }

  private void installLinkedMode(IDocument document, int[] offsets, int[] lengths,
      TypeArgumentProposal[] typeArgumentProposals, boolean withParentheses) {
    int replacementOffset = getReplacementOffset();
    String replacementString = getReplacementString();

    try {
      LinkedModeModel model = new LinkedModeModel();
      for (int i = 0; i != offsets.length; i++) {
        if (typeArgumentProposals[i].isAmbiguous()) {
          LinkedPositionGroup group = new LinkedPositionGroup();
          group.addPosition(new LinkedPosition(document, replacementOffset + offsets[i], lengths[i]));
          model.addGroup(group);
        }
      }
      if (withParentheses) {
        LinkedPositionGroup group = new LinkedPositionGroup();
        group.addPosition(new LinkedPosition(document, replacementOffset + getCursorPosition(), 0));
        model.addGroup(group);
      }

      model.forceInstall();
      DartEditor editor = getDartEditor();
      if (editor != null) {
        model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
      }

      LinkedModeUI ui = new EditorLinkedModeUI(model, getTextViewer());
      ui.setExitPolicy(new ExitPolicy(withParentheses ? ')' : '>', document));
      ui.setExitPosition(getTextViewer(), replacementOffset + replacementString.length(), 0,
          Integer.MAX_VALUE);
      ui.setDoContextInfo(true);
      ui.enter();

      fSelectedRegion = ui.getSelectedRegion();

    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
      openErrorDialog(e);
    }
  }

  /**
   * For the type parameter at <code>paramIndex</code> in the type at <code>path[pathIndex]</code> ,
   * this method computes the corresponding type parameter index in the type at <code>path[0]</code>
   * . If the type parameter does not map to a type parameter of the super type, <code>-1</code> is
   * returned.
   * 
   * @param path the type inheritance path, a non-empty array of consecutive sub types
   * @param pathIndex an index into <code>path</code> specifying the type to start with
   * @param paramIndex the index of the type parameter to map - <code>path[pathIndex]</code> must
   *          have a type parameter at that index, lest an
   *          <code>ArrayIndexOutOfBoundsException</code> is thrown
   * @return the index of the type parameter in <code>path[0]</code> corresponding to the type
   *         parameter at <code>paramIndex</code> in <code>path[pathIndex]</code>, or -1 if there is
   *         no corresponding type parameter
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   * @throws ArrayIndexOutOfBoundsException if <code>path[pathIndex]</code> has &lt;=
   *           <code>paramIndex</code> parameters
   */
  @SuppressWarnings("unused")
  private int mapTypeParameterIndex(Type[] path, int pathIndex, int paramIndex)
      throws DartModelException, ArrayIndexOutOfBoundsException {
    if (pathIndex == 0) {
      // break condition: we've reached the top of the hierarchy
      return paramIndex;
    }

    Type subType = path[pathIndex];
    Type superType = path[pathIndex - 1];

    String superSignature = findMatchingSuperTypeSignature(subType, superType);
//    TypeParameter param = subType.getTypeParameters()[paramIndex];
//    int index = findMatchingTypeArgumentIndex(superSignature, param.getElementName());
    int index = -1;
    if (index == -1) {
      // not mapped through
      return -1;
    }

    return mapTypeParameterIndex(path, pathIndex - 1, index);
  }

  private void openErrorDialog(BadLocationException e) {
    Shell shell = getTextViewer().getTextWidget().getShell();
    MessageDialog.openError(shell, DartTextMessages.FilledArgumentNamesMethodProposal_error_msg,
        e.getMessage());
  }

  /**
   * Returns <code>true</code> if type arguments should be appended when applying this proposal,
   * <code>false</code> if not (for example if the document already contains a type argument list
   * after the insertion point.
   * 
   * @param document the document
   * @param offset the insertion offset
   * @param trigger the trigger character
   * @return <code>true</code> if arguments should be appended
   */
  private boolean shouldAppendArguments(IDocument document, int offset, char trigger) {
    /*
     * No argument list if there were any special triggers (for example a period to qualify an inner
     * type).
     */
    if (trigger != '\0' && trigger != '<' && trigger != '(') {
      return false;
    }

    /* No argument list if the completion is empty (already within the argument list). */
    char[] completion = fProposal.getCompletion();
    if (completion.length == 0) {
      return false;
    }

    /* No argument list if there already is a generic signature behind the name. */
    try {
      IRegion region = document.getLineInformationOfOffset(offset);
      String line = document.get(region.getOffset(), region.getLength());

      int index = offset - region.getOffset();
      while (index != line.length() && Character.isUnicodeIdentifierPart(line.charAt(index))) {
        ++index;
      }

      if (index == line.length()) {
        return true;
      }

      char ch = line.charAt(index);
      return ch != '<';

    } catch (BadLocationException e) {
      return true;
    }
  }
}
