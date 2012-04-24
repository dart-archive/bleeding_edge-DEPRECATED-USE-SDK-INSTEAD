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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.viewers.StyledString;

public class OverrideCompletionProposal extends DartTypeCompletionProposal implements
    ICompletionProposalExtension4 {

//  private static String getIndentAt(IDocument document, int offset, CodeGenerationSettings settings) {
//    try {
//      IRegion region = document.getLineInformationOfOffset(offset);
//      return IndentManipulation.extractIndentString(
//          document.get(region.getOffset(), region.getLength()), settings.tabWidth,
//          settings.indentWidth);
//    } catch (BadLocationException e) {
//      return ""; //$NON-NLS-1$
//    }
//  }

  @SuppressWarnings("unused")
  private DartProject fJavaProject;
  private String fMethodName;

  @SuppressWarnings("unused")
  private String[] fParamTypes;

  public OverrideCompletionProposal(DartProject jproject, CompilationUnit cu, String methodName,
      String[] paramTypes, int start, int length, StyledString displayName,
      String completionProposal) {
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

  @Override
  public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
    return fMethodName;
  }

  @Override
  public boolean isAutoInsertable() {
    return false;
  }

  @Override
  protected boolean updateReplacementString(IDocument document, char trigger, int offset,
      ImportRewrite importRewrite) throws CoreException, BadLocationException {
//    Document recoveredDocument = new Document();
//    CompilationUnit unit = getRecoveredAST(document, offset, recoveredDocument);
//    ImportRewriteContext context;
//    if (importRewrite != null) {
//      context = new ContextSensitiveImportRewriteContext(unit, offset, importRewrite);
//    } else {
//      importRewrite = StubUtility.createImportRewrite(unit, true); // create a dummy import rewriter to have one
//      context = new ImportRewriteContext() { // forces that all imports are fully qualified
//        @Override
//        public int findInContext(String qualifier, String name, int kind) {
//          return RES_NAME_CONFLICT;
//        }
//      };
//    }
//
//    ITypeBinding declaringType = null;
//    ChildListPropertyDescriptor descriptor = null;
//    ASTNode node = NodeFinder.perform(unit, offset, 1);
//    if (node instanceof AnonymousClassDeclaration) {
//      declaringType = ((AnonymousClassDeclaration) node).resolveBinding();
//      descriptor = AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;
//    } else if (node instanceof AbstractTypeDeclaration) {
//      AbstractTypeDeclaration declaration = (AbstractTypeDeclaration) node;
//      descriptor = declaration.getBodyDeclarationsProperty();
//      declaringType = declaration.resolveBinding();
//    }
//    if (declaringType != null) {
//      ASTRewrite rewrite = ASTRewrite.create(unit.getAST());
//      IMethodBinding methodToOverride = Bindings.findMethodInHierarchy(declaringType, fMethodName,
//          fParamTypes);
//      if (methodToOverride == null && declaringType.isInterface()) {
//        methodToOverride = Bindings.findMethodInType(
//            node.getAST().resolveWellKnownType("java.lang.Object"), fMethodName, fParamTypes); //$NON-NLS-1$
//      }
//      if (methodToOverride != null) {
//        CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings(fJavaProject);
//        MethodDeclaration stub = StubUtility2.createImplementationStub(fCompilationUnit, rewrite,
//            importRewrite, context, methodToOverride, declaringType.getName(), settings,
//            declaringType.isInterface());
//        ListRewrite rewriter = rewrite.getListRewrite(node, descriptor);
//        rewriter.insertFirst(stub, null);
//
//        ITrackedNodePosition position = rewrite.track(stub);
//        try {
//          rewrite.rewriteAST(recoveredDocument, fJavaProject.getOptions(true)).apply(
//              recoveredDocument);
//
//          String generatedCode = recoveredDocument.get(position.getStartPosition(),
//              position.getLength());
//          int generatedIndent = IndentManipulation.measureIndentUnits(
//              getIndentAt(recoveredDocument, position.getStartPosition(), settings),
//              settings.tabWidth, settings.indentWidth);
//
//          String indent = getIndentAt(document, getReplacementOffset(), settings);
//          setReplacementString(IndentManipulation.changeIndent(generatedCode, generatedIndent,
//              settings.tabWidth, settings.indentWidth, indent,
//              TextUtilities.getDefaultLineDelimiter(document)));
//
//        } catch (MalformedTreeException exception) {
//          JavaPlugin.log(exception);
//        } catch (BadLocationException exception) {
//          JavaPlugin.log(exception);
//        }
//      }
//    }
    return true;
  }

//  private CompilationUnit getRecoveredAST(IDocument document, int offset, Document recoveredDocument) {
//    CompilationUnit ast = SharedASTProvider.getAST(fCompilationUnit,
//        SharedASTProvider.WAIT_ACTIVE_ONLY, null);
//    if (ast != null) {
//      recoveredDocument.set(document.get());
//      return ast;
//    }
//
//    char[] content = document.get().toCharArray();
//
//    // clear prefix to avoid compile errors
//    int index = offset - 1;
//    while (index >= 0 && Character.isJavaIdentifierPart(content[index])) {
//      content[index] = ' ';
//      index--;
//    }
//
//    recoveredDocument.set(new String(content));
//
//    final ASTParser parser = ASTParser.newParser(AST.JLS3);
//    parser.setResolveBindings(true);
//    parser.setStatementsRecovery(true);
//    parser.setSource(content);
//    parser.setUnitName(fCompilationUnit.getElementName());
//    parser.setProject(fCompilationUnit.getJavaProject());
//    return (CompilationUnit) parser.createAST(new NullProgressMonitor());
//  }
}
