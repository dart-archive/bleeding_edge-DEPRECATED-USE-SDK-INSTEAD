package com.google.dart.tools.internal.corext.refactoring;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartConventions;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.internal.corext.dom.ASTNodes;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * This class defines a set of reusable static checks methods.
 */
public class Checks {

  /* Constants returned by checkExpressionIsRValue */
  public static final int IS_RVALUE = 0;

  public static final int NOT_RVALUE_MISC = 1;
  public static final int NOT_RVALUE_VOID = 2;
  public static final int IS_RVALUE_GUESSED = 3;

  /**
   * Checks if the given name is a valid compilation unit name.
   * 
   * @param name the compilation unit name.
   * @param context an {@link DartElement} or <code>null</code>
   * @return a refactoring status containing the error message if the name is not a valid
   *         compilation unit name.
   */
  public static RefactoringStatus checkCompilationUnitName(String name, DartElement context) {
    return checkName(name, DartConventions.validateCompilationUnitName(name));
  }

  /**
   * Returns ok status if the new name is ok. This is when no other file with that name exists.
   * 
   * @param cu
   * @param newName
   * @return the status
   */
  public static RefactoringStatus checkCompilationUnitNewName(CompilationUnit cu, String newName) {
    // TODO(scheglov) implement
    throw new RuntimeException("Not implemented");
//    String newCUName = DartModelUtil.getRenamedCUName(cu, newName);
//    IPath renamedResourcePath = cu.getParent().getPath().append(newCUName);
//    if (resourceExists(renamedResourcePath))
//      return RefactoringStatus.createFatalErrorStatus(Messages.format(
//          RefactoringCoreMessages.Checks_cu_name_used,
//          BasicElementLabels.getResourceName(newCUName)));
//    else
//      return new RefactoringStatus();
  }

  /**
   * Checks if the given name is a valid Dart field name.
   * 
   * @param name the Dart field name.
   * @param context an {@link DartElement} or <code>null</code>
   * @return a refactoring status containing the error message if the name is not a valid Dart field
   *         name.
   */
  public static RefactoringStatus checkFieldName(String name, DartElement context) {
    return checkName(name, DartConventions.validateFieldName(name));
  }

  /**
   * Checks if the given name is a valid Dart type parameter name.
   * 
   * @param name the Dart type parameter name.
   * @param context an {@link DartElement} or <code>null</code>
   * @return a refactoring status containing the error message if the name is not a valid Dart type
   *         parameter name.
   */
//  public static RefactoringStatus checkTypeParameterName(String name, DartElement context) {
//  	return checkName(name, DartConventions.validateTypeVariableName(name, context));
//  }

  /**
   * Checks if the given name is a valid Dart identifier.
   * 
   * @param name the Dart identifier.
   * @param context an {@link DartElement} or <code>null</code>
   * @return a refactoring status containing the error message if the name is not a valid Dart
   *         identifier.
   */
//  public static RefactoringStatus checkIdentifier(String name, DartElement context) {
//    return checkName(name, DartConventions.validateIdentifier(name, context));
//  }

  /**
   * Checks if method will have a constructor name after renaming.
   * 
   * @param method
   * @param newMethodName
   * @param newTypeName
   * @return <code>RefactoringStatus</code> with <code>WARNING</code> severity if the give method
   *         will have a constructor name after renaming <code>null</code> otherwise.
   */
  public static RefactoringStatus checkIfConstructorName(Method method, String newMethodName,
      String newTypeName) {
    // TODO(scheglov) implement
    throw new RuntimeException("Not implemented");
//    if (!newMethodName.equals(newTypeName))
//      return null;
//    else
//      return RefactoringStatus.createWarningStatus(Messages.format(
//          RefactoringCoreMessages.Checks_constructor_name,
//          new Object[]{
//              JavaElementUtil.createMethodSignature(method),
//              JavaElementLabels.getElementLabel(
//                  method.getDeclaringType(),
//                  JavaElementLabels.ALL_FULLY_QUALIFIED)}));
  }

