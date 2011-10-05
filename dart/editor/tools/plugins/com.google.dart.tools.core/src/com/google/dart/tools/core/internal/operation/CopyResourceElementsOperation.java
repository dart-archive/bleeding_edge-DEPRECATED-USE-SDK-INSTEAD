/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartElementImpl;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.model.delta.DartElementDeltaImpl;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatus;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Instances of the class <code>CopyResourceElementsOperation</code> implement an operation that
 * copies/moves/renames a collection of resources from their current container to a new container,
 * optionally renaming the elements.
 * <p>
 * Notes:
 * <ul>
 * <li>If there is already an resource with the same name in the new container, the operation either
 * overwrites or aborts, depending on the collision policy setting. The default setting is abort.
 * <li>When a compilation unit is copied to a new package, the package declaration in the
 * compilation unit is automatically updated.
 * <li>The collection of elements being copied must all share the same type of container.
 * <li>This operation can be used to copy and rename elements within the same container.
 * <li>This operation only copies compilation units and package fragments. It does not copy package
 * fragment roots - a platform operation must be used for that.
 * </ul>
 */
public class CopyResourceElementsOperation extends MultiOperation {
  /**
   * The list of new resources created during this operation.
   */
  protected ArrayList<DartElement> createdElements;

  /**
   * Table specifying deltas for elements being copied/moved/renamed. Keyed by elements' project(s),
   * and values are the corresponding deltas.
   */
  protected Map<DartProject, DartElementDeltaImpl> deltasPerProject = new HashMap<DartProject, DartElementDeltaImpl>(
      1);

  /**
   * The <code>ASTParser</code> used to manipulate the source code of a {@link CompilationUnit}.
   */
  // protected ASTParser parser;

  /**
   * When executed, this operation will copy the given resources to the given containers. The
   * resources and destination containers must be in the correct order. If there is > 1 destination,
   * the number of destinations must be the same as the number of resources being copied/moved.
   */
  public CopyResourceElementsOperation(DartElement[] resourcesToCopy, DartElement[] destContainers,
      boolean force) {
    super(resourcesToCopy, destContainers, force);
    initializeASTParser();
  }

  /**
   * @see MultiOperation
   */
  @Override
  protected String getMainTaskName() {
    return Messages.operation_copyResourceProgress;
  }

  @Override
  protected ISchedulingRule getSchedulingRule() {
    if (this.elementsToProcess == null) {
      return null;
    }
    int length = this.elementsToProcess.length;
    if (length == 1) {
      return getSchedulingRule(this.elementsToProcess[0]);
    }
    ISchedulingRule[] rules = new ISchedulingRule[length];
    int index = 0;
    for (int i = 0; i < length; i++) {
      ISchedulingRule rule = getSchedulingRule(this.elementsToProcess[i]);
      if (rule != null) {
        rules[index++] = rule;
      }
    }
    if (index != length) {
      System.arraycopy(rules, 0, rules = new ISchedulingRule[index], 0, index);
    }
    return new MultiRule(rules);
  }

  /**
   * Sets the deltas to register the changes resulting from this operation for this source element
   * and its destination. If the operation is a cross project operation
   * <ul>
   * <li>On a copy, the delta should be rooted in the dest project
   * <li>On a move, two deltas are generated
   * <ul>
   * <li>one rooted in the source project
   * <li>one rooted in the destination project
   * </ul>
   * </ul>
   * If the operation is rooted in a single project, the delta is rooted in that project
   */
  protected void prepareDeltas(DartElement sourceElement, DartElement destinationElement,
      boolean isMove) {
    DartCore.notYetImplemented();
    // if (Util.isExcluded(sourceElement) ||
    // Util.isExcluded(destinationElement)) {
    // return;
    // }
    DartProject destProject = destinationElement.getDartProject();
    if (isMove) {
      DartProject sourceProject = sourceElement.getDartProject();
      getDeltaFor(sourceProject).movedFrom(sourceElement, destinationElement);
      getDeltaFor(destProject).movedTo(destinationElement, sourceElement);
    } else {
      getDeltaFor(destProject).added(destinationElement);
    }
  }

