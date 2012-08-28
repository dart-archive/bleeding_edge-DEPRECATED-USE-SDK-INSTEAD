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
package com.google.dart.tools.ui.internal.util;

import com.google.common.collect.Sets;
import com.google.dart.core.ILocalVariable;
import com.google.dart.core.IPackageFragment;
import com.google.dart.core.util.MethodOverrideTester;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.util.CharOperation;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeHierarchy;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.SuperTypeHierarchyCache;
import com.google.dart.tools.ui.internal.text.ValidateEditException;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.RewriteSessionEditProcessor;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility methods for the Dart Model.
 */
public final class DartModelUtil {

  /**
   * Only use this suffix for creating new .dart files. In general, use one of the three
   * *JavaLike*(..) methods in JavaScriptCore or create a name from an existing compilation unit
   * with {@link #getRenamedCUName(CompilationUnit, String)}
   * <p>
   * Note: Unlike {@link JavaScriptCore#getJavaScriptLikeExtensions()}, this suffix includes a
   * leading ".".
   * </p>
   * 
   * @see JavaScriptCore#getJavaScriptLikeExtensions()
   * @see JavaScriptCore#isJavaScriptLikeFileName(String)
   * @see JavaScriptCore#removeJavaScriptLikeExtension(String)
   * @see #getRenamedCUName(CompilationUnit, String)
   */
  public static final String DEFAULT_CU_SUFFIX = ".dart"; //$NON-NLS-1$

  /** The name of the boolean type. */
  public static final String TYPE_BOOL = "bool";

  /**
   * Applies an text edit to a compilation unit. Filed bug 117694 against jdt.core.
   * 
   * @param cu the compilation unit to apply the edit to
   * @param edit the edit to apply
   * @param save is set, save the CU after the edit has been applied
   * @param monitor the progress monitor to use
   * @throws CoreException Thrown when the access to the CU failed
   * @throws ValidateEditException if validate edit fails
   */
  public static void applyEdit(CompilationUnit cu, TextEdit edit, boolean save,
      IProgressMonitor monitor) throws CoreException, ValidateEditException {
    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }
    monitor.beginTask(CorextMessages.JavaModelUtil_applyedit_operation, 3);