  /**
   * Checks if the given name is a valid Dart method name.
   * 
   * @param name the Dart method name.
   * @param context an {@link DartElement} or <code>null</code>
   * @return a refactoring status containing the error message if the name is not a valid Dart
   *         method name.
   */
  public static RefactoringStatus checkMethodName(String name, DartElement context) {
    RefactoringStatus status = checkName(name, DartConventions.validateMethodName(name));
    if (status.isOK() && !startsWithLowerCase(name)) {
      return RefactoringStatus.createWarningStatus(RefactoringCoreMessages.Checks_method_names_lowercase);
    } else {
      return status;
    }
  }

  /**
   * Checks if the given name is a valid Dart package name.
   * 
   * @param name the Dart package name.
   * @param context an {@link DartElement} or <code>null</code>
   * @return a refactoring status containing the error message if the name is not a valid Dart
   *         package name.
   */
//  public static RefactoringStatus checkPackageName(String name, DartElement context) {
//  	return checkName(name, DartConventions.validatePackageName(name));
//  }

  /**
   * Returns a fatal error in case the name is empty. In all other cases, an error based on the
   * given status is returned.
   * 
   * @param name a name
   * @param status a status
   * @return RefactoringStatus based on the given status or the name, if empty.
   */
  public static RefactoringStatus checkName(String name, IStatus status) {
    RefactoringStatus result = new RefactoringStatus();
    if ("".equals(name)) {
      return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.Checks_Choose_name);
    }

    if (status.isOK()) {
      return result;
    }

    switch (status.getSeverity()) {
      case IStatus.ERROR:
        return RefactoringStatus.createFatalErrorStatus(status.getMessage());
      case IStatus.WARNING:
        return RefactoringStatus.createWarningStatus(status.getMessage());
      case IStatus.INFO:
        return RefactoringStatus.createInfoStatus(status.getMessage());
      default: //no nothing
        return new RefactoringStatus();
    }
  }

  /**
   * Checks if the given name is a valid Dart type name.
   * 
   * @param name the Dart method name.
   * @param context an {@link DartElement} or <code>null</code>
   * @return a refactoring status containing the error message if the name is not a valid Dart type
   *         name.
   */
  public static RefactoringStatus checkTypeName(String name, DartElement context) {
    //fix for: 1GF5Z0Z: ITPJUI:WINNT - assertion failed after renameType refactoring
    if (name.indexOf(".") != -1) {
      return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.Checks_no_dot);
    } else {
      return checkName(name, DartConventions.validateTypeName(name));
    }
  }

  public static boolean isAvailable(DartElement dartElement) throws DartModelException {
    if (dartElement == null) {
      return false;
    }
    if (!dartElement.exists()) {
      return false;
    }
    if (dartElement.isReadOnly()) {
      return false;
    }
    return true;
  }

  /**
   * @param e
   * @return int Checks.IS_RVALUE if e is an rvalue Checks.IS_RVALUE_GUESSED if e is guessed as an
   *         rvalue Checks.NOT_RVALUE_VOID if e is not an rvalue because its type is void
   *         Checks.NOT_RVALUE_MISC if e is not an rvalue for some other reason
   */
//  public static int checkExpressionIsRValue(Expression e) {
//  	if (e instanceof Name) {
//  		if(!(((Name) e).resolveBinding() instanceof IVariableBinding)) {
//  			return NOT_RVALUE_MISC;
//  		}
//  	}
//  	if (e instanceof Annotation)
//  		return NOT_RVALUE_MISC;
//  		
//
//  	ITypeBinding tb= e.resolveTypeBinding();
//  	boolean guessingRequired= false;
//  	if (tb == null) {
//  		guessingRequired= true;
//  		tb= ASTResolving.guessBindingForReference(e);
//  	}
//  	if (tb == null)
//  		return NOT_RVALUE_MISC;
//  	else if (tb.getName().equals("void")) //$NON-NLS-1$
//  		return NOT_RVALUE_VOID;
//
//  	return guessingRequired ? IS_RVALUE_GUESSED : IS_RVALUE;
//  }

  public static boolean isDeclaredIn(DartVariable tempDeclaration,
      Class<? extends DartNode> astNodeClass) {
    // TODO(scheglov) I think that this is bad function, because DartVariable is just local variable.
    DartNode parent = ASTNodes.getParent(tempDeclaration, astNodeClass);
    if (parent == null) {
      return false;
    }
    return true;
  }

