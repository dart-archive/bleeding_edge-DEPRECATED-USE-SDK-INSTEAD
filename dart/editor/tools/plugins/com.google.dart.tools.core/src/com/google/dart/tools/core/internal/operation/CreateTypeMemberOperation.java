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
package com.google.dart.tools.core.internal.operation;

import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dom.PropertyDescriptorHelper;
import com.google.dart.tools.core.dom.StructuralPropertyDescriptor;
import com.google.dart.tools.core.dom.rewrite.ASTRewrite;
import com.google.dart.tools.core.formatter.IndentManipulation;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.util.Util;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatus;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import java.util.List;
import java.util.Map;

/**
 * The abstract class <code>CreateTypeMemberOperation</code> implements functionality common to
 * operations that create type members.
 */
public abstract class CreateTypeMemberOperation extends CreateElementInCUOperation {
  /**
   * The source code for the new member.
   */
  protected String source = null;

  /**
   * The name of the <code>DartNode</code> that may be used to create this new element. Used by the
   * <code>CopyElementsOperation</code> for renaming.
   */
  protected String alteredName;

  /**
   * The AST node representing the element that this operation created.
   */
  protected DartNode createdNode;

  /**
   * When executed, this operation will create a type member in the given parent element with the
   * specified source.
   */
  public CreateTypeMemberOperation(DartElement parentElement, String source, boolean force) {
    super(parentElement);
    this.source = source;
    this.force = force;
  }

  /**
   * Possible failures:
   * <ul>
   * <li>NO_ELEMENTS_TO_PROCESS - the parent element supplied to the operation is <code>null</code>.
   * <li>INVALID_CONTENTS - The source is <code>null</code> or has serious syntax errors.
   * <li>NAME_COLLISION - A name collision occurred in the destination
   * </ul>
   */
  @Override
  public DartModelStatus verify() {
    DartModelStatus status = super.verify();
    if (!status.isOK()) {
      return status;
    }
    if (source == null) {
      return new DartModelStatusImpl(DartModelStatusConstants.INVALID_CONTENTS);
    }
    if (!force) {
      // check for name collisions
      try {
        CompilationUnit cu = getCompilationUnit();
        generateElementAST(null, cu);
      } catch (DartModelException jme) {
        return jme.getDartModelStatus();
      }
      return verifyNameCollision();
    }

    return DartModelStatusImpl.VERIFIED_OK;
  }

  protected DartNode generateElementAST(ASTRewrite rewriter, CompilationUnit cu)
      throws DartModelException {
    // if (createdNode == null) {
    // source = removeIndentAndNewLines(source, cu);
    // ASTParser parser = ASTParser.newParser(AST.JLS3);
    // parser.setSource(source.toCharArray());
    // parser.setProject(getCompilationUnit().getDartProject());
    // parser.setKind(ASTParser.K_CLASS_BODY_DECLARATIONS);
    // DartNode node = parser.createAST(progressMonitor);
    // String createdNodeSource;
    // if (node.getNodeType() != DartNode.TYPE_DECLARATION) {
    // createdNodeSource = generateSyntaxIncorrectAST();
    // if (createdNode == null)
    // throw new DartModelException(new DartModelStatusImpl(
    // DartModelStatusConstants.INVALID_CONTENTS));
    // } else {
    // DartClass typeDeclaration = (DartClass) node;
    // if ((typeDeclaration.getFlags() & DartNode.MALFORMED) != 0) {
    // createdNodeSource = generateSyntaxIncorrectAST();
    // if (createdNode == null)
    // throw new DartModelException(new DartModelStatusImpl(
    // DartModelStatusConstants.INVALID_CONTENTS));
    // } else {
    // List bodyDeclarations = typeDeclaration.bodyDeclarations();
    // if (bodyDeclarations.size() == 0) {
    // throw new DartModelException(new DartModelStatusImpl(
    // DartModelStatusConstants.INVALID_CONTENTS));
    // }
    // createdNode = (DartNode) bodyDeclarations.iterator().next();
    // createdNodeSource = source;
    // }
    // }
    // if (alteredName != null) {
    // SimpleName newName = createdNode.getAST().newSimpleName(alteredName);
    // SimpleName oldName = rename(createdNode, newName);
    // int nameStart = oldName.getStartPosition();
    // int nameEnd = nameStart + oldName.getLength();
    // StringBuffer newSource = new StringBuffer();
    // if (source.equals(createdNodeSource)) {
    // newSource.append(createdNodeSource.substring(0, nameStart));
    // newSource.append(alteredName);
    // newSource.append(createdNodeSource.substring(nameEnd));
    // } else {
    // // syntactically incorrect source
    // int createdNodeStart = createdNode.getStartPosition();
    // int createdNodeEnd = createdNodeStart + createdNode.getLength();
    // newSource.append(createdNodeSource.substring(createdNodeStart,
    // nameStart));
    // newSource.append(alteredName);
    // newSource.append(createdNodeSource.substring(nameEnd, createdNodeEnd));
    //
    // }
    // source = newSource.toString();
    // }
    // }
    // if (rewriter == null)
    // return createdNode;
    // // return a string place holder (instead of the created node) so has to
    // // not lose comments and formatting
    // return rewriter.createStringPlaceholder(source,
    // createdNode.getNodeType());
    DartCore.notYetImplemented();
    return null;
  }