  /**
   * Copies/moves a compilation unit with the name <code>newCUName</code> to the destination
   * package.<br>
   * The package statement in the compilation unit is updated if necessary. The main type of the
   * compilation unit is renamed if necessary.
   * 
   * @exception DartModelException if the operation is unable to complete
   */
  // private void processCompilationUnitResource(CompilationUnit source,
  // PackageFragment dest) throws DartModelException {
  // String newCUName = getNewNameFor(source);
  // String destName = (newCUName != null) ? newCUName :
  // source.getElementName();
  // TextEdit edit = updateContent(source, dest, newCUName); // null if
  // unchanged
  //
  // // TODO (frederic) remove when bug 67606 will be fixed (bug 67823)
  // // store encoding (fix bug 66898)
  // IFile sourceResource = (IFile)source.getResource();
  // String sourceEncoding = null;
  // try {
  // sourceEncoding = sourceResource.getCharset(false);
  // }
  // catch (CoreException ce) {
  // // no problem, use default encoding
  // }
  // // end todo
  // // copy resource
  // IContainer destFolder = (IContainer)dest.getResource(); // can be an
  // IFolder or an IProject
  // IFile destFile = destFolder.getFile(new Path(destName));
  // org.eclipse.jdt.internal.core.CompilationUnit destCU = new
  // org.eclipse.jdt.internal.core.CompilationUnit(dest, destName,
  // DefaultWorkingCopyOwner.PRIMARY);
  // if (!destFile.equals(sourceResource)) {
  // try {
  // if (!destCU.isWorkingCopy()) {
  // if (destFile.exists()) {
  // if (this.force) {
  // // we can remove it
  // deleteResource(destFile, IResource.KEEP_HISTORY);
  // destCU.close(); // ensure the in-memory buffer for the dest CU is closed
  // } else {
  // // abort
  // throw new DartModelException(new DartModelStatusImpl(
  // DartModelStatusConstants.NAME_COLLISION,
  // Messages.bind(Messages.status_nameCollision,
  // destFile.getFullPath().toString())));
  // }
  // }
  // int flags = this.force ? IResource.FORCE : IResource.NONE;
  // if (isMove()) {
  // flags |= IResource.KEEP_HISTORY;
  // sourceResource.move(destFile.getFullPath(), flags,
  // getSubProgressMonitor(1));
  // } else {
  // if (edit != null) flags |= IResource.KEEP_HISTORY;
  // sourceResource.copy(destFile.getFullPath(), flags,
  // getSubProgressMonitor(1));
  // }
  // setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
  // } else {
  // destCU.getBuffer().setContents(source.getBuffer().getContents());
  // }
  // } catch (DartModelException e) {
  // throw e;
  // } catch (CoreException e) {
  // throw new DartModelException(e);
  // }
  //
  // // update new resource content
  // if (edit != null){
  // boolean wasReadOnly = destFile.isReadOnly();
  // try {
  // saveContent(dest, destName, edit, sourceEncoding, destFile);
  // } catch (CoreException e) {
  // if (e instanceof DartModelException) throw (DartModelException) e;
  // throw new DartModelException(e);
  // } finally {
  // Util.setReadOnly(destFile, wasReadOnly);
  // }
  // }
  //
  // // register the correct change deltas
  // prepareDeltas(source, destCU, isMove());
  // if (newCUName != null) {
  // //the main type has been renamed
  // String oldName =
  // Util.getNameWithoutJavaLikeExtension(source.getElementName());
  // String newName = Util.getNameWithoutJavaLikeExtension(newCUName);
  // prepareDeltas(source.getType(oldName), destCU.getType(newName), isMove());
  // }
  // } else {
  // if (!this.force) {
  // throw new DartModelException(new DartModelStatusImpl(
  // DartModelStatusConstants.NAME_COLLISION,
  // Messages.bind(Messages.status_nameCollision,
  // destFile.getFullPath().toString())));
  // }
  // // update new resource content
  // // in case we do a saveas on the same resource we have to simply update the
  // contents
  // // see http://dev.eclipse.org/bugs/show_bug.cgi?id=9351
  // if (edit != null){
  // saveContent(dest, destName, edit, sourceEncoding, destFile);
  // }
  // }
  // }