//  public static boolean isTopLevel(Type type){
//  	return type.getDeclaringType() == null;
//  }
//
//  public static boolean isAnonymous(Type type) throws DartModelException {
//  	return type.isAnonymous();
//  }
//
//  public static boolean isTopLevelType(TypeMember member){
//  	return  member.getElementType() == DartElement.TYPE && isTopLevel((Type) member);
//  }
//
//  public static boolean isInsideLocalType(Type type) throws DartModelException {
//  	while (type != null) {
//  		if (type.isLocal())
//  			return true;
//  		type= type.getDeclaringType();
//  	}
//  	return false;
//  }
//
//  public static boolean isAlreadyNamed(DartElement element, String name){
//  	return name.equals(element.getElementName());
//  }
//
//  //-------------- main and native method checks ------------------
//  public static RefactoringStatus checkForMainAndNativeMethods(CompilationUnit cu) throws DartModelException {
//  	return checkForMainAndNativeMethods(cu.getTypes());
//  }
//
//  public static RefactoringStatus checkForMainAndNativeMethods(Type[] types) throws DartModelException {
//  	RefactoringStatus result= new RefactoringStatus();
//  	for (int i= 0; i < types.length; i++)
//  		result.merge(checkForMainAndNativeMethods(types[i]));
//  	return result;
//  }
//
//  public static RefactoringStatus checkForMainAndNativeMethods(Type type) throws DartModelException {
//  	RefactoringStatus result= new RefactoringStatus();
//  	result.merge(checkForMainAndNativeMethods(type.getMethods()));
//  	result.merge(checkForMainAndNativeMethods(type.getTypes()));
//  	return result;
//  }
//
//  private static RefactoringStatus checkForMainAndNativeMethods(Method[] methods) throws DartModelException {
//  	RefactoringStatus result= new RefactoringStatus();
//  	for (int i= 0; i < methods.length; i++) {
//  		if (JdtFlags.isNative(methods[i])){
//  			String typeName= JavaElementLabels.getElementLabel(methods[i].getDeclaringType(), JavaElementLabels.ALL_FULLY_QUALIFIED);
//  			String methodName= JavaElementLabels.getElementLabel(methods[i], JavaElementLabels.ALL_DEFAULT);
//  			String msg= Messages.format(RefactoringCoreMessages.Checks_method_native,
//  							new String[]{typeName, methodName, "UnsatisfiedLinkError"});//$NON-NLS-1$
//  			result.addEntry(RefactoringStatus.ERROR, msg, JavaStatusContext.create(methods[i]), Corext.getPluginId(), RefactoringStatusCodes.NATIVE_METHOD);
//  		}
//  		if (methods[i].isMainMethod()) {
//  			String msg= Messages.format(RefactoringCoreMessages.Checks_has_main,
//  					JavaElementLabels.getElementLabel(methods[i].getDeclaringType(), JavaElementLabels.ALL_FULLY_QUALIFIED));
//  			result.addEntry(RefactoringStatus.WARNING, msg, JavaStatusContext.create(methods[i]), Corext.getPluginId(), RefactoringStatusCodes.MAIN_METHOD);
//  		}
//  	}
//  	return result;
//  }

  //---- New method name checking -------------------------------------------------------------

  /**
   * Checks if the new method is already used in the given type.
   * 
   * @param type
   * @param methodName
   * @param parameters
   * @return the status
   */
