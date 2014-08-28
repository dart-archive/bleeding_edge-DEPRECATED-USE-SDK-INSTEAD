/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.ui.internal.text.editor;

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.Element;
import com.google.dart.server.generated.types.Outline;
import com.google.dart.tools.ui.DartElementImageDescriptor;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.viewsupport.ImageDescriptorRegistry;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.TreeItem;

import java.util.List;

/**
 * Helper for creating and displaying {@link LightNodeElement}s.
 */
public class LightNodeElements {
  /**
   * {@link ViewerComparator} for {@link LightNodeElement} names.
   */
  public static class NameComparator extends ViewerComparator {
    private static final int NOT_ELEMENT = 2;
    private static final int PRIVATE_ELEMENT = 1;
    private static final int PUBLIC_ELEMENT = 0;

    @Override
    public int category(Object e) {
      if (!(e instanceof LightNodeElement)) {
        return NOT_ELEMENT;
      }
      LightNodeElement element = (LightNodeElement) e;
      if (element.isPrivate()) {
        return PRIVATE_ELEMENT;
      }
      return PUBLIC_ELEMENT;
    }

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      // compare categories
      int cat1 = category(e1);
      int cat2 = category(e2);
      if (cat1 != cat2) {
        return cat1 - cat2;
      }
      // check types
      if (!(e1 instanceof LightNodeElement)) {
        return 0;
      }
      if (!(e2 instanceof LightNodeElement)) {
        return 0;
      }
      // compare names
      String name1 = ((LightNodeElement) e1).getName();
      String name2 = ((LightNodeElement) e2).getName();
      if (name1 == null || name2 == null) {
        return 0;
      }
      return name1.compareTo(name2);
    }
  }

  /**
   * {@link ViewerComparator} for {@link LightNodeElement} positions.
   */
  public static class PositionComparator extends ViewerComparator {
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (!(e1 instanceof LightNodeElement)) {
        return 0;
      }
      if (!(e2 instanceof LightNodeElement)) {
        return 0;
      }
      int offset1 = ((LightNodeElement) e1).getNameOffset();
      int offset2 = ((LightNodeElement) e2).getNameOffset();
      return offset1 - offset2;
    }
  }

  /**
   * {@link ITreeContentProvider} for {@link LightNodeElement}s in {@link CompilationUnit}.
   */
  private static class NodeContentProvider implements ITreeContentProvider {
    private final IFile contextFile;
    private final List<LightNodeElement> elements = Lists.newArrayList();

    public NodeContentProvider(IFile contextFile) {
      this.contextFile = contextFile;
    }

    @Override
    public void dispose() {
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      List<LightNodeElement> children = ((LightNodeElement) parentElement).children;
      return children.toArray(new LightNodeElement[children.size()]);
    }

    @Override
    public Object[] getElements(Object inputElement) {
      return elements.toArray(new LightNodeElement[elements.size()]);
    }

    @Override
    public Object getParent(Object element) {
      return ((LightNodeElement) element).getParent();
    }

    @Override
    public boolean hasChildren(Object element) {
      return getChildren(element).length != 0;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      elements.clear();
      // prepare CompilationUnit
      CompilationUnit unit = (CompilationUnit) newInput;
      if (unit == null) {
        return;
      }
      // create elements
      NodeList<CompilationUnitMember> unitDeclarations = unit.getDeclarations();
      for (CompilationUnitMember unitMember : unitDeclarations) {
        if (unitMember instanceof TopLevelVariableDeclaration) {
          TopLevelVariableDeclaration topVarDecl = (TopLevelVariableDeclaration) unitMember;
          List<VariableDeclaration> variables = topVarDecl.getVariables().getVariables();
          for (VariableDeclaration variable : variables) {
            LightNodeElement element = createLightNodeElement(contextFile, null, variable, true);
            if (element != null) {
              elements.add(element);
            }
          }
        } else {
          LightNodeElement element = createLightNodeElement(contextFile, null, unitMember, true);
          if (element != null) {
            elements.add(element);
          }
        }
        if (elements.size() > MAX_UNIT_MEMBER_COUNT) {
          elements.add(new LightNodeElement(contextFile, null, unit, unit, String.format(
              MAX_CHILDREN_TEXT,
              MAX_UNIT_MEMBER_COUNT,
              unitDeclarations.size())));
          break;
        }
      }
    }
  }
  /**
   * {@link LabelProvider} for {@link LightNodeElement}.
   */
  private static class NodeLabelProvider extends LabelProvider implements IStyledLabelProvider {
    private static final Point SIZE = new Point(22, 16);
    private static final ImageDescriptorRegistry registry = DartToolsPlugin.getImageDescriptorRegistry();

    private static ImageDescriptor getBaseImageDescriptor(AstNode node, boolean isPrivate) {
      if (node instanceof ClassDeclaration) {
        return isPrivate ? DartPluginImages.DESC_DART_CLASS_PRIVATE
            : DartPluginImages.DESC_DART_CLASS_PUBLIC;
      }
      if (node instanceof FunctionTypeAlias) {
        return isPrivate ? DartPluginImages.DESC_DART_FUNCTIONTYPE_PRIVATE
            : DartPluginImages.DESC_DART_FUNCTIONTYPE_PUBLIC;
      }
      if (node instanceof VariableDeclaration) {
        return isPrivate ? DartPluginImages.DESC_DART_FIELD_PRIVATE
            : DartPluginImages.DESC_DART_FIELD_PUBLIC;
      }
      if (node instanceof FunctionDeclaration || node instanceof ConstructorDeclaration
          || node instanceof MethodDeclaration) {
        return isPrivate ? DartPluginImages.DESC_DART_METHOD_PRIVATE
            : DartPluginImages.DESC_DART_METHOD_PUBLIC;
      }
      return null;
    }

    private static ImageDescriptor getImageDescriptor(AstNode node, boolean isPrivate) {
      ImageDescriptor base = getBaseImageDescriptor(node, isPrivate);
      if (base == null) {
        return null;
      }
      int flags = 0;
      // ClassDeclaration
      if (node instanceof ClassDeclaration) {
        ClassDeclaration classDeclaration = (ClassDeclaration) node;
        if (classDeclaration.isAbstract()) {
          flags |= DartElementImageDescriptor.ABSTRACT;
        }
      }
      // ConstructorDeclaration
      if (node instanceof ConstructorDeclaration) {
        flags |= DartElementImageDescriptor.CONSTRUCTOR;
      }
      // MethodDeclaration
      if (node instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) node;
        if (method.isAbstract()) {
          flags |= DartElementImageDescriptor.ABSTRACT;
        }
        if (method.isStatic()) {
          flags |= DartElementImageDescriptor.STATIC;
        }
        if (method.isGetter()) {
          flags |= DartElementImageDescriptor.GETTER;
        }
        if (method.isSetter()) {
          flags |= DartElementImageDescriptor.SETTER;
        }
      }
      // done
      return new DartElementImageDescriptor(base, flags, SIZE);
    }

    @Override
    public Image getImage(Object o) {
      LightNodeElement element = (LightNodeElement) o;
      boolean isPrivate = element.isPrivate();;
      AstNode node = element.getNode();
      ImageDescriptor descriptor = getImageDescriptor(node, isPrivate);
      if (descriptor != null) {
        return registry.get(descriptor);
      }
      return null;
    }

    @Override
    public StyledString getStyledText(Object obj) {
      StyledString styledString = new StyledString(getText(obj));
      // prepare object elements
      LightNodeElement lightElement = (LightNodeElement) obj;
      AstNode node = lightElement.getNode();
      // prepare parameters
      FormalParameterList parameters = null;
      TypeName returnType = null;
      String returnTypeSeparator = Element.RIGHT_ARROW;
      if (node instanceof VariableDeclaration
          && node.getParent() instanceof VariableDeclarationList) {
        VariableDeclarationList declaration = (VariableDeclarationList) node.getParent();
        returnType = declaration.getType();
        returnTypeSeparator = " : ";
      }
      if (node instanceof FunctionDeclaration) {
        FunctionDeclaration function = (FunctionDeclaration) node;
        FunctionExpression functionExpression = function.getFunctionExpression();
        if (functionExpression != null) {
          parameters = functionExpression.getParameters();
          returnType = function.getReturnType();
        }
      }
      if (node instanceof FunctionTypeAlias) {
        FunctionTypeAlias functionTypeAlias = (FunctionTypeAlias) node;
        parameters = functionTypeAlias.getParameters();
        returnType = functionTypeAlias.getReturnType();
      }
      if (node instanceof ConstructorDeclaration) {
        ConstructorDeclaration constructor = (ConstructorDeclaration) node;
        parameters = constructor.getParameters();
      }
      if (node instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) node;
        parameters = method.getParameters();
        returnType = method.getReturnType();
      }
      // may be append parameters
      if (parameters != null) {
        styledString.append(parameters.toSource(), StyledString.DECORATIONS_STYLER);
      }
      if (returnType != null) {
        styledString.append(
            returnTypeSeparator + returnType.toSource(),
            StyledString.QUALIFIER_STYLER);
      }
      // done
      return styledString;
    }

    @Override
    public String getText(Object element) {
      return ((LightNodeElement) element).getName();
    }
  }

  /**
   * The maximum number of children we want to show in any compilation unit.
   */
  private static final int MAX_UNIT_MEMBER_COUNT = 750;

  /**
   * The maximum number of children we want to show in any class.
   */
  private static final int MAX_CLASS_MEMBER_COUNT = 250;

  /**
   * The text to show if there are too many children.
   */
  private static final String MAX_CHILDREN_TEXT = "<First %d of %d children are displayed>";

  public static final ViewerComparator NAME_COMPARATOR = new NameComparator();
  public static final ViewerComparator POSITION_COMPARATOR = new PositionComparator();

  /**
   * @return the {@link LightNodeElement} for given {@link AstNode}, may be <code>null</code> if
   *         given is not declaration and does not have reasonable declaration child.
   */
  public static LightNodeElement createLightNodeElement(IFile contextFile, AstNode node) {
    if (node == null) {
      return null;
    }
    // prepare "childNode"
    AstNode childNode = null;
    AstNode parentNode = null;
    if (childNode == null) {
      FunctionDeclaration function = node.getAncestor(FunctionDeclaration.class);
      if (function != null) {
        childNode = function;
      }
    }
    if (childNode == null) {
      ConstructorDeclaration constructor = node.getAncestor(ConstructorDeclaration.class);
      if (constructor != null) {
        childNode = constructor;
      }
    }
    if (childNode == null) {
      MethodDeclaration method = node.getAncestor(MethodDeclaration.class);
      if (method != null) {
        childNode = method;
      }
    }
    if (childNode == null) {
      FunctionTypeAlias function = node.getAncestor(FunctionTypeAlias.class);
      if (function != null) {
        childNode = function;
      }
    }
    if (childNode == null) {
      childNode = node.getAncestor(VariableDeclaration.class);
    }
    {
      FieldDeclaration fieldDeclaration = node.getAncestor(FieldDeclaration.class);
      if (fieldDeclaration != null) {
        parentNode = fieldDeclaration.getParent();
        List<VariableDeclaration> fields = fieldDeclaration.getFields().getVariables();
        if (!fields.isEmpty()) {
          childNode = fields.get(0);
        }
      }
    }
    {
      TopLevelVariableDeclaration decl = node.getAncestor(TopLevelVariableDeclaration.class);
      if (decl != null) {
        parentNode = decl.getParent();
        List<VariableDeclaration> vars = decl.getVariables().getVariables();
        if (!vars.isEmpty()) {
          childNode = vars.get(0);
        }
      }
    }
    if (childNode == null) {
      ClassDeclaration clazz = node.getAncestor(ClassDeclaration.class);
      if (clazz != null) {
        childNode = clazz;
      }
    }
    // prepare "parent"
    LightNodeElement parent = null;
    if (childNode != null) {
      if (parentNode == null) {
        parentNode = childNode.getParent();
      }
      parent = createLightNodeElement(contextFile, parentNode);
    }
    // try to create LightNodeElement
    LightNodeElement element = createLightNodeElement(contextFile, parent, childNode, false);
    if (element == null) {
      element = parent;
    }
    return element;
  }

  /**
   * Expands {@link #viewer} us much as possible while still in the given time budget.
   */
  public static void expandTreeItemsTimeBoxed(TreeViewer viewer, long nanoBudget) {
    int numIterations = 10;
    int childrenLimit = 10;
    TreeItem[] rootTreeItems = viewer.getTree().getItems();
    for (int i = 0; i < numIterations; i++) {
      if (nanoBudget < 0) {
        break;
      }
      nanoBudget = expandTreeItemsTimeBoxed(viewer, rootTreeItems, childrenLimit, nanoBudget);
      childrenLimit *= 2;
    }
  }

  /**
   * @return the root {@link LightNodeElement}s created by {@link #newTreeContentProvider()}.
   */
  public static List<LightNodeElement> getRootElements(TreeViewer viewer) {
    return ((NodeContentProvider) viewer.getContentProvider()).elements;
  }

  /**
   * @return the new label provider instance to use for displaying {@link LightNodeElement}s.
   */
  public static IBaseLabelProvider newLabelProvider() {
    return new DelegatingStyledCellLabelProvider(new NodeLabelProvider());
  }

  /**
   * @return {@link ITreeContentProvider} for {@link TreeViewer} of {@link LightNodeElement}s.
   */
  public static ITreeContentProvider newTreeContentProvider(DartEditor editor) {
    IFile contextFile = editor.getInputResourceFile();
    return newTreeContentProvider(contextFile);
  }

  /**
   * @return {@link ITreeContentProvider} for {@link TreeViewer} of {@link LightNodeElement}s.
   */
  public static ITreeContentProvider newTreeContentProvider(IFile contextFile) {
    return new NodeContentProvider(contextFile);
  }

  private static void addLocalFunctions(final IFile contextFile, final LightNodeElement parent,
      AstNode in) {
    if (in == null) {
      return;
    }
    in.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitAssignmentExpression(AssignmentExpression node) {
        return null;
      }

      @Override
      public Void visitBinaryExpression(BinaryExpression node) {
        return null;
      }

      @Override
      public Void visitFunctionDeclaration(FunctionDeclaration node) {
        createLightNodeElement(contextFile, parent, node, true);
        return null;
      }

      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        return null;
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        return null;
      }

      @Override
      public Void visitVariableDeclarationStatement(VariableDeclarationStatement node) {
        return null;
      }
    });
  }

  private static LightNodeElement createLightNodeElement(final IFile contextFile,
      LightNodeElement parent, AstNode node, boolean withChildren) {
    // VariableDeclaration
    if (node instanceof VariableDeclaration) {
      VariableDeclaration variable = (VariableDeclaration) node;
      SimpleIdentifier nameNode = variable.getName();
      String name = nameNode.getName();
      return new LightNodeElement(contextFile, parent, variable, nameNode, name);
    }
    // ConstructorDeclaration
    if (node instanceof ConstructorDeclaration) {
      ConstructorDeclaration constructor = (ConstructorDeclaration) node;
      String name = parent.getName();
      SimpleIdentifier constructorName = constructor.getName();
      LightNodeElement result;
      if (constructorName != null) {
        name += "." + constructorName.getName();
        result = new LightNodeElement(contextFile, parent, node, constructorName, name);
      } else {
        result = new LightNodeElement(contextFile, parent, node, constructor.getReturnType(), name);
      }
      if (withChildren) {
        addLocalFunctions(contextFile, result, constructor.getBody());
      }
      return result;
    }
    // method
    if (node instanceof MethodDeclaration) {
      MethodDeclaration method = (MethodDeclaration) node;
      SimpleIdentifier nameNode = method.getName();
      String name = nameNode.getName();
      if (method.isSetter()) {
        name += "=";
      }
      LightNodeElement result = new LightNodeElement(contextFile, parent, node, nameNode, name);
      if (withChildren) {
        addLocalFunctions(contextFile, result, method.getBody());
      }
      return result;
    }
    // ClassDeclaration
    if (node instanceof ClassDeclaration) {
      ClassDeclaration classDeclaration = (ClassDeclaration) node;
      SimpleIdentifier nameNode = classDeclaration.getName();
      LightNodeElement classElement = new LightNodeElement(
          contextFile,
          null,
          node,
          nameNode,
          nameNode.getName());
      if (withChildren) {
        NodeList<ClassMember> classMembers = classDeclaration.getMembers();
        for (ClassMember classMember : classMembers) {
          if (classMember instanceof FieldDeclaration) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration) classMember;
            List<VariableDeclaration> fields = fieldDeclaration.getFields().getVariables();
            for (VariableDeclaration field : fields) {
              createLightNodeElement(contextFile, classElement, field, true);
            }
          } else {
            createLightNodeElement(contextFile, classElement, classMember, true);
          }
          if (classElement.children.size() > MAX_CLASS_MEMBER_COUNT) {
            new LightNodeElement(
                contextFile,
                classElement,
                classDeclaration,
                classDeclaration.getName(),
                String.format(MAX_CHILDREN_TEXT, MAX_CLASS_MEMBER_COUNT, classMembers.size()));
            break;
          }
        }
      }
      return classElement;
    }
    // FunctionDeclaration
    if (node instanceof FunctionDeclaration) {
      FunctionDeclaration functionDeclaration = (FunctionDeclaration) node;
      SimpleIdentifier nameNode = functionDeclaration.getName();
      final LightNodeElement result = new LightNodeElement(
          contextFile,
          parent,
          functionDeclaration,
          nameNode,
          nameNode.getName());
      if (withChildren) {
        addLocalFunctions(contextFile, result, functionDeclaration.getFunctionExpression());
      }
      return result;
    }
    // FunctionTypeAlias
    if (node instanceof FunctionTypeAlias) {
      FunctionTypeAlias alias = (FunctionTypeAlias) node;
      SimpleIdentifier nameNode = alias.getName();
      return new LightNodeElement(contextFile, null, alias, nameNode, nameNode.getName());
    }
    // unknown
    return null;
  }

  /**
   * Expands given {@link TreeItem}s if they have not too much children and we have time budget.
   */
  private static long expandTreeItemsTimeBoxed(TreeViewer viewer, TreeItem[] items,
      int childrenLimit, long nanoBudget) {
    if (nanoBudget < 0) {
      return -1;
    }
    for (TreeItem item : items) {
      Object itemData = item.getData();
      // prepare number of children
      int numChildren = 0;
      {
        if (itemData instanceof LightNodeElement) {
          numChildren = ((LightNodeElement) itemData).children.size();
        }
        if (itemData instanceof Outline) {
          numChildren = ((Outline) itemData).getChildren().size();
        }
      }
      // has children, not too many?
      if (numChildren == 0 || numChildren > childrenLimit) {
        continue;
      }
      // expand single item
      {
        long startNano = System.nanoTime();
        viewer.setExpandedState(itemData, true);
        nanoBudget -= System.nanoTime() - startNano;
      }
      // expand children
      nanoBudget = expandTreeItemsTimeBoxed(viewer, item.getItems(), childrenLimit, nanoBudget);
      if (nanoBudget < 0) {
        break;
      }
    }
    return nanoBudget;
  }
}