  /**
   * Process all of the changed deltas generated by this operation.
   */
  protected void processDeltas() {
    for (Iterator<DartElementDeltaImpl> deltas = deltasPerProject.values().iterator(); deltas.hasNext();) {
      addDelta(deltas.next());
    }
  }

  /**
   * @see MultiOperation This method delegates to <code>processCompilationUnitResource</code> or
   *      <code>processPackageFragmentResource</code>, depending on the type of <code>element</code>
   *      .
   */
  @Override
  protected void processElement(DartElement element) throws DartModelException {
    DartCore.notYetImplemented();
    // DartElement dest = getDestinationParent(element);
    // switch (element.getElementType()) {
    // case DartElement.COMPILATION_UNIT :
    // processCompilationUnitResource((CompilationUnit) element,
    // (PackageFragment) dest);
    // this.createdElements.add(((IPackageFragment)
    // dest).getCompilationUnit(element.getElementName()));
    // break;
    // case DartElement.PACKAGE_FRAGMENT :
    // processPackageFragmentResource((PackageFragment) element,
    // (PackageFragmentRoot) dest, getNewNameFor(element));
    // break;
    // default :
    // throw new DartModelException(new
    // DartModelStatusImpl(DartModelStatusConstants.INVALID_ELEMENT_TYPES,
    // element));
    // }
  }

  /**
   * @see MultiOperation Overridden to allow special processing of <code>DartElementDeltaImpl</code>
   *      s and <code>fResultElements</code>.
   */
  @Override
  protected void processElements() throws DartModelException {
    this.createdElements = new ArrayList<DartElement>(elementsToProcess.length);
    try {
      super.processElements();
    } catch (DartModelException jme) {
      throw jme;
    } finally {
      resultElements = new DartElement[createdElements.size()];
      createdElements.toArray(resultElements);
      processDeltas();
    }
  }