//  public static RefactoringStatus checkMethodInType(ITypeBinding type, String methodName, ITypeBinding[] parameters) {
//  	RefactoringStatus result= new RefactoringStatus();
//  	if (methodName.equals(type.getName()))
//  		result.addWarning(RefactoringCoreMessages.Checks_methodName_constructor);
//  	IMethodBinding method= org.eclipse.jdt.internal.corext.dom.Bindings.findMethodInType(type, methodName, parameters);
//  	if (method != null)
//  		result.addError(Messages.format(RefactoringCoreMessages.Checks_methodName_exists,
//  			new Object[] {BasicElementLabels.getJavaElementName(methodName), BasicElementLabels.getJavaElementName(type.getName())}),
//  			JavaStatusContext.create(method));
//  	return result;
//  }

  public static boolean resourceExists(IPath resourcePath) {
    return ResourcesPlugin.getWorkspace().getRoot().findMember(resourcePath) != null;
  }

  /**
   * Finds a method in a type This searches for a method with the same name and signature. Parameter
   * types are only compared by the simple name, no resolving for the fully qualified type name is
   * done
   * 
   * @param name
   * @param parameterCount
   * @param isConstructor
   * @param type
   * @return The first found method or null, if nothing found
   * @throws DartModelException
   */
//  public static Method findMethod(String name, int parameterCount, boolean isConstructor, Type type) throws DartModelException {
//  	return findMethod(name, parameterCount, isConstructor, type.getMethods());
//  }

  /**
   * Finds a method in a type. Searches for a method with the same name and the same parameter
   * count. Parameter types are <b>not</b> compared.
   * 
   * @param method
   * @param type
   * @return The first found method or null, if nothing found
   * @throws DartModelException
   */
//  public static Method findMethod(Method method, Type type) throws DartModelException {
//  	return findMethod(method.getElementName(), method.getParameterTypes().length, method.isConstructor(), type.getMethods());
//  }

  /**
   * Finds a method in an array of methods. Searches for a method with the same name and the same
   * parameter count. Parameter types are <b>not</b> compared.
   * 
   * @param method
   * @param methods
   * @return The first found method or null, if nothing found
   * @throws DartModelException
   */
//  public static Method findMethod(Method method, Method[] methods) throws DartModelException {
//  	return findMethod(method.getElementName(), method.getParameterTypes().length, method.isConstructor(), methods);
//  }
//
//  public static Method findMethod(String name, int parameters, boolean isConstructor, Method[] methods) throws DartModelException {
//  	for (int i= methods.length-1; i >= 0; i--) {
//  		Method curr= methods[i];
//  		if (name.equals(curr.getElementName())) {
//  			if (isConstructor == curr.isConstructor()) {
//  				if (parameters == curr.getParameterTypes().length) {
//  					return curr;
//  				}
//  			}
//  		}
//  	}
//  	return null;
//  }

  /**
   * Finds a method in a type. This searches for a method with the same name and signature.
   * Parameter types are only compared by the simple name, no resolving for the fully qualified type
   * name is done
   * 
   * @param method
   * @param type
   * @return The first found method or null, if nothing found
   * @throws DartModelException
   */
//  public static Method findSimilarMethod(Method method, Type type) throws DartModelException {
//  	return findSimilarMethod(method, type.getMethods());
//  }

  /**
   * Finds a method in an array of methods. This searches for a method with the same name and
   * signature. Parameter types are only compared by the simple name, no resolving for the fully
   * qualified type name is done
   * 
   * @param method
   * @param methods
   * @return The first found method or null, if nothing found
   * @throws DartModelException
   */
//  public static Method findSimilarMethod(Method method, Method[] methods) throws DartModelException {
//  	boolean isConstructor= method.isConstructor();
//  	for (int i= 0; i < methods.length; i++) {
//  		Method otherMethod= methods[i];
//  		if (otherMethod.isConstructor() == isConstructor && method.isSimilar(otherMethod))
//  			return otherMethod;
//  	}
//  	return null;
//  }

  /**
   * Compare two parameter signatures.
   */
