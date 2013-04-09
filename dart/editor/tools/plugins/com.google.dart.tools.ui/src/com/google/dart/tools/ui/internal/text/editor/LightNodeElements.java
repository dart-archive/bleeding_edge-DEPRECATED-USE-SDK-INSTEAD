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
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.tools.ui.DartElementImageDescriptor;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.viewsupport.ImageDescriptorRegistry;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import java.util.List;

/**
 * Helper for creating and displaying {@link LightNodeElement}s.
 */
public class LightNodeElements {
  /**
   * {@link ViewerComparator} for {@link LightNodeElement} names.
   */
  public static class NameComparator extends ViewerComparator {
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (!(e1 instanceof LightNodeElement)) {
        return 0;
      }
      if (!(e2 instanceof LightNodeElement)) {
        return 0;
      }
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
    private final List<LightNodeElement> elements = Lists.newArrayList();

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
      for (CompilationUnitMember unitMember : unit.getDeclarations()) {
        if (unitMember instanceof TopLevelVariableDeclaration) {
          TopLevelVariableDeclaration topVarDecl = (TopLevelVariableDeclaration) unitMember;
          List<VariableDeclaration> variables = topVarDecl.getVariables().getVariables();
          for (VariableDeclaration variable : variables) {
            LightNodeElement element = createLightNodeElement(null, variable, true);
            if (element != null) {
              elements.add(element);
            }
          }
        } else {
          LightNodeElement element = createLightNodeElement(null, unitMember, true);
          if (element != null) {
            elements.add(element);
          }
        }
      }
    }
  }

  /**
   * {@link LabelProvider} for {@link LightNodeElement}.
   */
  private static class NodeLabelProvider extends LabelProvider {
    private static final Point SIZE = new Point(22, 16);
    private static final ImageDescriptorRegistry registry = DartToolsPlugin.getImageDescriptorRegistry();

    private static ImageDescriptor getBaseImageDescriptor(ASTNode node, boolean isPrivate) {
      if (node instanceof ClassDeclaration) {
        return isPrivate ? DartPluginImages.DESC_DART_CLASS_PRIVATE
            : DartPluginImages.DESC_DART_CLASS_PUBLIC;
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

    private static ImageDescriptor getImageDescriptor(ASTNode node, boolean isPrivate) {
      ImageDescriptor base = getBaseImageDescriptor(node, isPrivate);
      if (base == null) {
        return null;
      }
      int flags = 0;
      // ClassDeclaration
      if (node instanceof ClassDeclaration) {
        ClassDeclaration classDeclaration = (ClassDeclaration) node;
        if (classDeclaration.getAbstractKeyword() != null) {
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
      ASTNode node = element.getNode();
      ImageDescriptor descriptor = getImageDescriptor(node, isPrivate);
      if (descriptor != null) {
        return registry.get(descriptor);
      }
      return null;
    }

    @Override
    public String getText(Object element) {
      return ((LightNodeElement) element).getName();
    }
  }

  public static final LabelProvider LABEL_PROVIDER = new NodeLabelProvider();
  public static final ViewerComparator NAME_COMPARATOR = new NameComparator();
  public static final ViewerComparator POSITION_COMPARATOR = new PositionComparator();

  /**
   * @return the {@link LightNodeElement} for given {@link ASTNode}, may be <code>null</code> if
   *         given is not declaration and does not have reasonable declaration child.
   */
  public static LightNodeElement createLightNodeElement(ASTNode node) {
    if (node == null) {
      return null;
    }
    // prepare "parent" and "childNode"
    LightNodeElement parent = null;
    ASTNode childNode = null;
    ClassDeclaration enclosingClass = node.getAncestor(ClassDeclaration.class);
    if (enclosingClass != null) {
      parent = createLightNodeElement(null, enclosingClass, false);
      {
        MethodDeclaration method = node.getAncestor(MethodDeclaration.class);
        if (method != null) {
          childNode = method;
        }
      }
      {
        ConstructorDeclaration constructor = node.getAncestor(ConstructorDeclaration.class);
        if (constructor != null) {
          childNode = constructor;
        }
      }
      if (childNode == null) {
        childNode = node.getAncestor(VariableDeclaration.class);
      }
      if (childNode == null) {
        FieldDeclaration fieldDeclaration = node.getAncestor(FieldDeclaration.class);
        if (fieldDeclaration != null) {
          List<VariableDeclaration> fields = fieldDeclaration.getFields().getVariables();
          if (!fields.isEmpty()) {
            childNode = fields.get(0);
          }
        }
      }
    } else {
      {
        FunctionDeclaration function = node.getAncestor(FunctionDeclaration.class);
        if (function != null) {
          childNode = function;
        }
      }
      if (childNode == null) {
        childNode = node.getAncestor(VariableDeclaration.class);
      }
      if (childNode == null) {
        TopLevelVariableDeclaration decl = node.getAncestor(TopLevelVariableDeclaration.class);
        if (decl != null) {
          List<VariableDeclaration> vars = decl.getVariables().getVariables();
          if (!vars.isEmpty()) {
            childNode = vars.get(0);
          }
        }
      }
    }
    // try to create LightNodeElement
    LightNodeElement element = createLightNodeElement(parent, childNode, false);
    if (element == null) {
      element = parent;
    }
    return element;
  }

  /**
   * @return the root {@link LightNodeElement}s created by {@link #newTreeContentProvider()}.
   */
  public static List<LightNodeElement> getRootElements(TreeViewer viewer) {
    return ((NodeContentProvider) viewer.getContentProvider()).elements;
  }

  /**
   * @return {@link ITreeContentProvider} for {@link TreeViewer} of {@link LightNodeElement}s.
   */
  public static ITreeContentProvider newTreeContentProvider() {
    return new NodeContentProvider();
  }

  private static LightNodeElement createLightNodeElement(LightNodeElement parent, ASTNode node,
      boolean withChildren) {
    // VariableDeclaration
    if (node instanceof VariableDeclaration) {
      VariableDeclaration variable = (VariableDeclaration) node;
      SimpleIdentifier nameNode = variable.getName();
      String name = nameNode.getName();
      return new LightNodeElement(parent, variable, nameNode, name);
    }
    // ConstructorDeclaration
    if (node instanceof ConstructorDeclaration) {
      ConstructorDeclaration constructor = (ConstructorDeclaration) node;
      String name = parent.getName();
      SimpleIdentifier constructorName = constructor.getName();
      if (constructorName != null) {
        name += "." + constructorName.getName();
        return new LightNodeElement(parent, node, constructorName, name);
      } else {
        return new LightNodeElement(parent, node, constructor.getReturnType(), name);
      }
    }
    // method
    if (node instanceof MethodDeclaration) {
      MethodDeclaration method = (MethodDeclaration) node;
      SimpleIdentifier nameNode = method.getName();
      String name = nameNode.getName();
      if (method.isSetter()) {
        name += "=";
      }
      return new LightNodeElement(parent, node, nameNode, name);
    }
    // ClassDeclaration
    if (node instanceof ClassDeclaration) {
      ClassDeclaration classDeclaration = (ClassDeclaration) node;
      SimpleIdentifier nameNode = classDeclaration.getName();
      LightNodeElement classElement = new LightNodeElement(null, node, nameNode, nameNode.getName());
      if (withChildren) {
        for (ClassMember classMember : classDeclaration.getMembers()) {
          if (classMember instanceof FieldDeclaration) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration) classMember;
            List<VariableDeclaration> fields = fieldDeclaration.getFields().getVariables();
            for (VariableDeclaration field : fields) {
              createLightNodeElement(classElement, field, true);
            }
          } else {
            createLightNodeElement(classElement, classMember, true);
          }
        }
      }
      return classElement;
    }
    // FunctionDeclaration
    if (node instanceof FunctionDeclaration) {
      FunctionDeclaration functionDeclaration = (FunctionDeclaration) node;
      SimpleIdentifier nameNode = functionDeclaration.getName();
      return new LightNodeElement(null, functionDeclaration, nameNode, nameNode.getName());
    }
    // unknown
    return null;
  }
}
