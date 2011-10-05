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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;

/**
 * From the Dart Guide:
 * <p>
 * A function overrides a function in a superclass if the function in the superclass has the same
 * name.
 * <p>
 * It is an error if the number of arguments is different between a function and the overriden
 * function, or if either function is static. Default arguments and variable arguments must also
 * match, or it is an error.
 */
public class OverrideCompletionProposal extends DartTypeCompletionProposal implements
    ICompletionProposalExtension4 {

  private DartProject fJavaProject;
  private String fMethodName;
  private String[] fParamTypes;

  public OverrideCompletionProposal(DartProject jproject, CompilationUnit cu, String methodName,
      String[] paramTypes, int start, int length, String displayName, String completionProposal) {
    super(completionProposal, cu, start, length, null, displayName, 0);
    Assert.isNotNull(jproject);
    Assert.isNotNull(methodName);
    Assert.isNotNull(paramTypes);
    Assert.isNotNull(cu);

    fParamTypes = paramTypes;
    fMethodName = methodName;

    fJavaProject = jproject;

    StringBuffer buffer = new StringBuffer();
    buffer.append(completionProposal);
    buffer.append(" {};"); //$NON-NLS-1$

    setReplacementString(buffer.toString());
  }

  /*
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#
   * getPrefixCompletionText(org.eclipse.jface.text.IDocument,int)
   */
  @Override
  public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
    return fMethodName;
  }

  /*
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension4# isAutoInsertable()
   */
  @Override
  public boolean isAutoInsertable() {
    return false;
  }

  /*
   * @see DartTypeCompletionProposal#updateReplacementString(IDocument,char,int, ImportRewrite)
   */
  @Override
  protected boolean updateReplacementString(IDocument document, char trigger, int offset,
      ImportRewrite importRewrite) throws CoreException, BadLocationException {

    //TODO (pquitslund): implement updateReplacementString for Overrrides
    return super.updateReplacementString(document, trigger, offset, importRewrite);

//    final IDocument buffer = new Document(document.get());
//    int index = offset - 1;
//    while (index >= 0 && Character.isJavaIdentifierPart(buffer.getChar(index)))
//      index--;
//    final int length = offset - index - 1;
//    buffer.replace(index + 1, length, " "); //$NON-NLS-1$
////    final ASTParser parser = ASTParser.newParser(AST.JLS3);
////    parser.setResolveBindings(true);
////    parser.setStatementsRecovery(true);
////    parser.setSource(buffer.get().toCharArray());
////    parser.setUnitName(fCompilationUnit.getResource().getFullPath().toString());
////    parser.setProject(fCompilationUnit.getDartProject());
////    final DartUnit unit = (DartUnit) parser.createAST(new NullProgressMonitor());
//    final DartUnit unit = DartCompilerUtilities.resolveUnit(fCompilationUnit);
//    ClassElement binding = null;
//    ChildListPropertyDescriptor descriptor = null;
//    DartNode node = NodeFinder.perform(unit, index + 1, 0);
////    if (node instanceof AnonymousClassDeclaration) {
////      switch (node.getParent().getNodeType()) {
////        case DartNode.CLASS_INSTANCE_CREATION:
////          binding = ((ClassInstanceCreation) node.getParent()).resolveTypeBinding();
////          break;
////      }
////      descriptor = AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;
////    } else
//    if (node instanceof DartClass) {
//      final DartClass declaration = ((DartClass) node);
//      descriptor = (ChildListPropertyDescriptor) PropertyDescriptorHelper.DART_CLASS_MEMBERS;
//      binding = declaration.getSymbol();
//    }
//    if (binding != null) {
//      ASTRewrite rewrite = ASTRewrite.create(new AST());
//      IFunctionBinding[] bindings = StubUtility2.getOverridableMethods(
//          rewrite.getAST(), binding, true);
//      if (bindings != null && bindings.length > 0) {
//        List<IFunctionBinding> candidates = new ArrayList<IFunctionBinding>(bindings.length);
//        IFunctionBinding method = null;
//        for (index = 0; index < bindings.length; index++) {
//          if (bindings[index].getName().equals(fMethodName)
//              && bindings[index].getParameterTypes().length == fParamTypes.length)
//            candidates.add(bindings[index]);
//        }
//        if (candidates.size() > 1) {
//          method = Bindings.findMethodInHierarchy(binding, fMethodName,
//              fParamTypes);
//          if (method == null) {
//            ITypeBinding objectType = rewrite.getAST().resolveWellKnownType(
//                "java.lang.Object"); //$NON-NLS-1$
//            method = Bindings.findMethodInType(objectType, fMethodName,
//                fParamTypes);
//          }
//        } else if (candidates.size() == 1)
//          method = (IFunctionBinding) candidates.get(0);
//        if (method != null) {
//          CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings(fJavaProject);
//          ListRewrite rewriter = rewrite.getListRewrite(node, descriptor);
//          String key = method.getKey();
//          FunctionDeclaration stub = null;
//          for (index = 0; index < bindings.length; index++) {
//            if (key.equals(bindings[index].getKey())) {
//              stub = StubUtility2.createImplementationStub(fCompilationUnit,
//                  rewrite, importRewrite, bindings[index], binding.getName(),
//                  false, settings);
//              if (stub != null)
//                rewriter.insertFirst(stub, null);
//              break;
//            }
//          }
//          if (stub != null) {
//            IDocument contents = new Document(
//                fCompilationUnit.getBuffer().getContents());
//            IRegion region = contents.getLineInformationOfOffset(getReplacementOffset());
//            ITrackedNodePosition position = rewrite.track(stub);
//            String indent = IndentManipulation.extractIndentString(
//                contents.get(region.getOffset(), region.getLength()),
//                settings.tabWidth, settings.indentWidth);
//            try {
//              rewrite.rewriteAST(contents, fJavaProject.getOptions(true)).apply(
//                  contents, TextEdit.UPDATE_REGIONS);
//            } catch (MalformedTreeException exception) {
//              DartToolsPlugin.log(exception);
//            } catch (BadLocationException exception) {
//              DartToolsPlugin.log(exception);
//            }
//            setReplacementString(IndentManipulation.changeIndent(
//                Strings.trimIndentation(
//                    contents.get(position.getStartPosition(),
//                        position.getLength()), settings.tabWidth,
//                    settings.indentWidth, false), 0, settings.tabWidth,
//                settings.indentWidth, indent,
//                TextUtilities.getDefaultLineDelimiter(contents)));
//          }
//        }
//      }
//    }
//    return true;
  }
}