//  public static boolean compareParamTypes(String[] paramTypes1, String[] paramTypes2) {
//  	if (paramTypes1.length == paramTypes2.length) {
//  		int i= 0;
//  		while (i < paramTypes1.length) {
//  			String t1= Signature.getSimpleName(Signature.toString(paramTypes1[i]));
//  			String t2= Signature.getSimpleName(Signature.toString(paramTypes2[i]));
//  			if (!t1.equals(t2)) {
//  				return false;
//  			}
//  			i++;
//  		}
//  		return true;
//  	}
//  	return false;
//  }

  //---------------------

//  public static RefactoringStatus checkIfCuBroken(TypeMember member) throws DartModelException{
//  	CompilationUnit cu= (CompilationUnit)JavaCore.create(member.getCompilationUnit().getResource());
//  	if (cu == null)
//  		return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.Checks_cu_not_created);
//  	else if (! cu.isStructureKnown())
//  		return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.Checks_cu_not_parsed);
//  	return new RefactoringStatus();
//  }

  /**
   * From SearchResultGroup[] passed as the parameter this method removes all those that correspond
   * to a non-parsable CompilationUnit and returns it as a result.
   * 
   * @param grouped the array of search result groups from which non parsable compilation units are
   *          to be removed.
   * @param status a refactoring status to collect errors and problems
   * @return the array of search result groups
   * @throws DartModelException
   */
//  public static SearchResultGroup[] excludeCompilationUnits(SearchResultGroup[] grouped, RefactoringStatus status) throws DartModelException{
//  	List<SearchResultGroup> result= new ArrayList<SearchResultGroup>();
//  	boolean wasEmpty= grouped.length == 0;
//  	for (int i= 0; i < grouped.length; i++){
//  		IResource resource= grouped[i].getResource();
//  		DartElement element= JavaCore.create(resource);
//  		if (! (element instanceof CompilationUnit))
//  			continue;
//  		//XXX this is a workaround 	for a jcore feature that shows errors in cus only when you get the original element
//  		CompilationUnit cu= (CompilationUnit)JavaCore.create(resource);
//  		if (! cu.isStructureKnown()){
//  			status.addError(Messages.format(RefactoringCoreMessages.Checks_cannot_be_parsed, BasicElementLabels.getPathLabel(cu.getPath(), false)));
//  			continue; //removed, go to the next one
//  		}
//  		result.add(grouped[i]);
//  	}
//
//  	if ((!wasEmpty) && result.isEmpty())
//  		status.addFatalError(RefactoringCoreMessages.Checks_all_excluded);
//
//  	return result.toArray(new SearchResultGroup[result.size()]);
//  }
//
//  public static RefactoringStatus checkCompileErrorsInAffectedFiles(SearchResultGroup[] grouped) throws DartModelException {
//  	RefactoringStatus result= new RefactoringStatus();
//  	for (int i= 0; i < grouped.length; i++)
//  		checkCompileErrorsInAffectedFile(result, grouped[i].getResource());
//  	return result;
//  }
//
//  public static void checkCompileErrorsInAffectedFile(RefactoringStatus result, IResource resource) throws DartModelException {
//  	if (hasCompileErrors(resource))
//  		result.addWarning(Messages.format(RefactoringCoreMessages.Checks_cu_has_compile_errors, BasicElementLabels.getPathLabel(resource.getFullPath(), false)));
//  }
//
//  public static RefactoringStatus checkCompileErrorsInAffectedFiles(SearchResultGroup[] references, IResource declaring) throws DartModelException {
//  	RefactoringStatus result= new RefactoringStatus();
//  	for (int i= 0; i < references.length; i++){
//  		IResource resource= references[i].getResource();
//  		if (resource.equals(declaring))
//  			declaring= null;
//  		checkCompileErrorsInAffectedFile(result, resource);
//  	}
//  	if (declaring != null)
//  		checkCompileErrorsInAffectedFile(result, declaring);
//  	return result;
//  }
//
//  private static boolean hasCompileErrors(IResource resource) throws DartModelException {
//  	try {
//  		IMarker[] problemMarkers= resource.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
//  		for (int i= 0; i < problemMarkers.length; i++) {
//  			if (problemMarkers[i].getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR)
//  				return true;
//  		}
//  		return false;
//  	} catch (DartModelException e){
//  		throw e;
//  	} catch (CoreException e){
//  		throw new DartModelException(e);
//  	}
//  }

  //------