  /**
   * Generate a <code>DartNode</code> based on the source of this operation when there is likely a
   * syntax error in the source. Return the source used to generate this node.
   */
  protected String generateSyntaxIncorrectAST() {
    // create some dummy source to generate an ast node
    StringBuffer buff = new StringBuffer();
    Type type = getType();
    String lineSeparator = Util.getLineSeparator(
        source,
        type == null ? null : type.getDartProject());
    buff.append(lineSeparator + " public class A {" + lineSeparator); //$NON-NLS-1$
    buff.append(source);
    buff.append(lineSeparator).append('}');
    try {
      DartUnit compilationUnit = DartCompilerUtilities.parseSource("A.dart", buff.toString());
      DartClass typeDeclaration = (DartClass) compilationUnit.getTopLevelNodes().get(0);
      List<DartNode> bodyDeclarations = typeDeclaration.getMembers();
      if (bodyDeclarations.size() != 0) {
        createdNode = bodyDeclarations.get(0);
      }
    } catch (DartModelException exception) {
      // If we can't parse the source, then we can't get the created node.
    }
    return buff.toString();
  }

  protected StructuralPropertyDescriptor getChildPropertyDescriptor(DartNode parent) {
    if (parent instanceof DartUnit) {
      return PropertyDescriptorHelper.DART_UNIT_MEMBERS;
    }
    return PropertyDescriptorHelper.DART_CLASS_MEMBERS;
  }

  /**
   * Returns the IType the member is to be created in.
   */
  protected Type getType() {
    return (Type) getParentElement();
  }

  /**
   * Rename the given node to the given name, returning the old name.
   * 
   * @return the old name
   */
  protected abstract DartIdentifier rename(DartNode node, DartIdentifier newName);

  /**
   * Sets the name of the <code>DartNode</code> that will be used to create this new element. Used
   * by the <code>CopyElementsOperation</code> for renaming
   */
  @Override
  protected void setAlteredName(String newName) {
    alteredName = newName;
  }

  /**
   * Verify for a name collision in the destination container.
   */
  protected DartModelStatus verifyNameCollision() {
    return DartModelStatusImpl.VERIFIED_OK;
  }

  private String removeIndentAndNewLines(String code, CompilationUnit cu) throws DartModelException {
    DartProject project = cu.getDartProject();
    Map<String, String> options = project.getOptions(true);
    int tabWidth = IndentManipulation.getTabWidth(options);
    int indentWidth = IndentManipulation.getIndentWidth(options);
    int indent = IndentManipulation.measureIndentUnits(code, tabWidth, indentWidth);
    int firstNonWhiteSpace = -1;
    int length = code.length();
    DartCore.notYetImplemented();
    // while (firstNonWhiteSpace < length - 1)
    // if (!ScannerHelper.isWhitespace(code.charAt(++firstNonWhiteSpace)))
    // break;
    int lastNonWhiteSpace = length;
    DartCore.notYetImplemented();
    // while (lastNonWhiteSpace > 0)
    // if (!ScannerHelper.isWhitespace(code.charAt(--lastNonWhiteSpace)))
    // break;
    String lineDelimiter = cu.findRecommendedLineSeparator();
    return IndentManipulation.changeIndent(
        code.substring(firstNonWhiteSpace, lastNonWhiteSpace + 1),
        indent,
        tabWidth,
        indentWidth,
        "", lineDelimiter); //$NON-NLS-1$
  }
}