    try {
      IDocument document = null;
      try {
        document = aquireDocument(cu, new SubProgressMonitor(monitor, 1));
        if (save) {
          commitDocument(cu, document, edit, new SubProgressMonitor(monitor, 1));
        } else {
          new RewriteSessionEditProcessor(document, edit, TextEdit.UPDATE_REGIONS).performEdits();
        }
      } catch (BadLocationException e) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartToolsPlugin.PLUGIN_ID,
            IStatus.ERROR,
            e.getMessage(),
            e));
      } finally {
        releaseDocument(cu, document, new SubProgressMonitor(monitor, 1));
      }
    } finally {
      monitor.done();
    }
  }

  /**
   * Concatenates two names. Uses a dot for separation. Both strings can be empty or
   * <code>null</code>.
   */
  public static String concatenateName(char[] name1, char[] name2) {
    StringBuffer buf = new StringBuffer();
    if (name1 != null && name1.length > 0) {
      buf.append(name1);
    }
    if (name2 != null && name2.length > 0) {
      if (buf.length() > 0) {
        buf.append('.');
      }
      buf.append(name2);
    }
    return buf.toString();
  }

  /**
   * Concatenates two names. Uses a dot for separation. Both strings can be empty or
   * <code>null</code>.
   */
  public static String concatenateName(String name1, String name2) {
    StringBuffer buf = new StringBuffer();
    if (name1 != null && name1.length() > 0) {
      buf.append(name1);
    }
    if (name2 != null && name2.length() > 0) {
      if (buf.length() > 0) {
        buf.append('.');
      }
      buf.append(name2);
    }
    return buf.toString();
  }

  /**
   * Finds an entry in a container. <code>null</code> is returned if the entry can not be found
   * 
   * @param container The container
   * @param libPath The path of the library to be found
   * @return IIncludePathEntry A classpath entry from the container of <code>null</code> if the
   *         container can not be modified.
   */
  public static Object/* IIncludePathEntry */findEntryInContainer(
      Object/* IJsGlobalScopeContainer */container, IPath libPath) {
    DartX.notYet();
    // IIncludePathEntry[] entries = container.getIncludepathEntries();
    // for (int i = 0; i < entries.length; i++) {
    // IIncludePathEntry curr = entries[i];
    // IIncludePathEntry resolved =
    // JavaScriptCore.getResolvedIncludepathEntry(curr);
    // if (resolved != null && libPath.equals(resolved.getPath())) {
    // return curr; // return the real entry
    // }
    // }
    return null; // attachment not possible
  }

  /**
   * Returns the element of the given compilation unit which is "equal" to the given element. Note
   * that the given element usually has a parent different from the given compilation unit.
   * 
   * @param cu the cu to search in
   * @param element the element to look for
   * @return an element of the given cu "equal" to the given element
   */
  public static DartElement findInCompilationUnit(CompilationUnit cu, DartElement element) {
    DartX.notYet();
    // DartElement[] elements= cu.findElements(element);
    // if (elements != null && elements.length > 0) {
    // return elements[0];
    // }
    return null;
  }

  /**
   * Finds a method in a type. This searches for a method with the same name and signature.
   * Parameter types are only compared by the simple name, no resolving for the fully qualified type
   * name is done. Constructors are only compared by parameters, not the name.
   * 
   * @param name The name of the method to find
   * @param paramTypes The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
   * @param isConstructor If the method is a constructor
   * @return The first found method or <code>null</code>, if nothing found
   */
  public static Method findMethod(String name, String[] paramTypes, boolean isConstructor, Type type)
      throws DartModelException {
    Method[] methods = type.getMethods();
    for (int i = 0; i < methods.length; i++) {
      if (isSameMethodSignature(name, paramTypes, isConstructor, methods[i])) {
        return methods[i];
      }
    }
    return null;
  }

  /**
   * Finds a method in a type and all its super types. The super class hierarchy is searched first,
   * then the super interfaces. This searches for a method with the same name and signature.
   * Parameter types are only compared by the simple name, no resolving for the fully qualified type
   * name is done. Constructors are only compared by parameters, not the name. NOTE: For finding
   * overridden methods or for finding the declaring method, use {@link MethodOverrideTester}
   * 
   * @param hierarchy The hierarchy containing the type
   * @param type The type to start the search from
   * @param name The name of the method to find
   * @param paramTypes The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
   * @param isConstructor If the method is a constructor
   * @return The first found method or <code>null</code>, if nothing found
   */
  public static Method findMethodInHierarchy(TypeHierarchy hierarchy, Type type, String name,
      String[] paramTypes, boolean isConstructor) throws DartModelException {
    Method method = findMethod(name, paramTypes, isConstructor, type);
    if (method != null) {
      return method;
    }
    Type superClass = hierarchy.getSuperclass(type);
    if (superClass != null) {
      Method res = findMethodInHierarchy(hierarchy, superClass, name, paramTypes, isConstructor);
      if (res != null) {
        return res;
      }
    }
    return method;
  }

  /**
   * Finds a type by its qualified type name (dot separated).
   * 
   * @param project The project to search in
   * @param typeName The fully qualified name (type name with enclosing type names and package (all
   *          separated by dots))
   * @return The type found, or null if not existing
   */
  public static Type findType(DartProject project, String typeName) throws DartModelException {
    for (DartLibrary lib : project.getDartLibraries()) {
      Type t = lib.findType(typeName);
      if (t != null) {
        return t;
      }
    }
    return null;
  }

  /**
   * Finds a type by its qualified type name (dot separated).
   * 
   * @param jproject The java project to search in
   * @param fullyQualifiedName The fully qualified name (type name with enclosing type names and
   *          package (all separated by dots))
   * @param owner the working copy owner
   * @return The type found, or null if not existing
   */
  public static Type findType(DartProject jproject, String fullyQualifiedName,
      WorkingCopyOwner owner) throws DartModelException {
    @SuppressWarnings("deprecation")
    Type type = jproject.findType(fullyQualifiedName, owner);
    if (type != null) {
      return type;
    }
    DartLibrary[] roots = jproject.getDartLibraries();
    for (int i = 0; i < roots.length; i++) {
      DartLibrary root = roots[i];
      type = root.findType(fullyQualifiedName);
      if (type != null && type.exists()) {
        return type;
      }
    }
    return null;
  }

  // TODO(devoncarew): commenting this method out - it had no callers, and had a compilation error
  // due to a call to DartProject.findElement(IPath).