//  public static boolean isReadOnly(Object element) throws DartModelException{
//  	if (element instanceof IResource)
//  		return isReadOnly((IResource)element);
//
//  	if (element instanceof DartElement) {
//  		if ((element instanceof IPackageFragmentRoot) && isClasspathDelete((IPackageFragmentRoot)element))
//  			return false;
//  		return isReadOnly(((DartElement)element).getResource());
//  	}
//
//  	Assert.isTrue(false, "not expected to get here");	 //$NON-NLS-1$
//  	return false;
//  }
//
//  public static boolean isReadOnly(IResource res) throws DartModelException {
//  	ResourceAttributes attributes= res.getResourceAttributes();
//  	if (attributes != null && attributes.isReadOnly())
//  		return true;
//
//  	if (! (res instanceof IContainer))
//  		return false;
//
//  	IContainer container= (IContainer)res;
//  	try {
//  		IResource[] children= container.members();
//  		for (int i= 0; i < children.length; i++) {
//  			if (isReadOnly(children[i]))
//  				return true;
//  		}
//  		return false;
//  	} catch (DartModelException e){
//  		throw e;
//  	} catch (CoreException e) {
//  		throw new DartModelException(e);
//  	}
//  }
//
//  public static boolean isClasspathDelete(IPackageFragmentRoot pkgRoot) {
//  	IResource res= pkgRoot.getResource();
//  	if (res == null)
//  		return true;
//  	IProject definingProject= res.getProject();
//  	if (res.getParent() != null && pkgRoot.isArchive() && ! res.getParent().equals(definingProject))
//  		return true;
//
//  	IProject occurringProject= pkgRoot.getJavaProject().getProject();
//  	return !definingProject.equals(occurringProject);
//  }

  //-------- validateEdit checks ----

//  public static RefactoringStatus validateModifiesFiles(IFile[] filesToModify, Object context) {
//  	RefactoringStatus result= new RefactoringStatus();
//  	IStatus status= Resources.checkInSync(filesToModify);
//  	if (!status.isOK())
//  		result.merge(RefactoringStatus.create(status));
//  	status= Resources.makeCommittable(filesToModify, context);
//  	if (!status.isOK()) {
//  		result.merge(RefactoringStatus.create(status));
//  		if (!result.hasFatalError()) {
//  			result.addFatalError(RefactoringCoreMessages.Checks_validateEdit);
//  		}
//  	}
//  	return result;
//  }
//
//  public static void addModifiedFilesToChecker(IFile[] filesToModify, CheckConditionsContext context) {
//  	ResourceChangeChecker checker= (ResourceChangeChecker) context.getChecker(ResourceChangeChecker.class);
//  	IResourceChangeDescriptionFactory deltaFactory= checker.getDeltaFactory();
//
//  	for (int i= 0; i < filesToModify.length; i++) {
//  		deltaFactory.change(filesToModify[i]);
//  	}
//  }
//
//
//  public static RefactoringStatus validateEdit(CompilationUnit unit, Object context) {
//  	IResource resource= unit.getPrimary().getResource();
//  	RefactoringStatus result= new RefactoringStatus();
//  	if (resource == null)
//  		return result;
//  	IStatus status= Resources.checkInSync(resource);
//  	if (!status.isOK())
//  		result.merge(RefactoringStatus.create(status));
//  	status= Resources.makeCommittable(resource, context);
//  	if (!status.isOK()) {
//  		result.merge(RefactoringStatus.create(status));
//  		if (!result.hasFatalError()) {
//  			result.addFatalError(RefactoringCoreMessages.Checks_validateEdit);
//  		}
//  	}
//  	return result;
//  }

  /**
   * Checks if the new method somehow conflicts with an already existing method in the hierarchy.
   * The following checks are done:
   * <ul>
   * <li>if the new method overrides a method defined in the given type or in one of its super
   * classes.</li>
   * </ul>
   * 
   * @param type
   * @param methodName
   * @param returnType
   * @param parameters
   * @return the status
   */
