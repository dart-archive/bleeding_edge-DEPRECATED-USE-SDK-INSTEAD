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
package com.google.dart.tools.core.dom.rewrite;

import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartComment;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dom.AST;
import com.google.dart.tools.core.dom.ChildListPropertyDescriptor;
import com.google.dart.tools.core.dom.PropertyDescriptorHelper;
import com.google.dart.tools.core.dom.StructuralPropertyDescriptor;
import com.google.dart.tools.core.internal.compiler.parser.RecoveryScannerData;
import com.google.dart.tools.core.internal.dom.rewrite.ASTRewriteAnalyzer;
import com.google.dart.tools.core.internal.dom.rewrite.LineInformation;
import com.google.dart.tools.core.internal.dom.rewrite.NodeInfoStore;
import com.google.dart.tools.core.internal.dom.rewrite.NodeRewriteEvent;
import com.google.dart.tools.core.internal.dom.rewrite.RewriteEventStore;
import com.google.dart.tools.core.internal.dom.rewrite.RewriteEventStore.CopySourceInfo;
import com.google.dart.tools.core.internal.dom.rewrite.RewriteEventStore.PropertyLocation;
import com.google.dart.tools.core.internal.dom.rewrite.TrackedNodePositionImpl;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class <code>ASTRewrite</code> implement an infrastructure for modifying code by
 * describing changes to AST nodes. The AST rewriter collects descriptions of modifications to nodes
 * and translates these descriptions into text edits that can then be applied to the original
 * source. The key thing is that this is all done without actually modifying the original AST, which
 * has the virtue of allowing one to entertain several alternate sets of changes on the same AST
 * (e.g., for calculating quick fix proposals). The rewrite infrastructure tries to generate minimal
 * text changes, preserve existing comments and indentation, and follow code formatter settings. If
 * the freedom to explore multiple alternate changes is not required, consider using the AST's
 * built-in rewriter (see {@link DartUnit#rewrite(IDocument, Map)}).
 * <p>
 * The following code snippet illustrated usage of this class:
 * 
 * <pre>
 * Document document = new Document("import java.util.List;\nclass X {}\n");
 * ASTParser parser = ASTParser.newParser(AST.JLS3);
 * parser.setSource(document.get().toCharArray());
 * CompilationUnit cu = (CompilationUnit) parser.createAST(null);
 * AST ast = cu.getAST();
 * ImportDeclaration id = ast.newImportDeclaration();
 * id.setName(ast.newName(new String[] {"java", "util", "Set"}));
 * ASTRewrite rewriter = ASTRewrite.create(ast);
 * TypeDeclaration td = (TypeDeclaration) cu.types().get(0);
 * ITrackedNodePosition tdLocation = rewriter.track(td);
 * ListRewrite lrw = rewriter.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY);
 * lrw.insertLast(id, null);
 * TextEdit edits = rewriter.rewriteAST(document, null);
 * UndoEdit undo = null;
 * try {
 *     undo = edits.apply(document);
 * } catch(MalformedTreeException e) {
 *     e.printStackTrace();
 * } catch(BadLocationException e) {
 *     e.printStackTrace();
 * }
 * assert "import java.util.List;\nimport java.util.Set;\nclass X {}\n".equals(document.get());
 * // tdLocation.getStartPosition() and tdLocation.getLength()
 * // are new source range for "class X {}" in document.get()
 * </pre>
 */
public class ASTRewrite {
  /**
   * Creates a new instance for describing manipulations of the given AST.
   * 
   * @param ast the AST whose nodes will be rewritten
   * @return the new rewriter instance
   */
  public static ASTRewrite create(AST ast) {
    return new ASTRewrite(ast);
  }

  /**
   * The root node for the rewrite: Only nodes under this root are accepted.
   */
  private final AST ast;

  private final RewriteEventStore eventStore;

  private final NodeInfoStore nodeStore;

  /**
   * Target source range computer; <code>null</code> means uninitialized; lazy initialized to
   * <code>new TargetSourceRangeComputer()</code>.
   */
  private TargetSourceRangeComputer targetSourceRangeComputer = null;

  /**
   * Primary field used in representing rewrite properties efficiently. If <code>null</code>, this
   * rewrite has no properties. If a {@link String}, this is the name of this rewrite's sole
   * property, and <code>property2</code> contains its value. If a {@link Map}, this is the table of
   * property name-value mappings. Initially <code>null</code>.
   * 
   * @see #property2
   */
  private Object property1 = null;

  /**
   * Auxiliary field used in representing rewrite properties efficiently.
   * 
   * @see #property1
   */
  private Object property2 = null;

  /**
   * Internal constructor. Creates a new instance for the given AST. Clients should use
   * {@link #create(AST)} to create instances.
   * 
   * @param ast the AST being rewritten
   */
  protected ASTRewrite(AST ast) {
    this.ast = ast;
    eventStore = new RewriteEventStore();
    nodeStore = new NodeInfoStore(ast);
  }

  /**
   * Create and return a placeholder node for a true copy of the given node. The placeholder node
   * can either be inserted as new or used to replace an existing node. When the document is
   * rewritten, a copy of the source code for the given node is inserted into the output document at
   * the position corresponding to the placeholder (indentation is adjusted).
   * 
   * @param node the node to create a copy placeholder for
   * @return the new placeholder node
   * @throws IllegalArgumentException if the node is null, or if the node is not part of this
   *           rewriter's AST
   */
  public final DartNode createCopyTarget(DartNode node) {
    return createTargetNode(node, false);
  }

  /**
   * Create and return a node that represents a sequence of nodes. Each of the given nodes must be
   * either be brand new (not part of the original AST), or a placeholder node (for example, one
   * created by {@link #createCopyTarget(DartNode)} or {@link #createStringPlaceholder(String, int)}
   * ), or another group node. The type of the returned node is unspecified. The returned node can
   * be used to replace an existing node (or as an element of another group node). When the document
   * is rewritten, the source code for each of the given nodes is inserted, in order, into the
   * output document at the position corresponding to the group (indentation is adjusted).
   * 
   * @param targetNodes the nodes to go in the group
   * @return the new group node
   * @throws IllegalArgumentException if the targetNodes is <code>null</code> or empty
   */
  public final DartNode createGroupNode(DartNode[] targetNodes) {
    if (targetNodes == null || targetNodes.length == 0) {
      throw new IllegalArgumentException();
    }
    DartBlock res = getNodeStore().createCollapsePlaceholder();
    DartCore.notYetImplemented();
    ListRewrite listRewrite = getListRewrite(
        res,
        (ChildListPropertyDescriptor) PropertyDescriptorHelper.DART_BLOCK_STATEMENTS);
    for (int i = 0; i < targetNodes.length; i++) {
      listRewrite.insertLast(targetNodes[i], null);
    }
    return res;
  }

  /**
   * Create and return a placeholder node for the new locations of the given node. After obtaining a
   * placeholder, the node should then to be removed or replaced. The placeholder node can either be
   * inserted as new or used to replace an existing node. When the document is rewritten, the source
   * code for the given node is inserted into the output document at the position corresponding to
   * the placeholder (indentation is adjusted).
   * 
   * @param node the node to create a move placeholder for
   * @return the new placeholder node
   * @throws IllegalArgumentException if the node is null, or if the node is not part of this
   *           rewriter's AST
   */
  public final DartNode createMoveTarget(DartNode node) {
    return createTargetNode(node, true);
  }

  /**
   * Create and return a placeholder node for a source string that is to be inserted into the output
   * document at the position corresponding to the placeholder. The string will be inserted without
   * being reformatted beyond correcting the indentation level. The placeholder node can either be
   * inserted as new or used to replace an existing node.
   * 
   * @param code the string to be inserted; lines should should not have extra indentation
   * @param nodeClass the DartNode class that corresponds to the passed code
   * @return the new placeholder node
   * @throws IllegalArgumentException if the code is null, or if the node type is invalid
   */
  public final DartNode createStringPlaceholder(String code, Class<? extends DartNode> nodeClass) {
    if (code == null || nodeClass == null) {
      throw new IllegalArgumentException();
    }
    DartNode placeholder = getNodeStore().newPlaceholderNode(nodeClass);
    if (placeholder == null) {
      throw new IllegalArgumentException(
          "String placeholder is not supported for type " + nodeClass.getName()); //$NON-NLS-1$
    }

    getNodeStore().markAsStringPlaceholder(placeholder, code);
    return placeholder;
  }

  /**
   * Return the value of the given property as managed by this rewriter. If the property has been
   * removed, <code>null</code> is returned. If it has been replaced, the replacing value is
   * returned. If the property has not been changed yet, the original value is returned.
   * <p>
   * For child list properties use {@link ListRewrite#getRewrittenList()} to get access to the
   * rewritten nodes in a list.
   * 
   * @param node the node
   * @param property the node's property
   * @return the value of the given property as managed by this rewriter
   */
  public Object get(DartNode node, StructuralPropertyDescriptor property) {
    if (node == null || property == null) {
      throw new IllegalArgumentException();
    }
    if (property.isChildListProperty()) {
      throw new IllegalArgumentException("Use the list rewriter to access nodes in a list"); //$NON-NLS-1$
    }
    return eventStore.getNewValue(node, property);
  }

  /**
   * Return the AST the rewrite was set up on.
   * 
   * @return the AST the rewrite was set up on
   */
  public final AST getAST() {
    return ast;
  }

  /**
   * Return the extended source range computer for this AST rewriter. The default value is a
   * <code>new TargetSourceRangeComputer()</code>.
   * 
   * @return an extended source range computer
   * @see #setTargetSourceRangeComputer(TargetSourceRangeComputer)
   */
  public final TargetSourceRangeComputer getExtendedSourceRangeComputer() {
    if (targetSourceRangeComputer == null) {
      // lazy initialize
      targetSourceRangeComputer = new TargetSourceRangeComputer();
    }
    return targetSourceRangeComputer;
  }

  /*
   * private void validateASTNotModified(DartNode root) throws IllegalArgumentException {
   * GenericVisitor isModifiedVisitor= new GenericVisitor() { protected boolean visitNode(DartNode
   * node) { if ((node.getFlags() & DartNode.ORIGINAL) == 0) { throw new IllegalArgumentException
   * ("The AST that is rewritten must not be modified."); //$NON-NLS-1$ } return true; } };
   * root.accept(isModifiedVisitor); }
   */

  /**
   * Create and return a new rewriter for describing modifications to the given list property of the
   * given node.
   * 
   * @param node the node
   * @param property the node's property; the child list property
   * @return a new list rewriter object
   * @throws IllegalArgumentException if the node or property is null, or if the node is not part of
   *           this rewriter's AST, or if the property is not a node property, or if the described
   *           modification is invalid
   */
  public final ListRewrite getListRewrite(DartNode node, ChildListPropertyDescriptor property) {
    if (node == null || property == null) {
      throw new IllegalArgumentException();
    }

    validateIsCorrectAST(node);
    validateIsListProperty(property);
    validateIsPropertyOfNode(property, node);

    return new ListRewrite(this, node, property);
  }

  /**
   * Return the value of the named property of this rewrite, or <code>null</code> if none.
   * 
   * @param propertyName the property name
   * @return the property value, or <code>null</code> if none
   * @throws IllegalArgumentException if the given property name is <code>null</code>
   * @see #setProperty(String,Object)
   */
  @SuppressWarnings("unchecked")
  public final Object getProperty(String propertyName) {
    if (propertyName == null) {
      throw new IllegalArgumentException();
    }
    if (property1 == null) {
      // rewrite has no properties at all
      return null;
    }
    if (property1 instanceof String) {
      // rewrite has only a single property
      if (propertyName.equals(property1)) {
        return property2;
      } else {
        return null;
      }
    }
    // otherwise rewrite has table of properties
    Map<String, Object> m = (Map<String, Object>) property1;
    return m.get(propertyName);
  }

  /**
   * Remove the given node from its parent in this rewriter. The AST itself is not actually modified
   * in any way; rather, the rewriter just records a note that this node should not be there.
   * 
   * @param node the node being removed. The node can either be an original node in the AST or a new
   *          node already inserted or used as replacement in this AST rewriter.
   * @param editGroup the edit group in which to collect the corresponding text edits, or
   *          <code>null</code> if ungrouped
   * @throws IllegalArgumentException if the node is null, or if the node is not part of this
   *           rewriter's AST, or if the described modification is invalid (such as removing a
   *           required node)
   */
  public final void remove(DartNode node, TextEditGroup editGroup) {
    if (node == null) {
      throw new IllegalArgumentException();
    }

    StructuralPropertyDescriptor property;
    DartNode parent;
    if (RewriteEventStore.isNewNode(node)) { // remove a new node, bug 164862
      PropertyLocation location = eventStore.getPropertyLocation(node, RewriteEventStore.NEW);
      if (location != null) {
        property = location.getProperty();
        parent = location.getParent();
      } else {
        throw new IllegalArgumentException("Node is not part of the rewriter's AST"); //$NON-NLS-1$
      }
    } else {
      property = PropertyDescriptorHelper.getLocationInParent(node);
      parent = node.getParent();
    }

    if (property.isChildListProperty()) {
      getListRewrite(parent, (ChildListPropertyDescriptor) property).remove(node, editGroup);
    } else {
      set(parent, property, null, editGroup);
    }
  }

  /**
   * Replace the given node in this rewriter. The replacement node must either be brand new (not
   * part of the original AST) or a placeholder node (for example, one created by
   * {@link #createCopyTarget(DartNode)} or {@link #createStringPlaceholder(String, int)}). The AST
   * itself is not actually modified in any way; rather, the rewriter just records a note that this
   * node has been replaced.
   * 
   * @param node the node being replaced. The node can either be an original node in the AST or
   *          (since 3.4) a new node already inserted or used as replacement in this AST rewriter.
   * @param replacement the replacement node, or <code>null</code> if no replacement
   * @param editGroup the edit group in which to collect the corresponding text edits, or
   *          <code>null</code> if ungrouped
   * @throws IllegalArgumentException if the node is null, or if the node is not part of this
   *           rewriter's AST, or if the replacement node is not a new node (or placeholder), or if
   *           the described modification is otherwise invalid
   */
  public final void replace(DartNode node, DartNode replacement, TextEditGroup editGroup) {
    if (node == null) {
      throw new IllegalArgumentException();
    }

    StructuralPropertyDescriptor property;
    DartNode parent;
    if (RewriteEventStore.isNewNode(node)) { // replace a new node, bug 164862
      PropertyLocation location = eventStore.getPropertyLocation(node, RewriteEventStore.NEW);
      if (location != null) {
        property = location.getProperty();
        parent = location.getParent();
      } else {
        throw new IllegalArgumentException("Node is not part of the rewriter's AST"); //$NON-NLS-1$
      }
    } else {
      DartCore.notYetImplemented();
      property = PropertyDescriptorHelper.getLocationInParent(node);
      parent = node.getParent();
    }

    if (property.isChildListProperty()) {
      getListRewrite(parent, (ChildListPropertyDescriptor) property).replace(
          node,
          replacement,
          editGroup);
    } else {
      set(parent, property, replacement, editGroup);
    }
  }

  /**
   * Convert all modifications recorded by this rewriter into an object representing the
   * corresponding text edits to the source of a {@link CompilationUnit} from which the AST was
   * created from. The type root's source itself is not modified by this method call.
   * <p>
   * Important: This API can only be used if the modified AST has been created from a
   * {@link CompilationUnit} with source. That means {@link ASTParser#setSource(CompilationUnit)}
   * has been used when initializing the {@link ASTParser}. An {@link IllegalArgumentException} is
   * thrown otherwise. An {@link IllegalArgumentException} is also thrown when the type roots buffer
   * does not correspond anymore to the AST. Use {@link #rewriteAST(IDocument, Map)} for all ASTs
   * created from other content.
   * <p>
   * For nodes in the original that are being replaced or deleted, this rewriter computes the
   * adjusted source ranges by calling
   * {@link TargetSourceRangeComputer#computeSourceRange(DartNode)
   * getExtendedSourceRangeComputer().computeSourceRange(node)}.
   * <p>
   * Calling this methods does not discard the modifications on record. Subsequence modifications
   * are added to the ones already on record. If this method is called again later, the resulting
   * text edit object will accurately reflect the net cumulative effect of all those changes.
   * 
   * @return text edit object describing the changes to the document corresponding to the changes
   *         recorded by this rewriter
   * @throws DartModelException A {@link DartModelException} is thrown when the underlying
   *           compilation units buffer could not be accessed.
   * @throws IllegalArgumentException An {@link IllegalArgumentException} is thrown if the document
   *           passed does not correspond to the AST that is rewritten.
   */
  public TextEdit rewriteAST() throws DartModelException, IllegalArgumentException {
    DartNode rootNode = getRootNode();
    if (rootNode == null) {
      return new MultiTextEdit(); // no changes
    }

    DartNode root = rootNode.getRoot();
    if (!(root instanceof DartUnit)) {
      throw new IllegalArgumentException(
          "This API can only be used if the AST is created from a compilation unit"); //$NON-NLS-1$
    }
    DartUnit astRoot = (DartUnit) root;
    CompilationUnit typeRoot = getTypeRoot(astRoot);
    if (typeRoot == null || typeRoot.getBuffer() == null) {
      throw new IllegalArgumentException(
          "This API can only be used if the AST is created from a compilation unit"); //$NON-NLS-1$
    }

    char[] content = typeRoot.getBuffer().getCharacters();
    LineInformation lineInfo = LineInformation.create(astRoot);
    String lineDelim = typeRoot.findRecommendedLineSeparator();
    Map<String, String> options = typeRoot.getDartProject().getOptions(true);

    DartCore.notYetImplemented();
    return internalRewriteAST(
        content,
        lineInfo,
        lineDelim,
        astRoot.getComments(),
        options,
        rootNode,
        null);
    // Last argument was: (RecoveryScannerData)
    // astRoot.getStatementsRecoveryData());
  }

  /**
   * Convert all modifications recorded by this rewriter into an object representing the
   * corresponding text edits to the given document containing the original source code. The
   * document itself is not modified.
   * <p>
   * For nodes in the original that are being replaced or deleted, this rewriter computes the
   * adjusted source ranges by calling
   * {@link TargetSourceRangeComputer#computeSourceRange(DartNode)
   * getExtendedSourceRangeComputer().computeSourceRange(node)}.
   * <p>
   * Calling this methods does not discard the modifications on record. Subsequence modifications
   * are added to the ones already on record. If this method is called again later, the resulting
   * text edit object will accurately reflect the net cumulative effect of all those changes.
   * 
   * @param document original document containing source code
   * @param options the table of formatter options (key type: <code>String</code>; value type:
   *          <code>String</code>); or <code>null</code> to use the standard global options
   *          {@link DartCore#getOptions()}
   * @return text edit object describing the changes to the document corresponding to the changes
   *         recorded by this rewriter
   * @throws IllegalArgumentException An <code>IllegalArgumentException</code> is thrown if the
   *           document passed does not correspond to the AST that is rewritten.
   */
  public TextEdit rewriteAST(IDocument document, Map<String, String> options)
      throws IllegalArgumentException {
    if (document == null) {
      throw new IllegalArgumentException();
    }

    DartNode rootNode = getRootNode();
    if (rootNode == null) {
      return new MultiTextEdit(); // no changes
    }

    char[] content = document.get().toCharArray();
    LineInformation lineInfo = LineInformation.create(document);
    String lineDelim = TextUtilities.getDefaultLineDelimiter(document);

    DartNode astRoot = rootNode.getRoot();
    List<DartComment> commentNodes = astRoot instanceof DartUnit
        ? ((DartUnit) astRoot).getComments() : null;
    Map<String, String> currentOptions = options == null ? DartCore.getOptions() : options;
    DartCore.notYetImplemented();
    return internalRewriteAST(
        content,
        lineInfo,
        lineDelim,
        commentNodes,
        currentOptions,
        rootNode,
        null);
    // Last argument was: (RecoveryScannerData) ((CompilationUnit)
    // astRoot).getStatementsRecoveryData());
  }

  /**
   * Set the given property of the given node. If the given property is a child property, the value
   * must be a replacement node that is either be brand new (not part of the original AST) or a
   * placeholder node (for example, one created by {@link #createCopyTarget(DartNode)} or
   * {@link #createStringPlaceholder(String, int)}); or it must be <code>null</code>, indicating
   * that the child should be deleted. If the given property is a simple property, the value must be
   * the new value (primitive types must be boxed) or <code>null</code>. The AST itself is not
   * actually modified in any way; rather, the rewriter just records a note that this node has been
   * changed in the specified way.
   * 
   * @param node the node
   * @param property the node's property; either a simple property or a child property
   * @param value the replacement child or new value, or <code>null</code> if none
   * @param editGroup the edit group in which to collect the corresponding text edits, or
   *          <code>null</code> if ungrouped
   * @throws IllegalArgumentException if the node or property is null, or if the node is not part of
   *           this rewriter's AST, or if the property is not a node property, or if the described
   *           modification is invalid
   */
  public final void set(DartNode node, StructuralPropertyDescriptor property, Object value,
      TextEditGroup editGroup) {
    if (node == null || property == null) {
      throw new IllegalArgumentException();
    }
    validateIsCorrectAST(node);
    validatePropertyType(property, value);
    validateIsPropertyOfNode(property, node);

    NodeRewriteEvent nodeEvent = eventStore.getNodeEvent(node, property, true);
    nodeEvent.setNewValue(value);
    if (editGroup != null) {
      eventStore.setEventEditGroup(nodeEvent, editGroup);
    }
  }

  /**
   * Set the named property of this rewrite to the given value, or to <code>null</code> to clear it.
   * <p>
   * Clients should employ property names that are sufficiently unique to avoid inadvertent
   * conflicts with other clients that might also be setting properties on the same rewrite.
   * <p>
   * Note that modifying a property is not considered a modification to the AST itself. This is to
   * allow clients to decorate existing rewrites with their own properties without jeopardizing
   * certain things (like the validity of bindings), which rely on the underlying tree remaining
   * static.
   * 
   * @param propertyName the property name
   * @param data the new property value, or <code>null</code> if none
   * @throws IllegalArgumentException if the given property name is <code>null</code>
   * @see #getProperty(String)
   */
  @SuppressWarnings("unchecked")
  public final void setProperty(String propertyName, Object data) {
    if (propertyName == null) {
      throw new IllegalArgumentException();
    }
    if (property1 == null) {
      // rewrite has no properties at all
      if (data == null) {
        // rewrite already knows this
        return;
      }
      // rewrite gets its fist property
      property1 = propertyName;
      property2 = data;
      return;
    }
    if (property1 instanceof String) {
      // rewrite has only a single property
      if (propertyName.equals(property1)) {
        // we're in luck
        if (data == null) {
          // just delete last property
          property1 = null;
          property2 = null;
        } else {
          property2 = data;
        }
        return;
      }
      if (data == null) {
        // we already know this
        return;
      }
      // rewrite already has one property - getting its second
      // convert to more flexible representation
      Map<Object, Object> m = new HashMap<Object, Object>(3);
      m.put(property1, property2);
      m.put(propertyName, data);
      property1 = m;
      property2 = null;
      return;
    }
    // rewrite has two or more properties
    Map<Object, Object> m = (Map<Object, Object>) property1;
    if (data == null) {
      m.remove(propertyName);
      // check for just one property left
      if (m.size() == 1) {
        // convert to more efficient representation
        Map.Entry<Object, Object>[] entries = m.entrySet().toArray(new Map.Entry[1]);
        property1 = entries[0].getKey();
        property2 = entries[0].getValue();
      }
      return;
    } else {
      m.put(propertyName, data);
      // still has two or more properties
      return;
    }
  }

  /**
   * Set a custom target source range computer for this AST rewriter. This is advanced feature to
   * modify how comments are associated with nodes, which should be done only in special cases.
   * 
   * @param computer a target source range computer, or <code>null</code> to restore the default
   *          value of <code>new TargetSourceRangeComputer()</code>
   * @see #getExtendedSourceRangeComputer()
   */
  public final void setTargetSourceRangeComputer(TargetSourceRangeComputer computer) {
    // if computer==null, rely on lazy init code in
    // getExtendedSourceRangeComputer()
    targetSourceRangeComputer = computer;
  }

  /**
   * Return a string suitable for debugging purposes (only).
   * 
   * @return a debug string
   */
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("Events:\n"); //$NON-NLS-1$
    // be extra careful of uninitialized or mangled instances
    if (eventStore != null) {
      buf.append(eventStore.toString());
    }
    return buf.toString();
  }

  /**
   * Return an object that tracks the source range of the given node across the rewrite to its AST.
   * Upon return, the result object reflects the given node's current source range in the AST. After
   * <code>rewrite</code> is called, the result object is updated to reflect the given node's source
   * range in the rewritten AST.
   * 
   * @param node the node to track
   * @return an object that tracks the source range of <code>node</code>
   * @throws IllegalArgumentException if the node is null, or if the node is not part of this
   *           rewriter's AST, or if the node is already being tracked
   */
  public final TrackedNodePosition track(DartNode node) {
    if (node == null) {
      throw new IllegalArgumentException();
    }
    TextEditGroup group = eventStore.getTrackedNodeData(node);
    if (group == null) {
      group = new TextEditGroup("internal"); //$NON-NLS-1$
      eventStore.setTrackedNodeData(node, group);
    }
    return new TrackedNodePositionImpl(group, node);
  }

  /**
   * Internal method. Return the internal node info store. Clients should not use.
   * 
   * @return the internal info store. Clients should not use.
   */
  protected final NodeInfoStore getNodeStore() {
    return nodeStore;
  }

  /**
   * Internal method. Return the internal event store. Clients should not use.
   * 
   * @return the internal event store. Clients should not use.
   */
  protected final RewriteEventStore getRewriteEventStore() {
    return eventStore;
  }

  private DartNode createTargetNode(DartNode node, boolean isMove) {
    if (node == null) {
      throw new IllegalArgumentException();
    }
    validateIsExistingNode(node);
    validateIsCorrectAST(node);
    CopySourceInfo info = getRewriteEventStore().markAsCopySource(
        node.getParent(),
        PropertyDescriptorHelper.getLocationInParent(node),
        node,
        isMove);

    DartNode placeholder = getNodeStore().newPlaceholderNode(node.getClass());
    if (placeholder == null) {
      throw new IllegalArgumentException(
          "Creating a target node is not supported for nodes of type" + node.getClass().getName()); //$NON-NLS-1$
    }
    getNodeStore().markAsCopyTarget(placeholder, info);

    return placeholder;
  }

  private DartNode getRootNode() {
    DartNode node = null;
    int start = -1;
    int end = -1;

    for (Iterator<DartNode> iter = getRewriteEventStore().getChangeRootIterator(); iter.hasNext();) {
      DartNode curr = iter.next();
      SourceInfo currSourceInfo = curr.getSourceInfo();
      if (!RewriteEventStore.isNewNode(curr)) {
        int currStart = currSourceInfo.getOffset();
        int currEnd = currStart + currSourceInfo.getLength();
        if (node == null || currStart < start && currEnd > end) {
          start = currStart;
          end = currEnd;
          node = curr;
        } else if (currStart < start) {
          start = currStart;
        } else if (currEnd > end) {
          end = currEnd;
        }
      }
    }
    if (node != null) {
      SourceInfo nodeSource = node.getSourceInfo();
      int currStart = nodeSource.getOffset();
      int currEnd = currStart + nodeSource.getLength();
      // go up until a node covers all
      while (start < currStart || end > currEnd) {
        node = node.getParent();
        currStart = nodeSource.getOffset();
        currEnd = currStart + nodeSource.getLength();
      }
      // go up until a parent has different range
      DartNode parent = node.getParent();
      SourceInfo parentSource = parent.getSourceInfo();
      while (parent != null && parentSource.getOffset() == nodeSource.getOffset()
          && parentSource.getLength() == nodeSource.getLength()) {
        node = parent;
        parent = node.getParent();
      }
    }
    return node;
  }

  /**
   * Return the compilation unit from which the given AST structure was created.
   * 
   * @param astRoot the AST structure created from the compilation unit to be returned
   * @return the compilation unit from which the given AST structure was created
   */
  private CompilationUnit getTypeRoot(DartUnit astRoot) {
    IResource[] resources = ResourceUtil.getResources(astRoot.getSourceInfo().getSource().getUri());
    if (resources != null) {
      for (IResource resource : resources) {
        if (resource instanceof IFile && resource.exists()) {
          DartElement element = DartCore.create(resource);
          if (element instanceof CompilationUnit) {
            return (CompilationUnit) element;
          }
        }
      }
    }
    return null;
  }

  private TextEdit internalRewriteAST(char[] content, LineInformation lineInfo, String lineDelim,
      List<DartComment> commentNodes, Map<String, String> options, DartNode rootNode,
      RecoveryScannerData recoveryScannerData) {
    TextEdit result = new MultiTextEdit();
    // validateASTNotModified(rootNode);

    TargetSourceRangeComputer sourceRangeComputer = getExtendedSourceRangeComputer();
    eventStore.prepareMovedNodes(sourceRangeComputer);

    ASTRewriteAnalyzer analyzer = new ASTRewriteAnalyzer(
        content,
        lineInfo,
        lineDelim,
        result,
        eventStore,
        nodeStore,
        commentNodes,
        options,
        sourceRangeComputer,
        recoveryScannerData);
    analyzer.analyze(rootNode); // throws IllegalArgumentException

    eventStore.revertMovedNodes();
    return result;
  }

  private void validateIsCorrectAST(DartNode node) {
    DartCore.notYetImplemented();
    // if (node.getAST() != getAST()) {
    //      throw new IllegalArgumentException("Node is not inside the AST"); //$NON-NLS-1$
    // }
  }

  private void validateIsExistingNode(DartNode node) {
    if (node.getSourceInfo().getOffset() == -1) {
      throw new IllegalArgumentException("Node is not an existing node"); //$NON-NLS-1$
    }
  }

  private void validateIsListProperty(StructuralPropertyDescriptor property) {
    if (!property.isChildListProperty()) {
      String message = property.getId() + " is not a list property"; //$NON-NLS-1$
      throw new IllegalArgumentException(message);
    }
  }

  private void validateIsPropertyOfNode(StructuralPropertyDescriptor property, DartNode node) {
    if (!property.getNodeClass().isInstance(node)) {
      String message = property.getId() + " is not a property of type " + node.getClass().getName(); //$NON-NLS-1$
      throw new IllegalArgumentException(message);
    }
  }

  private void validatePropertyType(StructuralPropertyDescriptor prop, Object node) {
    if (prop.isChildListProperty()) {
      String message = "Cannot modify a list property, use a list rewriter"; //$NON-NLS-1$
      throw new IllegalArgumentException(message);
    }
  }
}