//  /**
//   * Finds a type container by container name. The returned element will be of
//   * type <code>Type</code> or a <code>IPackageFragment</code>.
//   * <code>null</code> is returned if the type container could not be found.
//   * TODO Are we looking for CompilationUnits, or lib/app?
//   * 
//   * @param jproject The Java project defining the context to search
//   * @param typeContainerName A dot separated name of the type container
//   */
//  public static DartElement findTypeContainer(DartProject jproject,
//      String typeContainerName) throws DartModelException {
//    // try to find it as type
//    DartElement result = jproject.findType(typeContainerName);
//    if (result == null) {
//      // find it as package
//      IPath path = new Path(typeContainerName.replace('.', '/'));
//      result = jproject.findElement(path);
//      if (!(result instanceof IPackageFragment)) {
//        result = null;
//      }
//
//    }
//    return result;
//  }

  /**
   * Finds a type in a compilation unit. Typical usage is to find the corresponding type in a
   * working copy.
   * 
   * @param cu the compilation unit to search in
   * @param typeQualifiedName the type qualified name (type name with enclosing type names
   *          (separated by dots))
   * @return the type found, or null if not existing
   */
  public static Type findTypeInCompilationUnit(CompilationUnit cu, String typeQualifiedName)
      throws DartModelException {
    Type[] types = cu.getTypes();
    for (int i = 0; i < types.length; i++) {
      String currName = getTypeQualifiedName(types[i]);
      if (typeQualifiedName.equals(currName)) {
        return types[i];
      }
    }
    return null;
  }

  /**
   * Get all compilation units of a selection.
   * 
   * @param javaElements the selected java elements
   * @return all compilation units containing and contained in elements from javaElements
   * @throws DartModelException
   */
  public static CompilationUnit[] getAllCompilationUnits(DartElement[] javaElements)
      throws DartModelException {
    HashSet<CompilationUnit> result = new HashSet<CompilationUnit>();
    for (int i = 0; i < javaElements.length; i++) {
      addAllCus(result, javaElements[i]);
    }
    return result.toArray(new CompilationUnit[result.size()]);
  }

  public static Type[] getAllSuperTypes(Type type, IProgressMonitor pm) throws DartModelException {
    // workaround for 23656
    Type[] superTypes = SuperTypeHierarchyCache.getTypeHierarchy(type).getAllSuperclasses(type);
    return superTypes;
  }

  public static Method[] getConstructorsOfType(Type type) throws DartModelException {
    Method[] methods = type.getMethods();
    List<Method> constList = new ArrayList<Method>();
    for (Method method : methods) {
      if (method.isConstructor()) {
        constList.add(method);
      }
    }
    return constList.toArray(new Method[constList.size()]);
  }

  /**
   * @return the {@link DartElement} which contains given offset.
   */
  @SuppressWarnings("unchecked")
  public static <T extends DartElement & SourceReference> T getElementContaining(
      DartElement element, Class<T> elementClass, int offset) throws DartModelException {
    // may be already Function
    if (elementClass.isInstance(element)) {
      T result = (T) element;
      if (SourceRangeUtils.contains(result.getSourceRange(), offset)) {
        return result;
      }
    }
    // check children
    for (DartElement child : element.getChildren()) {
      T result = getElementContaining(child, elementClass, offset);
      if (result != null) {
        return result;
      }
    }
    // not found
    return null;
  }

  /**
   * Returns the fully qualified name of the given type using '.' as separators. This is a replace
   * for Type.getFullyQualifiedTypeName which uses '$' as separators. As '$' is also a valid
   * character in an id this is ambiguous. JavaScriptCore PR: 1GCFUNT
   */
  public static String getFullyQualifiedName(Type type) {
    return type.getElementName();
  }

