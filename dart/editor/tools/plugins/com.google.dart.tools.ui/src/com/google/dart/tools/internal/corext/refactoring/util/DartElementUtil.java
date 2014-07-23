package com.google.dart.tools.internal.corext.refactoring.util;

import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.internal.element.member.Member;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceReference;

import org.eclipse.core.resources.IFile;

public class DartElementUtil {

//  public static String createFieldSignature(IField field) {
//    return BasicElementLabels.getJavaElementName(field.getDeclaringType().getFullyQualifiedName('.')
//        + "." + field.getElementName()); //$NON-NLS-1$
//  }
//
//  public static String createMethodSignature(IMethod method) {
//    try {
//      return BasicElementLabels.getJavaElementName(Signature.toString(
//          method.getSignature(),
//          method.getElementName(),
//          method.getParameterNames(),
//          false,
//          !method.isConstructor()));
//    } catch (JavaModelException e) {
//      return BasicElementLabels.getJavaElementName(method.getElementName()); //fallback
//    }
//  }
//
//  public static String createSignature(IMember member) {
//    switch (member.getElementType()) {
//      case IJavaElement.FIELD:
//        return createFieldSignature((IField) member);
//      case IJavaElement.TYPE:
//        return BasicElementLabels.getJavaElementName(((IType) member).getFullyQualifiedName('.'));
//      case IJavaElement.INITIALIZER:
//        return RefactoringCoreMessages.JavaElementUtil_initializer;
//      case IJavaElement.METHOD:
//        return createMethodSignature((IMethod) member);
//      default:
//        Assert.isTrue(false);
//        return null;
//    }
//  }
//
//  public static IMethod[] getAllConstructors(IType type) throws JavaModelException {
//    if (JavaModelUtil.isInterfaceOrAnnotation(type)) {
//      return new IMethod[0];
//    }
//    List<IMethod> result = new ArrayList<IMethod>();
//    IMethod[] methods = type.getMethods();
//    for (int i = 0; i < methods.length; i++) {
//      IMethod iMethod = methods[i];
//      if (iMethod.isConstructor()) {
//        result.add(iMethod);
//      }
//    }
//    return result.toArray(new IMethod[result.size()]);
//  }
//
//  public static IJavaElement[] getElementsOfType(IJavaElement[] elements, int type) {
//    Set<IJavaElement> result = new HashSet<IJavaElement>(elements.length);
//    for (int i = 0; i < elements.length; i++) {
//      IJavaElement element = elements[i];
//      if (element.getElementType() == type) {
//        result.add(element);
//      }
//    }
//    return result.toArray(new IJavaElement[result.size()]);
//  }
//
//  public static IType getMainType(ICompilationUnit cu) throws JavaModelException {
//    IType[] types = cu.getTypes();
//    for (int i = 0; i < types.length; i++) {
//      if (isMainType(types[i])) {
//        return types[i];
//      }
//    }
//    return null;
//  }
//
//  /**
//   * @param pack a package fragment
//   * @return an array containing the given package and all subpackages
//   * @throws JavaModelException if getting the all sibling packages fails
//   */
//  public static IPackageFragment[] getPackageAndSubpackages(IPackageFragment pack)
//      throws JavaModelException {
//    if (pack.isDefaultPackage()) {
//      return new IPackageFragment[] {pack};
//    }
//
//    IPackageFragmentRoot root = (IPackageFragmentRoot) pack.getParent();
//    IJavaElement[] allPackages = root.getChildren();
//    ArrayList<IPackageFragment> subpackages = new ArrayList<IPackageFragment>();
//    subpackages.add(pack);
//    String prefix = pack.getElementName() + '.';
//    for (int i = 0; i < allPackages.length; i++) {
//      IPackageFragment currentPackage = (IPackageFragment) allPackages[i];
//      if (currentPackage.getElementName().startsWith(prefix)) {
//        subpackages.add(currentPackage);
//      }
//    }
//    return subpackages.toArray(new IPackageFragment[subpackages.size()]);
//  }
//
//  /**
//   * @param pack the package fragment; may not be null
//   * @return the parent package fragment, or null if the given package fragment is the default
//   *         package or a top level package
//   */
//  public static IPackageFragment getParentSubpackage(IPackageFragment pack) {
//    if (pack.isDefaultPackage()) {
//      return null;
//    }
//
//    final int index = pack.getElementName().lastIndexOf('.');
//    if (index == -1) {
//      return null;
//    }
//
//    final IPackageFragmentRoot root = (IPackageFragmentRoot) pack.getParent();
//    final String newPackageName = pack.getElementName().substring(0, index);
//    final IPackageFragment parent = root.getPackageFragment(newPackageName);
//    if (parent.exists()) {
//      return parent;
//    } else {
//      return null;
//    }
//  }
//
//  /**
//   * @param root the package fragment root
//   * @return array of projects that have the specified root on their classpath
//   * @throws JavaModelException if getting the raw classpath or all Java projects fails
//   */
//  public static IJavaProject[] getReferencingProjects(IPackageFragmentRoot root)
//      throws JavaModelException {
//    IClasspathEntry cpe = root.getRawClasspathEntry();
//    if (cpe.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
//      cpe = root.getResolvedClasspathEntry();
//    }
//    IJavaProject[] allJavaProjects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
//    List<IJavaProject> result = new ArrayList<IJavaProject>(allJavaProjects.length);
//    for (int i = 0; i < allJavaProjects.length; i++) {
//      IJavaProject project = allJavaProjects[i];
//      IPackageFragmentRoot[] roots = project.findPackageFragmentRoots(cpe);
//      if (roots.length > 0) {
//        result.add(project);
//      }
//    }
//    return result.toArray(new IJavaProject[result.size()]);
//  }
//
//  /* @see org.eclipse.jdt.internal.core.JavaElement#isAncestorOf(org.eclipse.jdt.core.IJavaElement) */
//  public static boolean isAncestorOf(IJavaElement ancestor, IJavaElement child) {
//    IJavaElement parent = child.getParent();
//    while (parent != null && !parent.equals(ancestor)) {
//      parent = parent.getParent();
//    }
//    return parent != null;
//  }
//
//  public static boolean isDefaultPackage(Object element) {
//    return (element instanceof IPackageFragment) && ((IPackageFragment) element).isDefaultPackage();
//  }
//
//  public static boolean isMainType(IType type) throws JavaModelException {
//    if (!type.exists()) {
//      return false;
//    }
//
//    if (type.isBinary()) {
//      return false;
//    }
//
//    if (type.getCompilationUnit() == null) {
//      return false;
//    }
//
//    if (type.getDeclaringType() != null) {
//      return false;
//    }
//
//    return isPrimaryType(type) || isCuOnlyType(type);
//  }