  /**
   * Copies/moves a package fragment with the name <code>newName</code> to the destination package.<br>
   * 
   * @exception DartModelException if the operation is unable to complete
   */
  // private void processPackageFragmentResource(PackageFragment source,
  // PackageFragmentRoot root, String newName) throws DartModelException {
  // try {
  // String[] newFragName = null; //(newName == null) ? source.names :
  // Util.getTrimmedSimpleNames(newName);
  // PackageFragment newFrag = root.getPackageFragment(newFragName);
  // IResource[] resources = collectResourcesOfInterest(source);
  //
  // // if isMove() can we move the folder itself ? (see
  // http://bugs.eclipse.org/bugs/show_bug.cgi?id=22458)
  // boolean shouldMoveFolder = isMove() && !newFrag.resource().exists(); // if
  // new pkg fragment exists, it is an override
  // IFolder srcFolder = (IFolder)source.resource();
  // IPath destPath = newFrag.getPath();
  // if (shouldMoveFolder) {
  // // check if destination is not included in source
  // if (srcFolder.getFullPath().isPrefixOf(destPath)) {
  // shouldMoveFolder = false;
  // } else {
  // // check if there are no sub-packages
  // IResource[] members = srcFolder.members();
  // for (int i = 0; i < members.length; i++) {
  // if ( members[i] instanceof IFolder) {
  // shouldMoveFolder = false;
  // break;
  // }
  // }
  // }
  // }
  // boolean containsReadOnlySubPackageFragments =
  // createNeededPackageFragments((IContainer) source.parent.resource(), root,
  // newFragName, shouldMoveFolder);
  // DartCore.notYetImplemented();
  // boolean sourceIsReadOnly = Util.isReadOnly(srcFolder);
  //
  // // Process resources
  // if (shouldMoveFolder) {
  // // move underlying resource
  // // TODO Revisit once bug 43044 is fixed
  // if (sourceIsReadOnly) {
  // Util.setReadOnly(srcFolder, false);
  // }
  // srcFolder.move(destPath, this.force, true /* keep history */,
  // getSubProgressMonitor(1));
  // if (sourceIsReadOnly) {
  // Util.setReadOnly(srcFolder, true);
  // }
  // setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
  // } else {
  // // process the leaf resources
  // if (resources.length > 0) {
  // if (isRename()) {
  // if (! destPath.equals(source.getPath())) {
  // moveResources(resources, destPath);
  // }
  // } else if (isMove()) {
  // // we need to delete this resource if this operation wants to override
  // existing resources
  // for (int i = 0, max = resources.length; i < max; i++) {
  // IResource destinationResource =
  // ResourcesPlugin.getWorkspace().getRoot().findMember(destPath.append(resources[i].getName()));
  // if (destinationResource != null) {
  // if (this.force) {
  // deleteResource(destinationResource, IResource.KEEP_HISTORY);
  // } else {
  // throw new DartModelException(new DartModelStatusImpl(
  // DartModelStatusConstants.NAME_COLLISION,
  // Messages.bind(Messages.status_nameCollision,
  // destinationResource.getFullPath().toString())));
  // }
  // }
  // }
  // moveResources(resources, destPath);
  // } else {
  // // we need to delete this resource if this operation wants to override
  // existing resources
  // for (int i = 0, max = resources.length; i < max; i++) {
  // IResource destinationResource =
  // ResourcesPlugin.getWorkspace().getRoot().findMember(destPath.append(resources[i].getName()));
  // if (destinationResource != null) {
  // if (this.force) {
  // // we need to delete this resource if this operation wants to override
  // existing resources
  // deleteResource(destinationResource, IResource.KEEP_HISTORY);
  // } else {
  // throw new DartModelException(new DartModelStatusImpl(
  // DartModelStatusConstants.NAME_COLLISION,
  // Messages.bind(Messages.status_nameCollision,
  // destinationResource.getFullPath().toString())));
  // }
  // }
  // }
  // copyResources(resources, destPath);
  // }
  // }
  // }
  //
  // // Update package statement in compilation unit if needed
  // if (!Util.equalArraysOrNull(newFragName, source.names)) { // if package has
  // been renamed, update the compilation units
  // char[][] inclusionPatterns = root.fullInclusionPatternChars();
  // char[][] exclusionPatterns = root.fullExclusionPatternChars();
  // for (int i = 0; i < resources.length; i++) {
  // String resourceName = resources[i].getName();
  // if (Util.isDartLikeFileName(resourceName)) {
  // // we only consider potential compilation units
  // CompilationUnit cu = newFrag.getCompilationUnit(resourceName);
  // if (Util.isExcluded(cu.getPath(), inclusionPatterns, exclusionPatterns,
  // false/*not a folder*/)) continue;
  // parser.setSource(cu);
  // CompilationUnit astCU = (CompilationUnit)
  // parser.createAST(this.progressMonitor);
  // AST ast = astCU.getAST();
  // ASTRewrite rewrite = ASTRewrite.create(ast);
  // updatePackageStatement(astCU, newFragName, rewrite, cu);
  // TextEdit edits = rewrite.rewriteAST();
  // applyTextEdit(cu, edits);
  // cu.save(null, false);
  // }
  // }
  // }
  //
  // // Discard empty old package (if still empty after the rename)
  // boolean isEmpty = true;
  // if (isMove()) {
  // // delete remaining files in this package (.class file in the case where
  // Proj=src=bin)
  // // in case of a copy
  // updateReadOnlyPackageFragmentsForMove((IContainer)
  // source.parent.resource(), root, newFragName, sourceIsReadOnly);
  // if (srcFolder.exists()) {
  // IResource[] remaining = srcFolder.members();
  // for (int i = 0, length = remaining.length; i < length; i++) {
  // IResource file = remaining[i];
  // if (file instanceof IFile) {
  // DartCore.notYetImplemented();
  // if (Util.isReadOnly(file)) {
  // Util.setReadOnly(file, false);
  // }
  // deleteResource(file, IResource.FORCE | IResource.KEEP_HISTORY);
  // } else {
  // isEmpty = false;
  // }
  // }
  // }
  // if (isEmpty) {
  // IResource rootResource;
  // // check if source is included in destination
  // if (destPath.isPrefixOf(srcFolder.getFullPath())) {
  // rootResource = newFrag.resource();
  // } else {
  // rootResource = source.parent.resource();
  // }
  //
  // // delete recursively empty folders
  // deleteEmptyPackageFragment(source, false, rootResource);
  // }
  // } else if (containsReadOnlySubPackageFragments) {
  // // in case of a copy
  // updateReadOnlyPackageFragmentsForCopy((IContainer)
  // source.parent.resource(), root, newFragName);
  // }
  // // workaround for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=24505
  // if (isEmpty && isMove() && !(Util.isExcluded(source) ||
  // Util.isExcluded(newFrag))) {
  // DartProject sourceProject = source.getJavaProject();
  // getDeltaFor(sourceProject).movedFrom(source, newFrag);
  // DartProject destProject = newFrag.getJavaProject();
  // getDeltaFor(destProject).movedTo(newFrag, source);
  // }
  // } catch (DartModelException e) {
  // throw e;
  // } catch (CoreException ce) {
  // throw new DartModelException(ce);
  // }
  // }