//  /**
//   * Helper method that tests if an classpath entry can be found in a container.
//   * <code>null</code> is returned if the entry can not be found or if the
//   * container does not allows the configuration of source attachments
//   * 
//   * @param jproject The container's parent project
//   * @param containerPath The path of the container
//   * @param libPath The path of the library to be found
//   * @return IIncludePathEntry A classpath entry from the container of
//   *         <code>null</code> if the container can not be modified.
//   * @throws DartModelException thrown if accessing the container failed
//   */
//  public static IIncludePathEntry getClasspathEntryToEdit(DartProject jproject,
//      IPath containerPath, IPath libPath) throws DartModelException {
//    IJsGlobalScopeContainer container = JavaScriptCore.getJsGlobalScopeContainer(
//        containerPath, jproject);
//    JsGlobalScopeContainerInitializer initializer = JavaScriptCore.getJsGlobalScopeContainerInitializer(containerPath.segment(0));
//    if (container != null && initializer != null
//        && initializer.canUpdateJsGlobalScopeContainer(containerPath, jproject)) {
//      return findEntryInContainer(container, libPath);
//    }
//    return null; // attachment not possible
//  }

//  public static String getCompilerCompliance(IVMInstall2 vMInstall,
//      String defaultCompliance) {
//    DartX.todo();
//    String version = vMInstall.getJavaVersion();
//    if (version == null) {
//      return defaultCompliance;
//    } else if (version.startsWith(JavaScriptCore.VERSION_1_6)) {
//      return JavaScriptCore.VERSION_1_6;
//    } else if (version.startsWith(JavaScriptCore.VERSION_1_5)) {
//      return JavaScriptCore.VERSION_1_5;
//    } else if (version.startsWith(JavaScriptCore.VERSION_1_4)) {
//      return JavaScriptCore.VERSION_1_4;
//    } else if (version.startsWith(JavaScriptCore.VERSION_1_3)) {
//      return JavaScriptCore.VERSION_1_3;
//    } else if (version.startsWith(JavaScriptCore.VERSION_1_2)) {
//      return JavaScriptCore.VERSION_1_3;
//    } else if (version.startsWith(JavaScriptCore.VERSION_1_1)) {
//      return JavaScriptCore.VERSION_1_3;
//    }
//    return defaultCompliance;
//  }

//  public static String getFilePackage(DartElement javaElement) {
//    DartElement fileAncestor = javaElement.getAncestor(DartElement.JAVASCRIPT_UNIT);
//    if (fileAncestor == null)
//      fileAncestor = javaElement.getAncestor(DartElement.CLASS_FILE);
//    IPath filePath = fileAncestor.getResource().getFullPath();
//    DartElement rootElement = fileAncestor.getAncestor(DartElement.PACKAGE_FRAGMENT_ROOT);
//    IPath rootPath = rootElement.getResource().getFullPath();
//    String relativePath = filePath.removeFirstSegments(rootPath.segmentCount()).toPortableString();
//    int index = Util.indexOfJavaLikeExtension(relativePath);
//    if (index >= 0)
//      relativePath = relativePath.substring(0, index);
//    relativePath = relativePath.replace('/', '.');
//    return relativePath;
//  }

//  public static String getExecutionEnvironmentCompliance(
//      IExecutionEnvironment executionEnvironment) {
//    String desc = executionEnvironment.getId();
//    if (desc.indexOf("1.6") != -1) { //$NON-NLS-1$
//      return JavaScriptCore.VERSION_1_6;
//    } else if (desc.indexOf("1.5") != -1) { //$NON-NLS-1$
//      return JavaScriptCore.VERSION_1_5;
//    } else if (desc.indexOf("1.4") != -1) { //$NON-NLS-1$
//      return JavaScriptCore.VERSION_1_4;
//    }
//    return JavaScriptCore.VERSION_1_3;
//  }

