package com.google.dart.tools.internal.corext.dom;

import com.google.common.collect.Lists;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.resolver.Element;

import java.util.List;

/**
 * Find all nodes connected to a given binding or node. e.g. Declaration of a field and all
 * references. For types this includes also the constructor declaration, for methods also overridden
 * methods or methods overriding (if existing in the same AST), for constructors also the type and
 * all other constructors.
 */
public class LinkedNodeFinder {

  private static class BindingFinder extends ASTVisitor<Void> {

    private static Element getDeclaration(Element binding) {
//      if (binding instanceof ClassBinding) {
//        return ((ITypeBinding) binding).getTypeDeclaration();
//      } else if (binding instanceof IMethodBinding) {
//        IMethodBinding methodBinding = (IMethodBinding) binding;
//        if (methodBinding.isConstructor()) { // link all constructors with their type
//          return methodBinding.getDeclaringClass().getTypeDeclaration();
//        } else {
//          return methodBinding.getMethodDeclaration();
//        }
//      } else if (binding instanceof IVariableBinding) {
//        return ((IVariableBinding) binding).getVariableDeclaration();
//      }
      return binding;
    }

    private final Element fBinding;

    private final List<DartIdentifier> fResult = Lists.newArrayList();

    public BindingFinder(Element binding) {
      fBinding = getDeclaration(binding);
    }

    @Override
    public Void visitIdentifier(DartIdentifier node) {
      Element binding = node.getElement();
      if (binding == null) {
        return null;
      }
      binding = getDeclaration(binding);

      if (fBinding == binding) {
        fResult.add(node);
      } else if (binding.getKind() != fBinding.getKind()) {
        return null;
      }
      // TODO(scheglov) restore later
//      else if (binding.getKind() == IBinding.METHOD) {
//        IMethodBinding curr = (IMethodBinding) binding;
//        IMethodBinding methodBinding = (IMethodBinding) fBinding;
//        if (methodBinding.overrides(curr) || curr.overrides(methodBinding)) {
//          fResult.add(node);
//        }
//      }
      return null;
    }
  }

//  private static class LabelFinder extends ASTVisitor {
//
//    private final DartIdentifier fLabel;
//    private DartNode fDefiningLabel;
//    private final ArrayList<DartIdentifier> fResult;
//
//    public LabelFinder(DartIdentifier label, ArrayList<DartIdentifier> result) {
//      super(true);
//      fLabel = label;
//      fResult = result;
//      fDefiningLabel = null;
//    }
//
//    @Override
//    public boolean visit(BreakStatement node) {
//      DartIdentifier label = node.getLabel();
//      if (fDefiningLabel != null && isSameLabel(label) && ASTNodes.isParent(label, fDefiningLabel)) {
//        fResult.add(label);
//      }
//      return false;
//    }
//
//    @Override
//    public boolean visit(ContinueStatement node) {
//      DartIdentifier label = node.getLabel();
//      if (fDefiningLabel != null && isSameLabel(label) && ASTNodes.isParent(label, fDefiningLabel)) {
//        fResult.add(label);
//      }
//      return false;
//    }
//
//    @Override
//    public boolean visit(LabeledStatement node) {
//      if (fDefiningLabel == null) {
//        DartIdentifier label = node.getLabel();
//        if (fLabel == label || isSameLabel(label) && ASTNodes.isParent(fLabel, node)) {
//          fDefiningLabel = node;
//          fResult.add(label);
//        }
//      }
//      node.getBody().accept(this);
//      return false;
//    }
//
//    private boolean isSameLabel(DartIdentifier label) {
//      return label != null && fLabel.getIdentifier().equals(label.getIdentifier());
//    }
//  }

  private static final int FIELD = 1;

  private static final int METHOD = 2;
  private static final int TYPE = 4;
  private static final int LABEL = 8;
  private static final int NAME = FIELD | TYPE;