  // private void saveContent(PackageFragment dest, String destName, TextEdit
  // edits, String sourceEncoding, IFile destFile) throws DartModelException {
  // try {
  // // TODO (frederic) remove when bug 67606 will be fixed (bug 67823)
  // // fix bug 66898
  // if (sourceEncoding != null) destFile.setCharset(sourceEncoding,
  // this.progressMonitor);
  // // end todo
  // }
  // catch (CoreException ce) {
  // // use no encoding
  // }
  // // when the file was copied, its read-only flag was preserved -> temporary
  // set it to false
  // // note this doesn't interfere with repository providers as this is a new
  // resource that cannot be under
  // // version control yet
  // Util.setReadOnly(destFile, false);
  // CompilationUnit destCU = dest.getCompilationUnit(destName);
  // applyTextEdit(destCU, edits);
  // destCU.save(getSubProgressMonitor(1), this.force);
  // }

  /**
   * Updates the content of <code>cu</code>, modifying the type name and/or package declaration as
   * necessary.
   * 
   * @return an AST rewrite or null if no rewrite needed
   */
  // private TextEdit updateContent(CompilationUnit cu, PackageFragment dest,
  // String newName) throws DartModelException {
  // String[] currPackageName = ((PackageFragment) cu.getParent()).names;
  // String[] destPackageName = dest.names;
  // if (Util.equalArraysOrNull(currPackageName, destPackageName) && newName ==
  // null) {
  // return null; //nothing to change
  // } else {
  // // ensure cu is consistent (noop if already consistent)
  // cu.makeConsistent(this.progressMonitor);
  // parser.setSource(cu);
  // CompilationUnit astCU = (CompilationUnit)
  // parser.createAST(this.progressMonitor);
  // AST ast = astCU.getAST();
  // ASTRewrite rewrite = ASTRewrite.create(ast);
  // updateTypeName(cu, astCU, cu.getElementName(), newName, rewrite);
  // updatePackageStatement(astCU, destPackageName, rewrite, cu);
  // return rewrite.rewriteAST();
  // }
  // }

  // private void updatePackageStatement(CompilationUnit astCU, String[]
  // pkgName, ASTRewrite rewriter, CompilationUnit cu) throws DartModelException
  // {
  // boolean defaultPackage = pkgName.length == 0;
  // AST ast = astCU.getAST();
  // if (defaultPackage) {
  // // remove existing package statement
  // PackageDeclaration pkg = astCU.getPackage();
  // if (pkg != null) {
  // int pkgStart;
  // Javadoc javadoc = pkg.getJavadoc();
  // if (javadoc != null) {
  // pkgStart = javadoc.getStartPosition() + javadoc.getLength() + 1;
  // } else {
  // pkgStart = pkg.getStartPosition();
  // }
  // int extendedStart = astCU.getExtendedStartPosition(pkg);
  // if (pkgStart != extendedStart) {
  // // keep the comments associated with package declaration
  // // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=247757
  // String commentSource = cu.getSource().substring(extendedStart, pkgStart);
  // ASTNode comment = rewriter.createStringPlaceholder(commentSource,
  // ASTNode.PACKAGE_DECLARATION);
  // rewriter.set(astCU, CompilationUnit.PACKAGE_PROPERTY, comment, null);
  // } else {
  // rewriter.set(astCU, CompilationUnit.PACKAGE_PROPERTY, null, null);
  // }
  // }
  // } else {
  // org.eclipse.jdt.core.dom.PackageDeclaration pkg = astCU.getPackage();
  // if (pkg != null) {
  // // rename package statement
  // Name name = ast.newName(pkgName);
  // rewriter.set(pkg, PackageDeclaration.NAME_PROPERTY, name, null);
  // } else {
  // // create new package statement
  // pkg = ast.newPackageDeclaration();
  // pkg.setName(ast.newName(pkgName));
  // rewriter.set(astCU, CompilationUnit.PACKAGE_PROPERTY, pkg, null);
  // }
  // }
  // }