//  public static RefactoringStatus checkMethodInHierarchy(ITypeBinding type, String methodName, ITypeBinding returnType, ITypeBinding[] parameters) {
//  	RefactoringStatus result= new RefactoringStatus();
//  	IMethodBinding method= Bindings.findMethodInHierarchy(type, methodName, parameters);
//  	if (method != null) {
//  		boolean returnTypeClash= false;
//  		ITypeBinding methodReturnType= method.getReturnType();
//  		if (returnType != null && methodReturnType != null) {
//  			String returnTypeKey= returnType.getKey();
//  			String methodReturnTypeKey= methodReturnType.getKey();
//  			if (returnTypeKey == null && methodReturnTypeKey == null) {
//  				returnTypeClash= returnType != methodReturnType;
//  			} else if (returnTypeKey != null && methodReturnTypeKey != null) {
//  				returnTypeClash= !returnTypeKey.equals(methodReturnTypeKey);
//  			}
//  		}
//  		ITypeBinding dc= method.getDeclaringClass();
//  		if (returnTypeClash) {
//  			result.addError(Messages.format(RefactoringCoreMessages.Checks_methodName_returnTypeClash,
//  				new Object[] {BasicElementLabels.getJavaElementName(methodName), BasicElementLabels.getJavaElementName(dc.getName())}),
//  				JavaStatusContext.create(method));
//  		} else {
//  			result.addError(Messages.format(RefactoringCoreMessages.Checks_methodName_overrides,
//  				new Object[] {BasicElementLabels.getJavaElementName(methodName), BasicElementLabels.getJavaElementName(dc.getName())}),
//  				JavaStatusContext.create(method));
//  		}
//  	}
//  	return result;
//  }

  //---- Selection checks --------------------------------------------------------------------

//  public static boolean isExtractableExpression(DartNode[] selectedNodes, DartNode coveringNode) {
//  	DartNode node= coveringNode;
//  	if (isEnumCase(node))
//  		return false;
//  	if (selectedNodes != null && selectedNodes.length == 1)
//  		node= selectedNodes[0];
//  	return isExtractableExpression(node);
//  }
//
//  public static boolean isEnumCase(DartNode node) {
//  	if (node instanceof SwitchCase) {
//  		final SwitchCase caze= (SwitchCase) node;
//  		final Expression expression= caze.getExpression();
//  		if (expression instanceof Name) {
//  			final Name name= (Name) expression;
//  			final IBinding binding= name.resolveBinding();
//  			if (binding instanceof IVariableBinding) {
//  				IVariableBinding variableBinding= (IVariableBinding) binding;
//  				return variableBinding.isEnumConstant();
//  			}
//  		}
//  	}
//  	return false;
//  }
//
//  public static boolean isExtractableExpression(DartNode node) {
//  	if (!(node instanceof Expression))
//  		return false;
//  	if (node instanceof Name) {
//  		IBinding binding= ((Name) node).resolveBinding();
//  		return binding == null || binding instanceof IVariableBinding;
//  	}
//  	return true;
//  }
//
//  public static boolean isInsideJavadoc(DartNode node) {
//  	do {
//  		if (node.getNodeType() == DartNode.JAVADOC)
//  			return true;
//  		node= node.getParent();
//  	} while (node != null);
//  	return false;
//  }

  public static boolean startsWithLowerCase(String s) {
    if (s == null) {
      return false;
    } else if (s.isEmpty()) {
      return false;
    } else {
      return s.charAt(0) == Character.toLowerCase(s.charAt(0));
    }
  }