//  /**
//   * Returns the package fragment root of <code>DartElement</code>. If the given
//   * element is already a package fragment root, the element itself is returned.
//   */
//  public static IPackageFragmentRoot getPackageFragmentRoot(DartElement element) {
//    DartX.todo();
//    // return (IPackageFragmentRoot)
//    // element.getAncestor(DartElement.PACKAGE_FRAGMENT_ROOT);
//    return null;
//  }

  /**
   * Compute a new name for a compilation unit, given the name of the new main type. This query
   * tries to maintain the existing extension (e.g. ".java").
   * 
   * @param cu a compilation unit
   * @param newMainName the new name of the cu's main type (without extension)
   * @return the new name for the compilation unit
   */
  public static String getRenamedCUName(CompilationUnit cu, String newMainName) {
    String oldName = cu.getElementName();
    int i = oldName.lastIndexOf('.');
    if (i != -1) {
      return newMainName + oldName.substring(i);
    } else {
      return newMainName;
    }
  }

  /**
   * Resolves a type name in the context of the declaring type.
   * 
   * @param refTypeSig the type name in signature notation (for example 'QVector') this can also be
   *          an array type, but dimensions will be ignored.
   * @param declaringType the context for resolving (type where the reference was made in)
   * @return returns the fully qualified type name or built-in-type name. if a unresolved type
   *         couldn't be resolved null is returned
   */
  public static String getResolvedTypeName(String refTypeSig, Type declaringType)
      throws DartModelException {
    DartX.todo();
    return null;
//    int arrayCount = Signature.getArrayCount(refTypeSig);
//    char type = refTypeSig.charAt(arrayCount);
//    if (type == Signature.C_UNRESOLVED) {
//      String name = ""; //$NON-NLS-1$
//      int semi = refTypeSig.indexOf(Signature.C_SEMICOLON, arrayCount + 1);
//      if (semi == -1) {
//        throw new IllegalArgumentException();
//      }
//      name = refTypeSig.substring(arrayCount + 1, semi);
//
//      String[][] resolvedNames = declaringType.resolveType(name);
//      if (resolvedNames != null && resolvedNames.length > 0) {
//        return DartModelUtil.concatenateName(resolvedNames[0][0],
//            resolvedNames[0][1]);
//      }
//      return null;
//    } else {
//      return Signature.toString(refTypeSig.substring(arrayCount));
//    }
  }

  /**
   * Returns the qualified type name of the given type using '.' as separators. This is a replace
   * for Type.getTypeQualifiedName() which uses '$' as separators. As '$' is also a valid character
   * in an id this is ambiguous. JavaScriptCore PR: 1GCFUNT
   */
  @SuppressWarnings("deprecation")
  public static String getTypeQualifiedName(Type type) {
    return type.getTypeQualifiedName('.');
  }

//  /**
//   * Returns the fully qualified name of a type's container. (package name or
//   * enclosing type name)
//   */
//  public static String getTypeContainerName(Type type) {
//    Type outerType = type.getDeclaringType();
//    if (outerType != null) {
//      return getFullyQualifiedName(outerType);
//    } else {
//      return type.getPackageFragment().getElementName();
//    }
//  }

  /**
   * @return the {@link CompilationUnit}s which are based on unique files.
   */
  public static CompilationUnit[] getUniqueCompilationUnits(CompilationUnit[] allUnits) {
    Set<IResource> files = Sets.newHashSet();
    Set<CompilationUnit> units = Sets.newHashSet();
    for (CompilationUnit unit : allUnits) {
      IResource resource = unit.getResource();
      if (!files.contains(resource)) {
        files.add(resource);
        units.add(unit);
      }
    }
    return units.toArray(new CompilationUnit[units.size()]);
  }

//  public static boolean is50OrHigher(DartProject project) {
//    return is50OrHigher(project.getOption(JavaScriptCore.COMPILER_COMPLIANCE,
//        true));
//  }

//  public static boolean is50OrHigher(String compliance) {
//    return !isVersionLessThan(compliance, JavaScriptCore.VERSION_1_5);
//  }

