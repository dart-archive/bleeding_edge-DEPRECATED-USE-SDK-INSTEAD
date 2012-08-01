package com.google.dart.tools.internal.corext.refactoring.util;

import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceReference;

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

  public static boolean isSourceAvailable(SourceReference sourceReference) {
    try {
      return SourceRangeUtils.isAvailable(sourceReference.getSourceRange());
    } catch (DartModelException e) {
      return false;
    }
  }

//  public static IMember[] merge(IMember[] a1, IMember[] a2) {
//    // Don't use hash sets since ordering is important for some refactorings.
//    List<IMember> result = new ArrayList<IMember>(a1.length + a2.length);
//    for (int i = 0; i < a1.length; i++) {
//      IMember member = a1[i];
//      if (!result.contains(member)) {
//        result.add(member);
//      }
//    }
//    for (int i = 0; i < a2.length; i++) {
//      IMember member = a2[i];
//      if (!result.contains(member)) {
//        result.add(member);
//      }
//    }
//    return result.toArray(new IMember[result.size()]);
//  }
//
//  public static IMember[] sortByOffset(IMember[] members) {
//    Comparator<IMember> comparator = new Comparator<IMember>() {
//      public int compare(IMember o1, IMember o2) {
//        try {
//          return o1.getNameRange().getOffset() - o2.getNameRange().getOffset();
//        } catch (JavaModelException e) {
//          return 0;
//        }
//      }
//    };
//    Arrays.sort(members, comparator);
//    return members;
//  }
//
//  private static boolean isCuOnlyType(IType type) throws JavaModelException {
//    return type.getCompilationUnit().getTypes().length == 1;
//  }
//
//  private static boolean isPrimaryType(IType type) {
//    return type.equals(type.getCompilationUnit().findPrimaryType());
//  }
//
//  //no instances
//  private JavaElementUtil() {
//  }
}
