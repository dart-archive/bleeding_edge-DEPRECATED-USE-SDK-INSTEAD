package com.google.dart.tools.internal.corext.refactoring;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.internal.util.DartModelUtil;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to detect whether a certain refactoring can be enabled on a selection.
 * <p>
 * This class has been introduced to decouple actions from the refactoring code, in order not to
 * eagerly load refactoring classes during action initialization.
 * </p>
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RefactoringAvailabilityTester {

  public static Type getDeclaringType(DartElement element) {
    if (element == null) {
      return null;
    }
    if (!(element instanceof Type)) {
      element = element.getAncestor(Type.class);
    }
    return (Type) element;
  }

  public static DartElement[] getDartElements(Object[] elements) {
    List<DartElement> result = new ArrayList<DartElement>();
    for (int index = 0; index < elements.length; index++) {
      if (elements[index] instanceof DartElement) {
        result.add((DartElement) elements[index]);
      }
    }
    return result.toArray(new DartElement[result.size()]);
  }

//  public static TypeMember[] getPullUpMembers(Type type) throws DartModelException {
//    List<TypeMember> list = new ArrayList<TypeMember>(3);
//    if (type.exists()) {
//      TypeMember[] members = type.getFields();
//      for (int index = 0; index < members.length; index++) {
//        if (isPullUpAvailable(members[index])) {
//          list.add(members[index]);
//        }
//      }
//      members = type.getMethods();
//      for (int index = 0; index < members.length; index++) {
//        if (isPullUpAvailable(members[index])) {
//          list.add(members[index]);
//        }
//      }
//    }
//    return list.toArray(new TypeMember[list.size()]);
//  }
//
//  public static TypeMember[] getPushDownMembers(Type type) throws DartModelException {
//    List<TypeMember> list = new ArrayList<TypeMember>(3);
//    if (type.exists()) {
//      TypeMember[] members = type.getFields();
//      for (int index = 0; index < members.length; index++) {
//        if (isPushDownAvailable(members[index])) {
//          list.add(members[index]);
//        }
//      }
//      members = type.getMethods();
//      for (int index = 0; index < members.length; index++) {
//        if (isPushDownAvailable(members[index])) {
//          list.add(members[index]);
//        }
//      }
//    }
//    return list.toArray(new TypeMember[list.size()]);
//  }
//
//  public static IResource[] getResources(Object[] elements) {
//    List<IResource> result = new ArrayList<IResource>();
//    for (int index = 0; index < elements.length; index++) {
//      if (elements[index] instanceof IResource) {
//        result.add((IResource) elements[index]);
//      }
//    }
//    return result.toArray(new IResource[result.size()]);
//  }
//
//  public static Type getSingleSelectedType(IStructuredSelection selection)
//      throws DartModelException {
//    Object first = selection.getFirstElement();
//    if (first instanceof Type) {
//      return (Type) first;
//    }
//    if (first instanceof CompilationUnit) {
//      CompilationUnit unit = (CompilationUnit) first;
//      if (unit.exists()) {
//        return JavaElementUtil.getMainType(unit);
//      }
//    }
//    return null;
//  }
//
//  public static Type getTopLevelType(TypeMember[] members) {
//    if (members != null && members.length == 1 && Checks.isTopLevelType(members[0])) {
//      return (Type) members[0];
//    }
//    return null;
//  }
//
//  public static boolean isChangeSignatureAvailable(Method method) throws DartModelException {
//    return Checks.isAvailable(method) && !Flags.isAnnotation(method.getDeclaringType().getFlags());
//  }
//
//  public static boolean isChangeSignatureAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.size() == 1) {
//      if (selection.getFirstElement() instanceof Method) {
//        Method method = (Method) selection.getFirstElement();
//        return isChangeSignatureAvailable(method);
//      }
//    }
//    return false;
//  }
//
//  public static boolean isChangeSignatureAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement[] elements = selection.resolveElementAtOffset();
//    if (elements.length == 1 && elements[0] instanceof Method) {
//      return isChangeSignatureAvailable((Method) elements[0]);
//    }
//    DartElement element = selection.resolveEnclosingElement();
//    return element instanceof Method && isChangeSignatureAvailable((Method) element);
//  }
//
//  public static boolean isCommonDeclaringType(TypeMember[] members) {
//    if (members.length == 0) {
//      return false;
//    }
//    Type type = members[0].getDeclaringType();
//    if (type == null) {
//      return false;
//    }
//    for (int index = 0; index < members.length; index++) {
//      if (!type.equals(members[index].getDeclaringType())) {
//        return false;
//      }
//    }
//    return true;
//  }
//
//  public static boolean isConvertAnonymousAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.size() == 1) {
//      if (selection.getFirstElement() instanceof Type) {
//        return isConvertAnonymousAvailable((Type) selection.getFirstElement());
//      }
//    }
//    return false;
//  }
//
//  public static boolean isConvertAnonymousAvailable(Type type) throws DartModelException {
//    if (Checks.isAvailable(type)) {
//      DartElement element = type.getParent();
//      if (element instanceof Field && JdtFlags.isEnum((TypeMember) element)) {
//        return false;
//      }
//      return type.isAnonymous();
//    }
//    return false;
//  }
//
//  public static boolean isConvertAnonymousAvailable(DartTextSelection selection)
//      throws DartModelException {
//    Type type = RefactoringActions.getEnclosingType(selection);
//    if (type != null) {
//      return RefactoringAvailabilityTester.isConvertAnonymousAvailable(type);
//    }
//    return false;
//  }
//
//  public static boolean isCopyAvailable(IResource[] resources, DartElement[] elements)
//      throws DartModelException {
//    return ReorgPolicyFactory.createCopyPolicy(resources, elements).canEnable();
//  }
//
//  public static boolean isDelegateCreationAvailable(Field field) throws DartModelException {
//    return field.exists()
//        && Flags.isStatic(field.getFlags()) && Flags.isFinal(field.getFlags());
//  }
//
//  public static boolean isDeleteAvailable(DartElement element) {
//    if (!element.exists()) {
//      return false;
//    }
//    if (element instanceof IJavaModel || element instanceof DartProject) {
//      return false;
//    }
//    if (element.getParent() != null && element.getParent().isReadOnly()) {
//      return false;
//    }
//    if (element instanceof IPackageFragmentRoot) {
//      IPackageFragmentRoot root = (IPackageFragmentRoot) element;
//      if (root.isExternal() || Checks.isClasspathDelete(root)) {
//        return false;
//      }
//
//      if (root.getResource().equals(root.getJavaProject().getProject())) {
//        return false;
//      }
//    }
//    if (element.getResource() == null
//        && !RefactoringAvailabilityTester.isWorkingCopyElement(element)) {
//      return false;
//    }
//    if (element instanceof TypeMember && ((TypeMember) element).isBinary()) {
//      return false;
//    }
//    return true;
//  }
//
//  public static boolean isDeleteAvailable(IResource resource) {
//    if (!resource.exists() || resource.isPhantom()) {
//      return false;
//    }
//    if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT) {
//      return false;
//    }
//    return true;
//  }
//
//  public static boolean isDeleteAvailable(IStructuredSelection selection) {
//    if (!selection.isEmpty()) {
//      return isDeleteAvailable(selection.toArray());
//    }
//    return false;
//  }
//
//  public static boolean isDeleteAvailable(Object[] objects) {
//    if (objects.length != 0) {
//      if (ReorgUtils.containsOnlyWorkingSets(Arrays.asList(objects))) {
//        return true;
//      }
//      IResource[] resources = RefactoringAvailabilityTester.getResources(objects);
//      DartElement[] elements = RefactoringAvailabilityTester.getJavaElements(objects);
//
//      if (objects.length != resources.length + elements.length) {
//        return false;
//      }
//      for (int index = 0; index < resources.length; index++) {
//        if (!isDeleteAvailable(resources[index])) {
//          return false;
//        }
//      }
//      for (int index = 0; index < elements.length; index++) {
//        if (!isDeleteAvailable(elements[index])) {
//          return false;
//        }
//      }
//      return true;
//    }
//    return false;
//  }
//
//  public static boolean isExternalizeStringsAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
//      Object element = iter.next();
//      if (element instanceof DartElement) {
//        DartElement javaElement = (DartElement) element;
//        if (javaElement.exists() && !javaElement.isReadOnly()) {
//          int elementType = javaElement.getElementType();
//          if (elementType == DartElement.PACKAGE_FRAGMENT) {
//            return true;
//          } else if (elementType == DartElement.PACKAGE_FRAGMENT_ROOT) {
//            IPackageFragmentRoot root = (IPackageFragmentRoot) javaElement;
//            if (!root.isExternal() && !ReorgUtils.isClassFolder(root)) {
//              return true;
//            }
//          } else if (elementType == DartElement.JAVA_PROJECT) {
//            return true;
//          } else if (elementType == DartElement.COMPILATION_UNIT) {
//            CompilationUnit cu = (CompilationUnit) javaElement;
//            if (cu.exists()) {
//              return true;
//            }
//          } else if (elementType == DartElement.TYPE) {
//            DartElement parent = ((Type) element).getParent();
//            if (parent instanceof CompilationUnit && parent.exists()) {
//              return true;
//            }
//          }
//        }
//      } else if (element instanceof IWorkingSet) {
//        IWorkingSet workingSet = (IWorkingSet) element;
//        return IWorkingSetIDs.JAVA.equals(workingSet.getId());
//      }
//    }
//    return false;
//  }
//
//  public static boolean isExtractConstantAvailable(DartTextSelection selection) {
//    return (selection.resolveInClassInitializer()
//        || selection.resolveInMethodBody()
//        || selection.resolveInVariableInitializer() || selection.resolveInAnnotation())
//        && Checks.isExtractableExpression(
//            selection.resolveSelectedNodes(),
//            selection.resolveCoveringNode());
//  }
//
//  public static boolean isExtractInterfaceAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.size() == 1) {
//      Object first = selection.getFirstElement();
//      if (first instanceof Type) {
//        return isExtractInterfaceAvailable((Type) first);
//      } else if (first instanceof CompilationUnit) {
//        CompilationUnit unit = (CompilationUnit) first;
//        if (!unit.exists() || unit.isReadOnly()) {
//          return false;
//        }
//
//        return true;
//      }
//    }
//    return false;
//  }
//
//  public static boolean isExtractInterfaceAvailable(Type type) throws DartModelException {
//    return Checks.isAvailable(type)
//        && !type.isBinary()
//        && !type.isReadOnly()
//        && !type.isAnnotation()
//        && !type.isAnonymous();
//  }
//
//  public static boolean isExtractInterfaceAvailable(DartTextSelection selection)
//      throws DartModelException {
//    return isExtractInterfaceAvailable(RefactoringActions.getEnclosingOrPrimaryType(selection));
//  }
//
//  public static boolean isExtractMethodAvailable(ASTNode[] nodes) {
//    if (nodes != null && nodes.length != 0) {
//      if (nodes.length == 1) {
//        return nodes[0] instanceof Statement || Checks.isExtractableExpression(nodes[0]);
//      } else {
//        for (int index = 0; index < nodes.length; index++) {
//          if (!(nodes[index] instanceof Statement)) {
//            return false;
//          }
//        }
//        return true;
//      }
//    }
//    return false;
//  }
//
//  public static boolean isExtractMethodAvailable(DartTextSelection selection) {
//    return (selection.resolveInMethodBody() || selection.resolveInClassInitializer() || selection.resolveInVariableInitializer())
//        && !selection.resolveInAnnotation()
//        && RefactoringAvailabilityTester.isExtractMethodAvailable(selection.resolveSelectedNodes());
//  }
//
//  public static boolean isExtractSupertypeAvailable(TypeMember member) throws DartModelException {
//    if (!member.exists()) {
//      return false;
//    }
//    int type = member.getElementType();
//    if (type != DartElement.METHOD && type != DartElement.FIELD && type != DartElement.TYPE) {
//      return false;
//    }
//    if (JdtFlags.isEnum(member) && type != DartElement.TYPE) {
//      return false;
//    }
//    if (!Checks.isAvailable(member)) {
//      return false;
//    }
//    if (member instanceof Method) {
//      Method method = (Method) member;
//      if (method.isConstructor()) {
//        return false;
//      }
//      if (JdtFlags.isNative(method)) {
//        return false;
//      }
//      member = method.getDeclaringType();
//    } else if (member instanceof Field) {
//      member = member.getDeclaringType();
//    }
//    if (member instanceof Type) {
//      if (JdtFlags.isEnum(member) || JdtFlags.isAnnotation(member)) {
//        return false;
//      }
//      if (member.getDeclaringType() != null && !JdtFlags.isStatic(member)) {
//        return false;
//      }
//    }
//    return true;
//  }
//
//  public static boolean isExtractSupertypeAvailable(TypeMember[] members)
//      throws DartModelException {
//    if (members != null && members.length != 0) {
//      Type type = getTopLevelType(members);
//      if (type != null && !type.isClass()) {
//        return false;
//      }
//      for (int index = 0; index < members.length; index++) {
//        if (!isExtractSupertypeAvailable(members[index])) {
//          return false;
//        }
//      }
//      return members.length == 1 || isCommonDeclaringType(members);
//    }
//    return false;
//  }
//
//  public static boolean isExtractSupertypeAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (!selection.isEmpty()) {
//      if (selection.size() == 1) {
//        if (selection.getFirstElement() instanceof CompilationUnit)
//         {
//          return true; // Do not force opening
//        }
//        Type type = getSingleSelectedType(selection);
//        if (type != null) {
//          return Checks.isAvailable(type) && isExtractSupertypeAvailable(new Type[]{type});
//        }
//      }
//      for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
//        if (!(iterator.next() instanceof TypeMember)) {
//          return false;
//        }
//      }
//      Set<TypeMember> members = new HashSet<TypeMember>();
//      @SuppressWarnings("unchecked")
//      List<TypeMember> selectionList =
//          (List<TypeMember>) (List<?>) Arrays.asList(selection.toArray());
//      members.addAll(selectionList);
//      return isExtractSupertypeAvailable(members.toArray(new TypeMember[members.size()]));
//    }
//    return false;
//  }
//
//  public static boolean isExtractSupertypeAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement element = selection.resolveEnclosingElement();
//    if (!(element instanceof TypeMember)) {
//      return false;
//    }
//    return isExtractSupertypeAvailable(new TypeMember[]{(TypeMember) element});
//  }
//
//  public static boolean isExtractTempAvailable(DartTextSelection selection) {
//    ASTNode[] nodes = selection.resolveSelectedNodes();
//    return (selection.resolveInMethodBody() || selection.resolveInClassInitializer())
//        && !selection.resolveInAnnotation()
//        && (Checks.isExtractableExpression(nodes, selection.resolveCoveringNode()) || nodes != null
//            && nodes.length == 1 && nodes[0] instanceof ExpressionStatement);
//  }
//
//  public static boolean isGeneralizeTypeAvailable(DartElement element)
//      throws DartModelException {
//    if (element != null && element.exists()) {
//      String type = null;
//      if (element instanceof Method) {
//        type = ((Method) element).getReturnTypeName();
//      } else if (element instanceof Field) {
//        Field field = (Field) element;
//        if (JdtFlags.isEnum(field)) {
//          return false;
//        }
//        type = field.getTypeSignature();
//      } else if (element instanceof DartVariableDeclaration) {
//        return true;
//      } else if (element instanceof Type) {
//        Type clazz = (Type) element;
//        if (JdtFlags.isEnum(clazz)) {
//          return false;
//        }
//        return true;
//      }
//      if (type == null || PrimitiveType.toCode(Signature.toString(type)) != null) {
//        return false;
//      }
//      return true;
//    }
//    return false;
//  }
//
//  public static boolean isGeneralizeTypeAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.size() == 1) {
//      Object element = selection.getFirstElement();
//      if (element instanceof Method) {
//        Method method = (Method) element;
//        if (!method.exists()) {
//          return false;
//        }
//        String type = method.getReturnTypeName();
//        if (PrimitiveType.toCode(Signature.toString(type)) == null) {
//          return Checks.isAvailable(method);
//        }
//      } else if (element instanceof Field) {
//        Field field = (Field) element;
//        if (!field.exists()) {
//          return false;
//        }
//        if (!JdtFlags.isEnum(field)) {
//          return Checks.isAvailable(field);
//        }
//      }
//    }
//    return false;
//  }
//
//  public static boolean isGeneralizeTypeAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement[] elements = selection.resolveElementAtOffset();
//    if (elements.length != 1) {
//      return false;
//    }
//    return isGeneralizeTypeAvailable(elements[0]);
//  }
//
//  public static boolean isInferTypeArgumentsAvailable(DartElement element)
//      throws DartModelException {
//    if (!Checks.isAvailable(element)) {
//      return false;
//    } else if (element instanceof DartProject) {
//      DartProject project = (DartProject) element;
//      IClasspathEntry[] classpathEntries = project.getRawClasspath();
//      for (int i = 0; i < classpathEntries.length; i++) {
//        if (classpathEntries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
//          return true;
//        }
//      }
//      return false;
//    } else if (element instanceof IPackageFragmentRoot) {
//      return ((IPackageFragmentRoot) element).getKind() == IPackageFragmentRoot.K_SOURCE;
//    } else if (element instanceof IPackageFragment) {
//      return ((IPackageFragment) element).getKind() == IPackageFragmentRoot.K_SOURCE;
//    } else if (element instanceof CompilationUnit) {
//      return true;
//    } else if (element.getAncestor(DartElement.COMPILATION_UNIT) != null) {
//      return true;
//    } else {
//      return false;
//    }
//  }
//
//  public static boolean isInferTypeArgumentsAvailable(DartElement[] elements)
//      throws DartModelException {
//    if (elements.length == 0) {
//      return false;
//    }
//
//    for (int i = 0; i < elements.length; i++) {
//      if (!isInferTypeArgumentsAvailable(elements[i])) {
//        return false;
//      }
//    }
//    return true;
//  }
//
//  public static boolean isInferTypeArgumentsAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.isEmpty()) {
//      return false;
//    }
//
//    for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
//      Object element = iter.next();
//      if (!(element instanceof DartElement)) {
//        return false;
//      }
//      if (element instanceof CompilationUnit) {
//        CompilationUnit unit = (CompilationUnit) element;
//        if (!unit.exists() || unit.isReadOnly()) {
//          return false;
//        }
//
//        return true;
//      }
//      if (!isInferTypeArgumentsAvailable((DartElement) element)) {
//        return false;
//      }
//    }
//    return true;
//  }
//
//  public static boolean isInlineConstantAvailable(Field field) throws DartModelException {
//    return Checks.isAvailable(field)
//        && JdtFlags.isStatic(field)
//        && JdtFlags.isFinal(field)
//        && !JdtFlags.isEnum(field);
//  }
//
//  public static boolean isInlineConstantAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.isEmpty() || selection.size() != 1) {
//      return false;
//    }
//    Object first = selection.getFirstElement();
//    return first instanceof Field && isInlineConstantAvailable((Field) first);
//  }
//
//  public static boolean isInlineConstantAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement[] elements = selection.resolveElementAtOffset();
//    if (elements.length != 1) {
//      return false;
//    }
//    return elements[0] instanceof Field && isInlineConstantAvailable((Field) elements[0]);
//  }
//
//  public static boolean isInlineMethodAvailable(Method method) throws DartModelException {
//    if (method == null) {
//      return false;
//    }
//    if (!method.exists()) {
//      return false;
//    }
//    if (!method.isStructureKnown()) {
//      return false;
//    }
//    if (!method.isBinary()) {
//      return true;
//    }
//    if (method.isConstructor()) {
//      return false;
//    }
//    return SourceRange.isAvailable(method.getNameRange());
//  }
//
//  public static boolean isInlineMethodAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.isEmpty() || selection.size() != 1) {
//      return false;
//    }
//    Object first = selection.getFirstElement();
//    return first instanceof Method && isInlineMethodAvailable((Method) first);
//  }
//
//  public static boolean isInlineMethodAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement[] elements = selection.resolveElementAtOffset();
//    if (elements.length != 1) {
//      DartElement enclosingElement = selection.resolveEnclosingElement();
//      if (!(enclosingElement instanceof TypeMember)) {
//        return false;
//      }
//      ITypeRoot typeRoot = ((TypeMember) enclosingElement).getTypeRoot();
//      CompilationUnit compilationUnit = selection.resolvePartialAstAtOffset();
//      if (compilationUnit == null) {
//        return false;
//      }
//      return getInlineableMethodNode(
//          typeRoot,
//          compilationUnit,
//          selection.getOffset(),
//          selection.getLength()) != null;
//    }
//    DartElement element = elements[0];
//    if (!(element instanceof Method)) {
//      return false;
//    }
//    Method method = (Method) element;
//    if (!isInlineMethodAvailable(method)) {
//      return false;
//    }
//
//    // in binary class, only activate for method declarations
//    DartElement enclosingElement = selection.resolveEnclosingElement();
//    if (enclosingElement == null || enclosingElement.getAncestor(DartElement.CLASS_FILE) == null) {
//      return true;
//    }
//    if (!(enclosingElement instanceof Method)) {
//      return false;
//    }
//    Method enclosingMethod = (Method) enclosingElement;
//    if (enclosingMethod.isConstructor()) {
//      return false;
//    }
//    int nameOffset = enclosingMethod.getNameRange().getOffset();
//    int nameLength = enclosingMethod.getNameRange().getLength();
//    return nameOffset <= selection.getOffset()
//        && selection.getOffset() + selection.getLength() <= nameOffset + nameLength;
//  }
//
//  public static ASTNode getInlineableMethodNode(ITypeRoot typeRoot,
//      CompilationUnit root,
//      int offset,
//      int length) {
//    ASTNode node = null;
//    try {
//      node = getInlineableMethodNode(NodeFinder.perform(root, offset, length, typeRoot), typeRoot);
//    } catch (DartModelException e) {
//      // Do nothing
//    }
//    if (node != null) {
//      return node;
//    }
//    return getInlineableMethodNode(NodeFinder.perform(root, offset, length), typeRoot);
//  }
//
//  private static ASTNode getInlineableMethodNode(ASTNode node, DartElement unit) {
//    if (node == null) {
//      return null;
//    }
//    switch (node.getNodeType()) {
//      case ASTNode.SIMPLE_NAME:
//        StructuralPropertyDescriptor locationInParent = node.getLocationInParent();
//        if (locationInParent == MethodDeclaration.NAME_PROPERTY) {
//          return node.getParent();
//        } else if (locationInParent == MethodInvocation.NAME_PROPERTY
//            || locationInParent == SuperMethodInvocation.NAME_PROPERTY) {
//          return unit instanceof CompilationUnit ? node.getParent() : null; // don't start on invocations in binary
//        }
//        return null;
//      case ASTNode.EXPRESSION_STATEMENT:
//        node = ((ExpressionStatement) node).getExpression();
//    }
//    switch (node.getNodeType()) {
//      case ASTNode.METHOD_DECLARATION:
//        return node;
//      case ASTNode.METHOD_INVOCATION:
//      case ASTNode.SUPER_METHOD_INVOCATION:
//      case ASTNode.CONSTRUCTOR_INVOCATION:
//        return unit instanceof CompilationUnit ? node : null; // don't start on invocations in binary
//    }
//    return null;
//  }
//
//  public static boolean isInlineTempAvailable(DartVariableDeclaration variable)
//      throws DartModelException {
//    return Checks.isAvailable(variable);
//  }
//
//  public static boolean isInlineTempAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement[] elements = selection.resolveElementAtOffset();
//    if (elements.length != 1) {
//      return false;
//    }
//    return elements[0] instanceof DartVariableDeclaration
//        && isInlineTempAvailable((DartVariableDeclaration) elements[0]);
//  }
//
//  public static boolean isIntroduceFactoryAvailable(Method method) throws DartModelException {
//    return Checks.isAvailable(method) && method.isConstructor();
//  }
//
//  public static boolean isIntroduceFactoryAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.size() == 1 && selection.getFirstElement() instanceof Method) {
//      return isIntroduceFactoryAvailable((Method) selection.getFirstElement());
//    }
//    return false;
//  }
//
//  public static boolean isIntroduceFactoryAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement[] elements = selection.resolveElementAtOffset();
//    if (elements.length == 1 && elements[0] instanceof Method) {
//      return isIntroduceFactoryAvailable((Method) elements[0]);
//    }
//
//    // there's no Method for the default constructor
//    if (!Checks.isAvailable(selection.resolveEnclosingElement())) {
//      return false;
//    }
//    ASTNode node = selection.resolveCoveringNode();
//    if (node == null) {
//      ASTNode[] selectedNodes = selection.resolveSelectedNodes();
//      if (selectedNodes != null && selectedNodes.length == 1) {
//        node = selectedNodes[0];
//        if (node == null) {
//          return false;
//        }
//      } else {
//        return false;
//      }
//    }
//
//    if (node.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
//      return true;
//    }
//
//    node = ASTNodes.getNormalizedNode(node);
//    if (node.getLocationInParent() == ClassInstanceCreation.TYPE_PROPERTY) {
//      return true;
//    }
//
//    return false;
//  }
//
//  public static boolean isIntroduceIndirectionAvailable(Method method) throws DartModelException {
//    if (method == null) {
//      return false;
//    }
//    if (!method.exists()) {
//      return false;
//    }
//    if (!method.isStructureKnown()) {
//      return false;
//    }
//    if (method.isConstructor()) {
//      return false;
//    }
//    if (method.getDeclaringType().isAnnotation()) {
//      return false;
//    }
//    if (JavaModelUtil.isPolymorphicSignature(method)) {
//      return false;
//    }
//
//    return true;
//  }
//
//  public static boolean isIntroduceIndirectionAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.isEmpty() || selection.size() != 1) {
//      return false;
//    }
//    Object first = selection.getFirstElement();
//    return first instanceof Method && isIntroduceIndirectionAvailable((Method) first);
//  }
//
//  public static boolean isIntroduceIndirectionAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement[] elements = selection.resolveElementAtOffset();
//    if (elements.length == 1) {
//      return elements[0] instanceof Method
//          && isIntroduceIndirectionAvailable((Method) elements[0]);
//    }
//    ASTNode[] selectedNodes = selection.resolveSelectedNodes();
//    if (selectedNodes == null || selectedNodes.length != 1) {
//      return false;
//    }
//    switch (selectedNodes[0].getNodeType()) {
//      case ASTNode.METHOD_DECLARATION:
//      case ASTNode.METHOD_INVOCATION:
//      case ASTNode.SUPER_METHOD_INVOCATION:
//        return true;
//      default:
//        return false;
//    }
//  }
//
//  public static boolean isIntroduceParameterAvailable(ASTNode[] selectedNodes,
//      ASTNode coveringNode) {
//    return Checks.isExtractableExpression(selectedNodes, coveringNode);
//  }
//
//  public static boolean isIntroduceParameterAvailable(DartTextSelection selection) {
//    return selection.resolveInMethodBody()
//        && !selection.resolveInAnnotation()
//        && isIntroduceParameterAvailable(
//            selection.resolveSelectedNodes(),
//            selection.resolveCoveringNode());
//  }
//
//  public static boolean isMoveAvailable(IResource[] resources, DartElement[] elements)
//      throws DartModelException {
//    if (elements != null) {
//      for (int index = 0; index < elements.length; index++) {
//        DartElement element = elements[index];
//        if (element == null || !element.exists()) {
//          return false;
//        }
//        if (element instanceof Type && ((Type) element).isLocal()) {
//          return false;
//        }
//        if (element instanceof IPackageDeclaration) {
//          return false;
//        }
//        if (element instanceof Field && JdtFlags.isEnum((TypeMember) element)) {
//          return false;
//        }
//      }
//    }
//    return ReorgPolicyFactory.createMovePolicy(resources, elements).canEnable();
//  }
//
//  public static boolean isMoveAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement element = selection.resolveEnclosingElement();
//    if (element == null) {
//      return false;
//    }
//    return isMoveAvailable(new IResource[0], new DartElement[]{element});
//  }
//
//  public static boolean isMoveInnerAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.size() == 1) {
//      Object first = selection.getFirstElement();
//      if (first instanceof Type) {
//        return isMoveInnerAvailable((Type) first);
//      }
//    }
//    return false;
//  }
//
//  public static boolean isMoveInnerAvailable(Type type) throws DartModelException {
//    return Checks.isAvailable(type)
//        && !Checks.isAnonymous(type)
//        && !JavaElementUtil.isMainType(type)
//        && !Checks.isInsideLocalType(type);
//  }
//
//  public static boolean isMoveInnerAvailable(DartTextSelection selection)
//      throws DartModelException {
//    Type type = RefactoringAvailabilityTester.getDeclaringType(selection.resolveEnclosingElement());
//    if (type == null) {
//      return false;
//    }
//    return isMoveInnerAvailable(type);
//  }
//
//  public static boolean isMoveMethodAvailable(Method method) throws DartModelException {
//    return method.exists()
//        && !method.isConstructor()
//        && !method.isBinary()
//        && !method.getDeclaringType().isInterface()
//        && !method.isReadOnly()
//        && !JdtFlags.isStatic(method);
//  }
//
//  public static boolean isMoveMethodAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.size() == 1) {
//      Object first = selection.getFirstElement();
//      return first instanceof Method && isMoveMethodAvailable((Method) first);
//    }
//    return false;
//  }
//
//  public static boolean isMoveMethodAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement method = selection.resolveEnclosingElement();
//    if (!(method instanceof Method)) {
//      return false;
//    }
//    return isMoveMethodAvailable((Method) method);
//  }
//
//  public static boolean isMoveStaticAvailable(TypeMember member) throws DartModelException {
//    if (!member.exists()) {
//      return false;
//    }
//    int type = member.getElementType();
//    if (type != DartElement.METHOD && type != DartElement.FIELD && type != DartElement.TYPE) {
//      return false;
//    }
//    if (JdtFlags.isEnum(member) && type != DartElement.TYPE) {
//      return false;
//    }
//    Type declaring = member.getDeclaringType();
//    if (declaring == null) {
//      return false;
//    }
//    if (!Checks.isAvailable(member)) {
//      return false;
//    }
//    if (type == DartElement.METHOD && declaring.isInterface()) {
//      return false;
//    }
//    if (type == DartElement.METHOD && !JdtFlags.isStatic(member)) {
//      return false;
//    }
//    if (type == DartElement.METHOD && ((Method) member).isConstructor()) {
//      return false;
//    }
//    if (type == DartElement.TYPE && !JdtFlags.isStatic(member)) {
//      return false;
//    }
//    if (!declaring.isInterface() && !JdtFlags.isStatic(member)) {
//      return false;
//    }
//    return true;
//  }
//
//  public static boolean isMoveStaticAvailable(TypeMember[] members) throws DartModelException {
//    for (int index = 0; index < members.length; index++) {
//      if (!isMoveStaticAvailable(members[index])) {
//        return false;
//      }
//    }
//    return true;
//  }
//
//  public static boolean isMoveStaticAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement element = selection.resolveEnclosingElement();
//    if (!(element instanceof TypeMember)) {
//      return false;
//    }
//    return RefactoringAvailabilityTester.isMoveStaticMembersAvailable(new TypeMember[]{(TypeMember) element});
//  }
//
//  public static boolean isMoveStaticMembersAvailable(TypeMember[] members)
//      throws DartModelException {
//    if (members == null) {
//      return false;
//    }
//    if (members.length == 0) {
//      return false;
//    }
//    if (!isMoveStaticAvailable(members)) {
//      return false;
//    }
//    if (!isCommonDeclaringType(members)) {
//      return false;
//    }
//    return true;
//  }
//
//  public static boolean isPromoteTempAvailable(DartVariableDeclaration variable)
//      throws DartModelException {
//    return Checks.isAvailable(variable);
//  }
//
//  public static boolean isPromoteTempAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement[] elements = selection.resolveElementAtOffset();
//    if (elements.length != 1) {
//      return false;
//    }
//    return elements[0] instanceof DartVariableDeclaration
//        && isPromoteTempAvailable((DartVariableDeclaration) elements[0]);
//  }
//
//  public static boolean isPullUpAvailable(TypeMember member) throws DartModelException {
//    if (!member.exists()) {
//      return false;
//    }
//    int type = member.getElementType();
//    if (type != DartElement.METHOD && type != DartElement.FIELD && type != DartElement.TYPE) {
//      return false;
//    }
//    if (JdtFlags.isEnum(member) && type != DartElement.TYPE) {
//      return false;
//    }
//    if (!Checks.isAvailable(member)) {
//      return false;
//    }
//    if (member instanceof Type) {
//      if (!JdtFlags.isStatic(member) && !JdtFlags.isEnum(member) && !JdtFlags.isAnnotation(member)) {
//        return false;
//      }
//    }
//    if (member instanceof Method) {
//      Method method = (Method) member;
//      if (method.isConstructor()) {
//        return false;
//      }
//      if (JdtFlags.isNative(method)) {
//        return false;
//      }
//      Type declaring = method.getDeclaringType();
//      if (declaring != null && declaring.isAnnotation()) {
//        return false;
//      }
//    }
//    return true;
//  }
//
//  public static boolean isPullUpAvailable(TypeMember[] members) throws DartModelException {
//    if (members != null && members.length != 0) {
//      Type type = getTopLevelType(members);
//      if (type != null && getPullUpMembers(type).length != 0) {
//        return true;
//      }
//      for (int index = 0; index < members.length; index++) {
//        if (!isPullUpAvailable(members[index])) {
//          return false;
//        }
//      }
//      return isCommonDeclaringType(members);
//    }
//    return false;
//  }
//
//  public static boolean isPullUpAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (!selection.isEmpty()) {
//      if (selection.size() == 1) {
//        if (selection.getFirstElement() instanceof CompilationUnit)
//         {
//          return true; // Do not force opening
//        }
//        Type type = getSingleSelectedType(selection);
//        if (type != null) {
//          return Checks.isAvailable(type) && isPullUpAvailable(new Type[]{type});
//        }
//      }
//      for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
//        if (!(iterator.next() instanceof TypeMember)) {
//          return false;
//        }
//      }
//      Set<TypeMember> members = new HashSet<TypeMember>();
//      @SuppressWarnings("unchecked")
//      List<TypeMember> selectionList =
//          (List<TypeMember>) (List<?>) Arrays.asList(selection.toArray());
//      members.addAll(selectionList);
//      return isPullUpAvailable(members.toArray(new TypeMember[members.size()]));
//    }
//    return false;
//  }
//
//  public static boolean isPullUpAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement element = selection.resolveEnclosingElement();
//    if (!(element instanceof TypeMember)) {
//      return false;
//    }
//    return isPullUpAvailable(new TypeMember[]{(TypeMember) element});
//  }
//
//  public static boolean isPushDownAvailable(TypeMember member) throws DartModelException {
//    if (!member.exists()) {
//      return false;
//    }
//    int type = member.getElementType();
//    if (type != DartElement.METHOD && type != DartElement.FIELD) {
//      return false;
//    }
//    if (JdtFlags.isEnum(member)) {
//      return false;
//    }
//    if (!Checks.isAvailable(member)) {
//      return false;
//    }
//    if (JdtFlags.isStatic(member)) {
//      return false;
//    }
//    if (type == DartElement.METHOD) {
//      Method method = (Method) member;
//      if (method.isConstructor()) {
//        return false;
//      }
//      if (JdtFlags.isNative(method)) {
//        return false;
//      }
//      Type declaring = method.getDeclaringType();
//      if (declaring != null && declaring.isAnnotation()) {
//        return false;
//      }
//    }
//    return true;
//  }
//
//  public static boolean isPushDownAvailable(TypeMember[] members) throws DartModelException {
//    if (members != null && members.length != 0) {
//      Type type = getTopLevelType(members);
//      if (type != null && RefactoringAvailabilityTester.getPushDownMembers(type).length != 0) {
//        return true;
//      }
//      if (type != null && JdtFlags.isEnum(type)) {
//        return false;
//      }
//      for (int index = 0; index < members.length; index++) {
//        if (!isPushDownAvailable(members[index])) {
//          return false;
//        }
//      }
//      return isCommonDeclaringType(members);
//    }
//    return false;
//  }
//
//  public static boolean isPushDownAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (!selection.isEmpty()) {
//      if (selection.size() == 1) {
//        if (selection.getFirstElement() instanceof CompilationUnit)
//         {
//          return true; // Do not force opening
//        }
//        Type type = getSingleSelectedType(selection);
//        if (type != null) {
//          return isPushDownAvailable(new Type[]{type});
//        }
//      }
//      for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
//        if (!(iterator.next() instanceof TypeMember)) {
//          return false;
//        }
//      }
//      Set<TypeMember> members = new HashSet<TypeMember>();
//      @SuppressWarnings("unchecked")
//      List<TypeMember> selectionList =
//          (List<TypeMember>) (List<?>) Arrays.asList(selection.toArray());
//      members.addAll(selectionList);
//      return isPushDownAvailable(members.toArray(new TypeMember[members.size()]));
//    }
//    return false;
//  }
//
//  public static boolean isPushDownAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement element = selection.resolveEnclosingElement();
//    if (!(element instanceof TypeMember)) {
//      return false;
//    }
//    return isPullUpAvailable(new TypeMember[]{(TypeMember) element});
//  }

  public static boolean isRenameAvailable(CompilationUnit unit) {
    if (unit == null) {
      return false;
    }
    if (!unit.exists()) {
      return false;
    }
    if (!DartModelUtil.isPrimary(unit)) {
      return false;
    }
    if (unit.isReadOnly()) {
      return false;
    }
    return true;
  }

  public static boolean isRenameAvailable(DartProject project) throws DartModelException {
    if (project == null) {
      return false;
    }
    if (!Checks.isAvailable(project)) {
      return false;
    }
    if (!project.isConsistent()) {
      return false;
    }
    return true;
  }

  public static boolean isRenameAvailable(DartVariableDeclaration variable)
      throws DartModelException {
    return Checks.isAvailable(variable);
  }

  public static boolean isRenameAvailable(IResource resource) {
    if (resource == null) {
      return false;
    }
    if (!resource.exists()) {
      return false;
    }
    if (!resource.isAccessible()) {
      return false;
    }
    return true;
  }

//  public static boolean isRenameAvailable(IPackageFragment fragment)
//      throws DartModelException {
//    if (fragment == null) {
//      return false;
//    }
//    if (!Checks.isAvailable(fragment)) {
//      return false;
//    }
//    if (fragment.isDefaultPackage()) {
//      return false;
//    }
//    return true;
//  }
//
//  public static boolean isRenameAvailable(IPackageFragmentRoot root)
//      throws DartModelException {
//    if (root == null) {
//      return false;
//    }
//    if (!Checks.isAvailable(root)) {
//      return false;
//    }
//    if (root.isArchive()) {
//      return false;
//    }
//    if (root.isExternal()) {
//      return false;
//    }
//    if (!root.isConsistent()) {
//      return false;
//    }
//    if (root.getResource() instanceof IProject) {
//      return false;
//    }
//    return true;
//  }

  public static boolean isRenameAvailable(Method method) throws CoreException {
    if (method == null) {
      return false;
    }
    if (!Checks.isAvailable(method)) {
      return false;
    }
    if (method.isConstructor()) {
      return false;
    }
    if (isRenameProhibited(method)) {
      return false;
    }
    return true;
  }

  public static boolean isRenameAvailable(Type type) throws DartModelException {
    if (type == null) {
      return false;
    }
    if (!Checks.isAvailable(type)) {
      return false;
    }
    if (isRenameProhibited(type)) {
      return false;
    }
    return true;
  }

//  public static boolean isRenameAvailable(ITypeParameter parameter) throws DartModelException {
//    return Checks.isAvailable(parameter);
//  }

  public static boolean isRenameElementAvailable(DartElement element) throws CoreException {
    switch (element.getElementType()) {
      case DartElement.DART_PROJECT:
        return isRenameAvailable((DartProject) element);
//      case DartElement.PACKAGE_FRAGMENT_ROOT:
//        return isRenameAvailable((IPackageFragmentRoot) element);
//      case DartElement.PACKAGE_FRAGMENT:
//        return isRenameAvailable((IPackageFragment) element);
      case DartElement.COMPILATION_UNIT:
        return isRenameAvailable((CompilationUnit) element);
      case DartElement.TYPE:
        return isRenameAvailable((Type) element);
      case DartElement.METHOD:
        Method method = (Method) element;
        if (method.isConstructor()) {
          return isRenameAvailable(method.getDeclaringType());
        } else {
          return isRenameAvailable(method);
        }
      case DartElement.FIELD:
        Field field = (Field) element;
        return isRenameFieldAvailable(field);
//      case DartElement.TYPE_PARAMETER:
//        return isRenameAvailable((ITypeParameter) element);
      case DartElement.VARIABLE:
        return isRenameAvailable((DartVariableDeclaration) element);
    }
    return false;
  }

  public static boolean isRenameFieldAvailable(Field field) throws DartModelException {
    return Checks.isAvailable(field);
  }

  public static boolean isRenameNonVirtualMethodAvailable(Method method) throws DartModelException,
      CoreException {
    // TODO(scheglov) virtual?
    return isRenameAvailable(method);
    //return isRenameAvailable(method) && !MethodChecks.isVirtual(method);
  }

  public static boolean isRenameProhibited(Method method) throws CoreException {
    if (method.getElementName().equals("toString") //$NON-NLS-1$
        && method.getParameterNames().length == 0
        && (method.getReturnTypeName().equals("Ljava.lang.String;") //$NON-NLS-1$
            || method.getReturnTypeName().equals("QString;") //$NON-NLS-1$
        || method.getReturnTypeName().equals("Qjava.lang.String;"))) {
      return true;
    } else {
      return false;
    }
  }

  public static boolean isRenameProhibited(Type type) {
    return false;
    //return type.getPackageFragment().getElementName().equals("java.lang"); //$NON-NLS-1$
  }

  public static boolean isRenameVirtualMethodAvailable(Method method) throws CoreException {
    // TODO(scheglov) virtual?
    return isRenameAvailable(method);
    //return isRenameAvailable(method) && MethodChecks.isVirtual(method);
  }

//  public static boolean isReplaceInvocationsAvailable(Method method) throws DartModelException {
//    if (method == null) {
//      return false;
//    }
//    if (!method.exists()) {
//      return false;
//    }
//    if (method.isConstructor()) {
//      return false;
//    }
//    return true;
//  }
//
//  public static boolean isReplaceInvocationsAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.isEmpty() || selection.size() != 1) {
//      return false;
//    }
//    Object first = selection.getFirstElement();
//    return first instanceof Method && isReplaceInvocationsAvailable((Method) first);
//  }
//
//  public static boolean isReplaceInvocationsAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement[] elements = selection.resolveElementAtOffset();
//    if (elements.length != 1) {
//      return false;
//    }
//    DartElement element = elements[0];
//    return element instanceof Method && isReplaceInvocationsAvailable((Method) element);
//  }
//
//  public static boolean isSelfEncapsulateAvailable(Field field) throws DartModelException {
//    return Checks.isAvailable(field)
//        && !JdtFlags.isEnum(field)
//        && !field.getDeclaringType().isAnnotation();
//  }
//
//  public static boolean isSelfEncapsulateAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.size() == 1) {
//      if (selection.getFirstElement() instanceof Field) {
//        Field field = (Field) selection.getFirstElement();
//        return Checks.isAvailable(field) && !JdtFlags.isEnum(field);
//      }
//    }
//    return false;
//  }
//
//  public static boolean isSelfEncapsulateAvailable(DartTextSelection selection)
//      throws DartModelException {
//    DartElement[] elements = selection.resolveElementAtOffset();
//    if (elements.length != 1) {
//      return false;
//    }
//    return elements[0] instanceof Field && isSelfEncapsulateAvailable((Field) elements[0]);
//  }
//
//  public static boolean isUseSuperTypeAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    if (selection.size() == 1) {
//      Object first = selection.getFirstElement();
//      if (first instanceof Type) {
//        return isUseSuperTypeAvailable((Type) first);
//      } else if (first instanceof CompilationUnit) {
//        CompilationUnit unit = (CompilationUnit) first;
//        if (!unit.exists() || unit.isReadOnly()) {
//          return false;
//        }
//
//        return true;
//      }
//    }
//    return false;
//  }
//
//  public static boolean isUseSuperTypeAvailable(Type type) throws DartModelException {
//    return type != null && type.exists() && !type.isAnnotation() && !type.isAnonymous();
//  }
//
//  public static boolean isUseSuperTypeAvailable(DartTextSelection selection)
//      throws DartModelException {
//    return isUseSuperTypeAvailable(RefactoringActions.getEnclosingOrPrimaryType(selection));
//  }
//
//  public static boolean isWorkingCopyElement(DartElement element) {
//    if (element instanceof CompilationUnit) {
//      return ((CompilationUnit) element).isWorkingCopy();
//    }
//    if (ReorgUtils.isInsideCompilationUnit(element)) {
//      return ReorgUtils.getCompilationUnit(element).isWorkingCopy();
//    }
//    return false;
//  }
//
//  private RefactoringAvailabilityTester() {
//    // Not for instantiation
//  }
//
//  public static boolean isIntroduceParameterObjectAvailable(IStructuredSelection selection)
//      throws DartModelException {
//    return isChangeSignatureAvailable(selection); //TODO test selected element for more than 1 parameter?
//  }
//
//  public static boolean isIntroduceParameterObjectAvailable(DartTextSelection selection)
//      throws DartModelException {
//    return isChangeSignatureAvailable(selection); //TODO test selected element for more than 1 parameter?
//  }
//
//  public static boolean isExtractClassAvailable(Type type) throws DartModelException {
//    if (type == null) {
//      return false;
//    }
//    if (!type.exists()) {
//      return false;
//    }
//    return ReorgUtils.isInsideCompilationUnit(type) && type.isClass() && !type.isAnonymous();
//  }
}