//  public static boolean is50OrHigherJRE(DartProject project)
//      throws CoreException {
//    IVMInstall vmInstall = JavaRuntime.getVMInstall(project);
//    if (!(vmInstall instanceof IVMInstall2))
//      return true; // assume 5.0.
//
//    String compliance = getCompilerCompliance((IVMInstall2) vmInstall, null);
//    if (compliance == null)
//      return true; // assume 5.0
//    return compliance.startsWith(JavaScriptCore.VERSION_1_5)
//        || compliance.startsWith(JavaScriptCore.VERSION_1_6);
//  }

  /**
   * Checks if the field is boolean.
   */
  public static boolean isBoolean(Field field) throws DartModelException {
    return TYPE_BOOL.equals(field.getTypeName());
  }

  /**
   * Returns if a CU can be edited.
   */
  public static boolean isEditable(CompilationUnit cu) {
    Assert.isNotNull(cu);
    IResource resource = cu.getPrimary().getResource();
    return (resource.exists() && !resource.getResourceAttributes().isReadOnly());
  }

  /*
   * http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253 Reconciling happens in a separate thread.
   * This can cause a situation where the Java element gets disposed after an exists test has been
   * done. So we should not log not present exceptions when they happen in working copies.
   */
  public static boolean isExceptionToBeLogged(CoreException exception) {
    if (!(exception instanceof DartModelException)) {
      return true;
    }
    DartModelException je = (DartModelException) exception;
    if (!je.isDoesNotExist()) {
      return true;
    }
    DartElement[] elements = je.getDartModelStatus().getElements();
    for (int i = 0; i < elements.length; i++) {
      DartElement element = elements[i];
      // if the element is already a compilation unit don't log
      // does not exist exceptions. See bug
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=75894
      // for more details
      if (element.getElementType() == DartElement.COMPILATION_UNIT) {
        continue;
      }
      CompilationUnit unit = element.getAncestor(CompilationUnit.class);
      if (unit == null) {
        return true;
      }
      if (!unit.isWorkingCopy()) {
        return true;
      }
    }
    return false;
  }

  /*
   * Returns whether the given resource path matches one of the exclusion patterns.
   * 
   * @see IIncludePathEntry#getExclusionPatterns
   */
  public final static boolean isExcluded(IPath resourcePath, char[][] exclusionPatterns) {
    if (exclusionPatterns == null) {
      return false;
    }
    char[] path = resourcePath.toString().toCharArray();
    for (int i = 0, length = exclusionPatterns.length; i < length; i++) {
      if (CharOperation.pathMatch(exclusionPatterns[i], path, true, '/')) {
        return true;
      }
    }
    return false;
  }

  public static boolean isExcludedPath(IPath resourcePath, IPath[] exclusionPatterns) {
    char[] path = resourcePath.toString().toCharArray();
    for (int i = 0, length = exclusionPatterns.length; i < length; i++) {
      char[] pattern = exclusionPatterns[i].toString().toCharArray();
      if (CharOperation.pathMatch(pattern, path, true, '/')) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return <code>true</code> if given {@link CompilationUnit} is external.
   */
  public static boolean isExternal(CompilationUnit unit) {
    return unit.getResource() == null;
  }

  public static boolean isImplicitImport(String qualifier, CompilationUnit cu) {
    if ("java.lang".equals(qualifier)) { //$NON-NLS-1$
      return true;
    }
    String packageName = cu.getParent().getElementName();
    if (qualifier.equals(packageName)) {
      return true;
    }
    String typeName = DartCore.removeDartLikeExtension(cu.getElementName());
    String mainTypeName = DartModelUtil.concatenateName(packageName, typeName);
    return qualifier.equals(mainTypeName);
  }

  public static boolean isOpenableStorage(Object storage) {
//    if (storage instanceof IJarEntryResource) {
//      return ((IJarEntryResource) storage).isFile();
//    } else {
    return storage instanceof IStorage;
//    }
  }

  /**
   * Returns true iff the given local variable is a parameter of its declaring method. TODO replace
   * this method with new API when available: https://bugs.eclipse.org/bugs/show_bug.cgi?id=48420
   * 
   * @param currentLocal the local variable to test
   * @return returns true if the variable is a parameter
   * @throws DartModelException
   */
  public static boolean isParameter(ILocalVariable currentLocal) throws DartModelException {

    final DartElement parent = currentLocal.getParent();
    if (parent instanceof Method) {
      final String[] params = ((Method) parent).getParameterNames();
      for (int i = 0; i < params.length; i++) {
        if (params[i].equals(currentLocal.getElementName())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if a cu is a primary cu (original or shared working copy)
   */
  public static boolean isPrimary(CompilationUnit cu) {
    return cu.getPrimary() == cu;
  }

  /**
   * Tests if a method equals to the given signature. Parameter types are only compared by the
   * simple name, no resolving for the fully qualified type name is done. Constructors are only
   * compared by parameters, not the name.
   * 
   * @param name Name of the method
   * @param paramTypes The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
   * @param isConstructor Specifies if the method is a constructor
   * @return Returns <code>true</code> if the method has the given name and parameter types and
   *         constructor state.
   */
  public static boolean isSameMethodSignature(String name, String[] paramTypes,
      boolean isConstructor, Method curr) throws DartModelException {
    if (isConstructor || name.equals(curr.getElementName())) {
      if (isConstructor == curr.isConstructor()) {
        String[] currParamTypes = curr.getParameterTypeNames();
        if (paramTypes.length == currParamTypes.length) {
          for (int i = 0; i < paramTypes.length; i++) {
            if (!paramTypes[i].equals(currParamTypes[i])) {
              return false;
            }
          }
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Tests if two <code>IPackageFragment</code>s represent the same logical java package.
   * 
   * @return <code>true</code> if the package fragments' names are equal.
   */
  public static boolean isSamePackage(IPackageFragment pack1, IPackageFragment pack2) {
    return pack1.getElementName().equals(pack2.getElementName());
  }

  public static boolean isSuperType(TypeHierarchy hierarchy, Type possibleSuperType, Type type) {
    // filed bug 112635 to add this method to TypeHierarchyImpl
    Type superClass = hierarchy.getSuperclass(type);
    if (superClass != null
        && (possibleSuperType.equals(superClass) || isSuperType(
            hierarchy,
            possibleSuperType,
            superClass))) {
      return true;
    }
    return false;
  }

  /**
   * @return returns if version 1 is less than version 2.
   */
  public static boolean isVersionLessThan(String version1, String version2) {
    return version1.compareTo(version2) < 0;
  }

  /**
   * Evaluates if a member (possible from another package) is visible from elements in a package.
   * 
   * @param member The member to test the visibility for
   * @param pack The package in focus
   */
  public static boolean isVisible(TypeMember member, IPackageFragment pack)
      throws DartModelException {
    return true;
//    int type = member.getElementType();
//    if (type == DartElement.INITIALIZER
//        || (type == DartElement.METHOD && member.getElementName().startsWith(
//            "<"))) { //$NON-NLS-1$
//      return false;
//    }
//
//    int otherflags = member.getFlags();
//    if (Flags.isPublic(otherflags)) {
//      return true;
//    } else if (Flags.isPrivate(otherflags)) {
//      return false;
//    }
//
//    IPackageFragment otherpack = (IPackageFragment) member.getAncestor(DartElement.PACKAGE_FRAGMENT);
//    return (pack != null && otherpack != null && isSamePackage(pack, otherpack));
  }

  /**
   * Evaluates if a member in the focus' element hierarchy is visible from elements in a package.
   * 
   * @param member The member to test the visibility for
   * @param pack The package of the focus element focus
   */
  public static boolean isVisibleInHierarchy(TypeMember member, IPackageFragment pack)
      throws DartModelException {
    return true;
//    int type = member.getElementType();
//    if (type == DartElement.INITIALIZER
//        || (type == DartElement.METHOD && member.getElementName().startsWith(
//            "<"))) { //$NON-NLS-1$
//      return false;
//    }
//
//     int otherflags= member.getFlags();
//    
//     if (Flags.isPublic(otherflags)) {
//     return true;
//     } else if (Flags.isPrivate(otherflags)) {
//     return false;
//     }
//    
//     IPackageFragment otherpack= (IPackageFragment)
//     member.getAncestor(DartElement.PACKAGE_FRAGMENT);
//     return (pack != null && pack.equals(otherpack));
  }

  /**
   * Force a reconcile of a compilation unit.
   * 
   * @param unit
   */
  public static void reconcile(CompilationUnit unit) throws DartModelException {
    DartX.todo();
    ((CompilationUnitImpl) unit).reconcile(false, null);
//    unit.reconcile(CompilationUnit.NO_AST, false /*
//                                                  * don't force problem
//                                                  * detection
//                                                  */,
//        null /* use primary owner */, null /* no progress monitor */);
  }

  private static void addAllCus(HashSet<CompilationUnit> collector, DartElement javaElement)
      throws DartModelException {
    switch (javaElement.getElementType()) {
      case DartElement.DART_PROJECT:
        DartProject javaProject = (DartProject) javaElement;
        DartLibrary[] packageFragmentRoots = javaProject.getDartLibraries();
        for (int i = 0; i < packageFragmentRoots.length; i++) {
          addAllCus(collector, packageFragmentRoots[i]);
        }
        return;

      case DartElement.LIBRARY:
        DartLibrary library = (DartLibrary) javaElement;
        DartElement[] packageFragments = library.getChildren();
        for (int j = 0; j < packageFragments.length; j++) {
          addAllCus(collector, packageFragments[j]);
        }
        return;

      case DartElement.COMPILATION_UNIT:
        collector.add((CompilationUnit) javaElement);
        return;

      default:
        CompilationUnit cu = javaElement.getAncestor(CompilationUnit.class);
        if (cu != null) {
          collector.add(cu);
        }
    }
  }

  private static IDocument aquireDocument(CompilationUnit cu, IProgressMonitor monitor)
      throws CoreException {
    if (DartModelUtil.isPrimary(cu)) {
      IFile file = (IFile) cu.getResource();
      if (file.exists()) {
        ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
        IPath path = cu.getPath();
        bufferManager.connect(path, LocationKind.IFILE, monitor);
        return bufferManager.getTextFileBuffer(path, LocationKind.IFILE).getDocument();
      }
    }
    monitor.done();
    return new Document(cu.getSource());
  }

  private static void commitDocument(CompilationUnit cu, IDocument document, TextEdit edit,
      IProgressMonitor monitor) throws CoreException, MalformedTreeException, BadLocationException {
    if (DartModelUtil.isPrimary(cu)) {
      IFile file = (IFile) cu.getResource();
      if (file.exists()) {
        IStatus status = Resources.makeCommittable(file, null);
        if (!status.isOK()) {
          throw new ValidateEditException(status);
        }
        new RewriteSessionEditProcessor(document, edit, TextEdit.UPDATE_REGIONS).performEdits(); // apply
                                                                                                 // after
        // file is
        // commitable

        ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
        bufferManager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE).commit(
            monitor,
            true);
        return;
      }
    }
    // no commit possible, make sure changes are in
    new RewriteSessionEditProcessor(document, edit, TextEdit.UPDATE_REGIONS).performEdits();
  }

//  private static Type findType(CompilationUnit cu, String fullyQualifiedName)
//      throws DartModelException {
//    Type[] types = cu.getTypes();
//    for (int i = 0; i < types.length; i++) {
//      Type type = types[i];
//      if (getFullyQualifiedName(type).equals(fullyQualifiedName))
//        return type;
//    }
//    return null;
//  }

//  private static Type findType(IPackageFragment pack, String fullyQualifiedName)
//      throws DartModelException {
//    CompilationUnit[] cus = pack.getJavaScriptUnits();
//    for (int i = 0; i < cus.length; i++) {
//      CompilationUnit unit = cus[i];
//      Type type = findType(unit, fullyQualifiedName);
//      if (type != null && type.exists())
//        return type;
//    }
//    return null;
//  }

//  private static Type findType(IPackageFragmentRoot root,
//      String fullyQualifiedName) throws DartModelException {
//    DartElement[] children = root.getChildren();
//    for (int i = 0; i < children.length; i++) {
//      DartElement element = children[i];
//      if (element.getElementType() == DartElement.PACKAGE_FRAGMENT) {
//        IPackageFragment pack = (IPackageFragment) element;
//        if (!fullyQualifiedName.startsWith(pack.getElementName()))
//          continue;
//        Type type = findType(pack, fullyQualifiedName);
//        if (type != null && type.exists())
//          return type;
//      }
//    }
//    return null;
//  }

  private static void releaseDocument(CompilationUnit cu, IDocument document,
      IProgressMonitor monitor) throws CoreException {
    if (DartModelUtil.isPrimary(cu)) {
      IFile file = (IFile) cu.getResource();
      if (file.exists()) {
        ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
        bufferManager.disconnect(file.getFullPath(), LocationKind.IFILE, monitor);
        return;
      }
    }
    cu.getBuffer().setContents(document.get());
    monitor.done();
  }
}