//  /**
//   * Checks whether it is possible to modify the given <code>DartElement</code>. The
//   * <code>DartElement</code> must exist and be non read-only to be modifiable. Moreover, if it is a
//   * <code>TypeMember</code> it must not be binary. The returned <code>RefactoringStatus</code> has
//   * <code>ERROR</code> severity if it is not possible to modify the element.
//   * 
//   * @param javaElement
//   * @return the status
//   * @throws DartModelException
//   * @see DartElement#exists
//   * @see DartElement#isReadOnly
//   * @see TypeMember#isBinary
//   * @see RefactoringStatus
//   */
//  public static RefactoringStatus checkAvailability(DartElement javaElement) throws DartModelException{
//  	RefactoringStatus result= new RefactoringStatus();
//  	if (! javaElement.exists())
//  		result.addFatalError(Messages.format(RefactoringCoreMessages.Refactoring_not_in_model, getJavaElementName(javaElement)));
//  	if (javaElement.isReadOnly())
//  		result.addFatalError(Messages.format(RefactoringCoreMessages.Refactoring_read_only, getJavaElementName(javaElement)));
//  	if (javaElement.exists() && !javaElement.isStructureKnown())
//  		result.addFatalError(Messages.format(RefactoringCoreMessages.Refactoring_unknown_structure, getJavaElementName(javaElement)));
//  	if (javaElement instanceof TypeMember && ((TypeMember)javaElement).isBinary())
//  		result.addFatalError(Messages.format(RefactoringCoreMessages.Refactoring_binary, getJavaElementName(javaElement)));
//  	return result;
//  }
//
//  private static String getDartElementName(DartElement element) {
//    return DartElementLabels.getElementLabel(element, DartElementLabels.ALL_DEFAULT);
//  }
//
//  public static Type findTypeInPackage(IPackageFragment pack, String elementName) throws DartModelException {
//  	Assert.isTrue(pack.exists());
//  	Assert.isTrue(!pack.isReadOnly());
//
//  	String packageName= pack.getElementName();
//  	elementName= packageName.length() > 0 ? packageName + '.' + elementName : elementName;
//
//  	return pack.getJavaProject().findType(elementName, (IProgressMonitor) null);
//  }
//
//  public static RefactoringStatus checkTempName(String newName, DartElement context) {
//  	RefactoringStatus result= Checks.checkIdentifier(newName, context);
//  	if (result.hasFatalError())
//  		return result;
//  	if (! Checks.startsWithLowerCase(newName))
//  		result.addWarning(RefactoringCoreMessages.ExtractTempRefactoring_convention);
//  	return result;
//  }
//
//  public static RefactoringStatus checkEnumConstantName(String newName, DartElement context) {
//  	RefactoringStatus result= Checks.checkFieldName(newName, context);
//  	if (result.hasFatalError())
//  		return result;
//  	for (int i= 0; i < newName.length(); i++) {
//  		char c= newName.charAt(i);
//  		if (Character.isLetter(c) && !Character.isUpperCase(c)) {
//  			result.addWarning(RefactoringCoreMessages.RenameEnumConstRefactoring_convention);
//  			break;
//  		}
//  	}
//  	return result;
//  }
//
//  public static RefactoringStatus checkConstantName(String newName, DartElement context) {
//  	RefactoringStatus result= Checks.checkFieldName(newName, context);
//  	if (result.hasFatalError())
//  		return result;
//  	for (int i= 0; i < newName.length(); i++) {
//  		char c= newName.charAt(i);
//  		if (Character.isLetter(c) && !Character.isUpperCase(c)) {
//  			result.addWarning(RefactoringCoreMessages.ExtractConstantRefactoring_convention);
//  			break;
//  		}
//  	}
//  	return result;
//  }
//
//  public static boolean isException(Type iType, IProgressMonitor pm) throws DartModelException {
//  	try{
//  		if (! iType.isClass())
//  			return false;
//  		Type[] superTypes= iType.newSupertypeHierarchy(pm).getAllSupertypes(iType);
//  		for (int i= 0; i < superTypes.length; i++) {
//  			if ("java.lang.Throwable".equals(superTypes[i].getFullyQualifiedName())) //$NON-NLS-1$
//  				return true;
//  		}
//  		return false;
//  	} finally{
//  		pm.done();
//  	}
//  }

  /**
   * no instances
   */
  private Checks() {
  }
}