  /**
   * Each synthetic {@link PropertyInducingElement} has either getter or setter
   * {@link PropertyAccessorElement}. When we want to open such {@link PropertyInducingElement}, we
   * actually should open one of the {@link PropertyAccessorElement}.
   * 
   * @return the given {@link Element} or its {@link PropertyAccessorElement}.
   */
  public static Element getAccessorIfSyntheticVariable(Element element) {
    if (element instanceof PropertyInducingElement) {
      PropertyInducingElement variable = (PropertyInducingElement) element;
      if (variable.isSynthetic()) {
        // first try setter, because it is usually used in Angular
        PropertyAccessorElement setter = variable.getSetter();
        if (setter != null) {
          return setter;
        }
        // try getter
        PropertyAccessorElement getter = variable.getSetter();
        if (getter != null) {
          return getter;
        }
      }
    }
    return element;
  }

  /**
   * @return the given {@link Element} or, its "base" {@link Element} if {@link Member}.
   */
  public static Element getBaseIfMember(Element element) {
    if (element instanceof Member) {
      return ((Member) element).getBaseElement();
    }
    return element;
  }

  /**
   * There is nothing to open for synthetic default constructor. We can show {@link ClassElement}
   * instead.
   * 
   * @return the given {@link Element} or {@link ClassElement}.
   */
  public static Element getClassIfSyntheticDefaultConstructor(Element element) {
    if (element instanceof ConstructorElement) {
      ConstructorElement constructor = (ConstructorElement) element;
      if (constructor.isSynthetic()) {
        return constructor.getEnclosingElement();
      }
    }
    return element;
  }

  /**
   * @return the {@link CompilationUnitElement} of the given {@link IFile}, may be {@code null}.
   */
  public static CompilationUnitElement getCompilationUnitElement(IFile file) {
    // prepare context
    ProjectManager projectManager = DartCore.getProjectManager();
    Source source = projectManager.getSource(file);
    AnalysisContext context = projectManager.getContext(file);
    if (source == null || context == null) {
      return null;
    }
    // prepare library
    Source[] librarySources = context.getLibrariesContaining(source);
    if (librarySources.length != 1) {
      return null;
    }
    // get unit element
    return context.getCompilationUnitElement(source, librarySources[0]);
  }

  /**
   * Synthetic implicit constructors of {@link ClassTypeAlias} use implementations of constructors
   * from superclass.
   * 
   * @return the given {@link Element} or implementation {@link ConstructorElement}.
   */
  public static Element getExplicitIfSyntheticImplicitConstructor(Element element) {
    if (element instanceof ConstructorElement) {
      ConstructorElement constructor = (ConstructorElement) element;
      if (constructor.isSynthetic()) {
        ConstructorElement redirectedConstructor = constructor.getRedirectedConstructor();
        if (redirectedConstructor != null) {
          return redirectedConstructor;
        }
      }
    }
    return element;
  }

  /**
   * @return the given {@link Element} or, its {@link FieldElement} if
   *         {@link FieldFormalParameterElement}.
   */
  public static Element getFieldIfFieldFormalParameter(Element element) {
    if (element instanceof FieldFormalParameterElement) {
      FieldFormalParameterElement fieldParameterElement = (FieldFormalParameterElement) element;
      element = fieldParameterElement.getField();
    }
    return element;
  }

  /**
   * @return the given {@link Element} or, its {@link PropertyInducingElement} if
   *         {@link PropertyAccessorElement}.
   */
  public static Element getVariableIfAccessor(Element element) {
    if (element instanceof PropertyAccessorElement) {
      PropertyAccessorElement accessorElement = (PropertyAccessorElement) element;
      element = accessorElement.getVariable();
    }
    return element;
  }

  /**
   * Each {@link PropertyInducingElement} has synthetic read and (may be) write
   * {@link PropertyAccessorElement}. When we want to open or rename such
   * {@link PropertyAccessorElement}, we actually should do this on original
   * {@link PropertyInducingElement}.
   * 
   * @return the given {@link Element} or its {@link PropertyInducingElement}.
   */
  public static Element getVariableIfSyntheticAccessor(Element element) {
    if (element instanceof PropertyAccessorElement) {
      PropertyAccessorElement accessorElement = (PropertyAccessorElement) element;
      if (accessorElement.isSynthetic()) {
        element = accessorElement.getVariable();
      }
    }
    return element;
  }

  public static boolean isSourceAvailable(SourceReference sourceReference) {
    try {
      return SourceRangeUtils.isAvailable(sourceReference.getSourceRange());
    } catch (DartModelException e) {
      return false;
    }
  }
}