  // private void updateReadOnlyPackageFragmentsForCopy(IContainer sourceFolder,
  // PackageFragmentRoot root, String[] newFragName) {
  // IContainer parentFolder = (IContainer) root.resource();
  // for (int i = 0, length = newFragName.length; i <length; i++) {
  // String subFolderName = newFragName[i];
  // parentFolder = parentFolder.getFolder(new Path(subFolderName));
  // sourceFolder = sourceFolder.getFolder(new Path(subFolderName));
  // if (sourceFolder.exists() && Util.isReadOnly(sourceFolder)) {
  // Util.setReadOnly(parentFolder, true);
  // }
  // }
  // }

  // private void updateReadOnlyPackageFragmentsForMove(IContainer sourceFolder,
  // PackageFragmentRoot root, String[] newFragName, boolean
  // sourceFolderIsReadOnly) {
  // IContainer parentFolder = (IContainer) root.resource();
  // for (int i = 0, length = newFragName.length; i < length; i++) {
  // String subFolderName = newFragName[i];
  // parentFolder = parentFolder.getFolder(new Path(subFolderName));
  // sourceFolder = sourceFolder.getFolder(new Path(subFolderName));
  // if ((sourceFolder.exists() && Util.isReadOnly(sourceFolder)) || (i ==
  // length - 1 && sourceFolderIsReadOnly)) {
  // Util.setReadOnly(parentFolder, true);
  // // the source folder will be deleted anyway (move operation)
  // Util.setReadOnly(sourceFolder, false);
  // }
  // }
  // }

  /**
   * Renames the main type in <code>cu</code>.
   */
  // private void updateTypeName(CompilationUnit cu, CompilationUnit astCU,
  // String oldName, String newName, ASTRewrite rewriter) throws
  // DartModelException {
  // if (newName != null) {
  // String oldTypeName= Util.getNameWithoutJavaLikeExtension(oldName);
  // String newTypeName= Util.getNameWithoutJavaLikeExtension(newName);
  // AST ast = astCU.getAST();
  // // update main type name
  // IType[] types = cu.getTypes();
  // for (int i = 0, max = types.length; i < max; i++) {
  // IType currentType = types[i];
  // if (currentType.getElementName().equals(oldTypeName)) {
  // AbstractTypeDeclaration typeNode = (AbstractTypeDeclaration)
  // ((DartElementImpl) currentType).findNode(astCU);
  // if (typeNode != null) {
  // // rename type
  // rewriter.replace(typeNode.getName(), ast.newSimpleName(newTypeName), null);
  // // rename constructors
  // Iterator bodyDeclarations = typeNode.bodyDeclarations().iterator();
  // while (bodyDeclarations.hasNext()) {
  // Object bodyDeclaration = bodyDeclarations.next();
  // if (bodyDeclaration instanceof MethodDeclaration) {
  // MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
  // if (methodDeclaration.isConstructor()) {
  // SimpleName methodName = methodDeclaration.getName();
  // if (methodName.getIdentifier().equals(oldTypeName)) {
  // rewriter.replace(methodName, ast.newSimpleName(newTypeName), null);
  // }
  // }
  // }
  // }
  // }
  // }
  // }
  // }
  // }