  /**
   * Find all nodes connected to the given binding. e.g. Declaration of a field and all references.
   * For types this includes also the constructor declaration, for methods also overridden methods
   * or methods overriding (if existing in the same AST)
   * 
   * @param root The root of the AST tree to search
   * @param binding The binding of the searched nodes
   * @return Return
   */
  public static DartIdentifier[] findByBinding(DartNode root, Element binding) {
    BindingFinder nodeFinder = new BindingFinder(binding);
    root.accept(nodeFinder);
    List<DartIdentifier> res = nodeFinder.fResult;
    return res.toArray(new DartIdentifier[res.size()]);
  }

  /**
   * Find all nodes connected to the given name node. If the node has a binding then all nodes
   * connected to this binding are returned. If the node has no binding, then all nodes that also
   * miss a binding and have the same name are returned.
   * 
   * @param root The root of the AST tree to search
   * @param name The node to find linked nodes for
   * @return Return
   */
  public static DartIdentifier[] findByNode(DartNode root, DartIdentifier name) {
    Element binding = name.getElement();
    if (binding != null) {
      return findByBinding(root, binding);
    }
    // TODO(scheglov) restore later
//    DartIdentifier[] names = findByProblems(root, name);
//    if (names != null) {
//      return names;
//    }
//    int parentKind = name.getParent().getNodeType();
//    if (parentKind == DartNode.LABELED_STATEMENT || parentKind == DartNode.BREAK_STATEMENT
//        || parentKind == DartNode.CONTINUE_STATEMENT) {
//      ArrayList<DartIdentifier> res = new ArrayList<DartIdentifier>();
//      LabelFinder nodeFinder = new LabelFinder(name, res);
//      root.accept(nodeFinder);
//      return res.toArray(new DartIdentifier[res.size()]);
//    }
    return new DartIdentifier[] {name};
  }

//  public static DartIdentifier[] findByProblems(DartNode parent, DartIdentifier nameNode) {
//    ArrayList<DartIdentifier> res = new ArrayList<DartIdentifier>();
//
//    DartNode astRoot = parent.getRoot();
//    if (!(astRoot instanceof CompilationUnit)) {
//      return null;
//    }
//
//    IProblem[] problems = ((CompilationUnit) astRoot).getProblems();
//    int nameNodeKind = getNameNodeProblemKind(problems, nameNode);
//    if (nameNodeKind == 0) { // no problem on node
//      return null;
//    }
//
//    int bodyStart = parent.getStartPosition();
//    int bodyEnd = bodyStart + parent.getLength();
//
//    String name = nameNode.getIdentifier();
//
//    for (int i = 0; i < problems.length; i++) {
//      IProblem curr = problems[i];
//      int probStart = curr.getSourceStart();
//      int probEnd = curr.getSourceEnd() + 1;
//
//      if (probStart > bodyStart && probEnd < bodyEnd) {
//        int currKind = getProblemKind(curr);
//        if ((nameNodeKind & currKind) != 0) {
//          DartNode node = NodeFinder.perform(parent, probStart, probEnd - probStart);
//          if (node instanceof DartIdentifier && name.equals(((DartIdentifier) node).getIdentifier())) {
//            res.add((DartIdentifier) node);
//          }
//        }
//      }
//    }
//    return res.toArray(new DartIdentifier[res.size()]);
//  }
//
//  private static int getNameNodeProblemKind(IProblem[] problems, DartIdentifier nameNode) {
//    int nameOffset = nameNode.getStartPosition();
//    int nameInclEnd = nameOffset + nameNode.getLength() - 1;
//
//    for (int i = 0; i < problems.length; i++) {
//      IProblem curr = problems[i];
//      if (curr.getSourceStart() == nameOffset && curr.getSourceEnd() == nameInclEnd) {
//        int kind = getProblemKind(curr);
//        if (kind != 0) {
//          return kind;
//        }
//      }
//    }
//    return 0;
//  }
//
//  private static int getProblemKind(IProblem problem) {
//    switch (problem.getID()) {
//      case IProblem.UndefinedField:
//        return FIELD;
//      case IProblem.UndefinedMethod:
//        return METHOD;
//      case IProblem.UndefinedLabel:
//        return LABEL;
//      case IProblem.UndefinedName:
//      case IProblem.UnresolvedVariable:
//        return NAME;
//      case IProblem.UndefinedType:
//        return TYPE;
//    }
//    return 0;
//  }

  private LinkedNodeFinder() {
  }
}