  /**
   * Possible failures:
   * <ul>
   * <li>NO_ELEMENTS_TO_PROCESS - no elements supplied to the operation
   * <li>INDEX_OUT_OF_BOUNDS - the number of renamings supplied to the operation does not match the
   * number of elements that were supplied.
   * </ul>
   */
  @Override
  protected DartModelStatus verify() {
    DartModelStatus status = super.verify();
    if (!status.isOK()) {
      return status;
    }
    if (renamingsList != null && renamingsList.length != elementsToProcess.length) {
      return new DartModelStatusImpl(DartModelStatusConstants.INDEX_OUT_OF_BOUNDS);
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  @Override
  protected void verify(DartElement element) throws DartModelException {
    if (element == null || !element.exists()) {
      error(DartModelStatusConstants.ELEMENT_DOES_NOT_EXIST, element);
    }
    DartCore.notYetImplemented();
    // if (element.isReadOnly() && (isRename() || isMove())) {
    // error(DartModelStatusConstants.READ_ONLY, element);
    // }
    // IResource resource = ((DartElementImpl) element).resource();
    // if (resource instanceof IFolder) {
    // if (resource.isLinked()) {
    // error(DartModelStatusConstants.INVALID_RESOURCE, element);
    // }
    // }

    int elementType = element.getElementType();

    if (elementType == DartElement.COMPILATION_UNIT) {
      CompilationUnitImpl compilationUnit = (CompilationUnitImpl) element;
      if (isMove() && compilationUnit.isWorkingCopy() && !compilationUnit.isPrimary()) {
        error(DartModelStatusConstants.INVALID_ELEMENT_TYPES, element);
        // } else if (elementType != DartElement.PACKAGE_FRAGMENT) {
        // error(DartModelStatusConstants.INVALID_ELEMENT_TYPES, element);
      }
    }

    DartElementImpl dest = (DartElementImpl) getDestinationParent(element);
    verifyDestination(element, dest);
    if (this.renamings != null) {
      verifyRenaming(element);
    }
  }

  /**
   * Returns the children of <code>source</code> which are affected by this operation. If
   * <code>source</code> is a <code>K_SOURCE</code>, these are the <code>.java</code> files, if it
   * is a <code>K_BINARY</code>, they are the <code>.class</code> files.
   */
  // private IResource[] collectResourcesOfInterest(IPackageFragment source)
  // throws DartModelException {
  // DartElement[] children = source.getChildren();
  // int childOfInterest = DartElement.COMPILATION_UNIT;
  // if (source.getKind() == IPackageFragmentRoot.K_BINARY) {
  // childOfInterest = DartElement.CLASS_FILE;
  // }
  // ArrayList correctKindChildren = new ArrayList(children.length);
  // for (int i = 0; i < children.length; i++) {
  // DartElement child = children[i];
  // if (child.getElementType() == childOfInterest) {
  // correctKindChildren.add(((DartElementImpl) child).resource());
  // }
  // }
  // // Gather non-java resources
  // Object[] nonJavaResources = source.getNonJavaResources();
  // int actualNonJavaResourceCount = 0;
  // for (int i = 0, max = nonJavaResources.length; i < max; i++){
  // if (nonJavaResources[i] instanceof IResource) actualNonJavaResourceCount++;
  // }
  // IResource[] actualNonJavaResources = new
  // IResource[actualNonJavaResourceCount];
  // for (int i = 0, max = nonJavaResources.length, index = 0; i < max; i++){
  // if (nonJavaResources[i] instanceof IResource)
  // actualNonJavaResources[index++] = (IResource)nonJavaResources[i];
  // }
  //
  // if (actualNonJavaResourceCount != 0) {
  // int correctKindChildrenSize = correctKindChildren.size();
  // IResource[] result = new IResource[correctKindChildrenSize +
  // actualNonJavaResourceCount];
  // correctKindChildren.toArray(result);
  // System.arraycopy(actualNonJavaResources, 0, result,
  // correctKindChildrenSize, actualNonJavaResourceCount);
  // return result;
  // } else {
  // IResource[] result = new IResource[correctKindChildren.size()];
  // correctKindChildren.toArray(result);
  // return result;
  // }
  // }

  /**
   * Creates any destination package fragment(s) which do not exists yet. Return true if a read-only
   * package fragment has been found among package fragments, false otherwise
   */
  // private boolean createNeededPackageFragments(IContainer sourceFolder,
  // PackageFragmentRoot root, String[] newFragName, boolean moveFolder) throws
  // DartModelException {
  // boolean containsReadOnlyPackageFragment = false;
  // IContainer parentFolder = (IContainer) root.resource();
  // DartElementDeltaImpl projectDelta = null;
  // String[] sideEffectPackageName = null;
  // char[][] inclusionPatterns = root.fullInclusionPatternChars();
  // char[][] exclusionPatterns = root.fullExclusionPatternChars();
  // for (int i = 0; i < newFragName.length; i++) {
  // String subFolderName = newFragName[i];
  // sideEffectPackageName = Util.arrayConcat(sideEffectPackageName,
  // subFolderName);
  // IResource subFolder = parentFolder.findMember(subFolderName);
  // if (subFolder == null) {
  // // create deepest folder only if not a move (folder will be moved in
  // processPackageFragmentResource)
  // if (!(moveFolder && i == newFragName.length-1)) {
  // createFolder(parentFolder, subFolderName, this.force);
  // }
  // parentFolder = parentFolder.getFolder(new Path(subFolderName));
  // sourceFolder = sourceFolder.getFolder(new Path(subFolderName));
  // if (Util.isReadOnly(sourceFolder)) {
  // containsReadOnlyPackageFragment = true;
  // }
  // IPackageFragment sideEffectPackage =
  // root.getPackageFragment(sideEffectPackageName);
  // if (i < newFragName.length - 1 // all but the last one are side effect
  // packages
  // && !Util.isExcluded(parentFolder, inclusionPatterns, exclusionPatterns)) {
  // if (projectDelta == null) {
  // projectDelta = getDeltaFor(root.getJavaProject());
  // }
  // projectDelta.added(sideEffectPackage);
  // }
  // this.createdElements.add(sideEffectPackage);
  // } else {
  // parentFolder = (IContainer) subFolder;
  // }
  // }
  // return containsReadOnlyPackageFragment;
  // }

  /**
   * Returns the <code>DartElementDeltaImpl</code> for <code>javaProject</code>, creating it and
   * putting it in <code>fDeltasPerProject</code> if it does not exist yet.
   */
  private DartElementDeltaImpl getDeltaFor(DartProject javaProject) {
    DartElementDeltaImpl delta = deltasPerProject.get(javaProject);
    if (delta == null) {
      delta = new DartElementDeltaImpl(javaProject);
      deltasPerProject.put(javaProject, delta);
    }
    return delta;
  }

  private IResource getResource(DartElement element) {
    if (element == null) {
      return null;
    }
    DartCore.notYetImplemented();
    // if (element.getElementType() == DartElement.PACKAGE_FRAGMENT) {
    // String pkgName = element.getElementName();
    // int firstDot = pkgName.indexOf('.');
    // if (firstDot != -1) {
    // element = ((IPackageFragmentRoot)
    // element.getParent()).getPackageFragment(pkgName.substring(0, firstDot));
    // }
    // }
    return element.getResource();
  }

  private ISchedulingRule getSchedulingRule(DartElement element) {
    if (element == null) {
      return null;
    }
    IResource sourceResource = getResource(element);
    IResource destContainer = getResource(getDestinationParent(element));
    if (!(destContainer instanceof IContainer)) {
      return null;
    }
    String newName = getNewNameFor(element);
    if (newName == null) {
      newName = element.getElementName();
    }
    IResource destResource;
    String sourceEncoding = null;
    if (sourceResource.getType() == IResource.FILE) {
      destResource = ((IContainer) destContainer).getFile(new Path(newName));
      try {
        sourceEncoding = ((IFile) sourceResource).getCharset(false);
      } catch (CoreException ce) {
        // use default encoding
      }
    } else {
      destResource = ((IContainer) destContainer).getFolder(new Path(newName));
    }
    IResourceRuleFactory factory = ResourcesPlugin.getWorkspace().getRuleFactory();
    ISchedulingRule rule;
    if (isMove()) {
      rule = factory.moveRule(sourceResource, destResource);
    } else {
      rule = factory.copyRule(sourceResource, destResource);
    }
    if (sourceEncoding != null) {
      rule = new MultiRule(new ISchedulingRule[] {rule, factory.charsetRule(destResource)});
    }
    return rule;
  }

  private void initializeASTParser() {
    DartCore.notYetImplemented();
    // parser = ASTParser.newParser(AST.JLS3);
  }
}
