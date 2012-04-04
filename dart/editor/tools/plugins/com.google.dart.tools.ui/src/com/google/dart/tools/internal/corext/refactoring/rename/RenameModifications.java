package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.internal.corext.refactoring.participants.ResourceModifications;
import com.google.dart.tools.internal.corext.refactoring.reorg.RefactoringModifications;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.IParticipantDescriptorFilter;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameModifications extends RefactoringModifications {

  private final List<Object> fRename;
  private final List<RefactoringArguments> fRenameArguments;
  private final List<IParticipantDescriptorFilter> fParticipantDescriptorFilter;

  public RenameModifications() {
    fRename = new ArrayList<Object>();
    fRenameArguments = new ArrayList<RefactoringArguments>();
    fParticipantDescriptorFilter = new ArrayList<IParticipantDescriptorFilter>();
  }

  @Override
  public void buildDelta(IResourceChangeDescriptionFactory builder) {
    for (int i = 0; i < fRename.size(); i++) {
      Object element = fRename.get(i);
      if (element instanceof IResource) {
        ResourceModifications.buildMoveDelta(
            builder,
            (IResource) element,
            (RenameArguments) fRenameArguments.get(i));
      }
    }
    getResourceModifications().buildDelta(builder);
  }

  @Override
  public void buildValidateEdits(ValidateEditChecker checker) {
    for (Iterator<Object> iter = fRename.iterator(); iter.hasNext();) {
      Object element = iter.next();
      if (element instanceof CompilationUnit) {
        CompilationUnit unit = (CompilationUnit) element;
        IResource resource = unit.getResource();
        if (resource != null && resource.getType() == IResource.FILE) {
          checker.addFile((IFile) resource);
        }
      }
    }
  }

//  public void rename(IPackageFragmentRoot sourceFolder, RenameArguments arguments) {
//    add(sourceFolder, arguments, null);
//    if (sourceFolder.getResource() != null) {
//      getResourceModifications().addRename(sourceFolder.getResource(), arguments);
//    }
//  }

//  public void rename(IPackageFragment rootPackage, RenameArguments args, boolean renameSubPackages)
//      throws CoreException {
//    add(rootPackage, args, null);
//    IPackageFragment[] allSubPackages = null;
//    if (renameSubPackages) {
//      allSubPackages = getSubpackages(rootPackage);
//      for (int i = 0; i < allSubPackages.length; i++) {
//        IPackageFragment pack = allSubPackages[i];
//        RenameArguments subArgs =
//            new RenameArguments(getNewPackageName(
//                rootPackage,
//                args.getNewName(),
//                pack.getElementName()), args.getUpdateReferences());
//        add(pack, subArgs, null);
//      }
//    }
//    IContainer container = (IContainer) rootPackage.getResource();
//    if (container == null)
//      return;
//    IContainer target =
//        (IContainer) ((IPackageFragmentRoot) rootPackage.getParent()).getPackageFragment(
//            args.getNewName()).getResource();
//    if ((!rootPackage.hasSubpackages() || renameSubPackages) && canMove(container, target)) {
//      createIncludingParents(target.getParent());
//      if (container.getParent().equals(target.getParent())) {
//        getResourceModifications().addRename(
//            container,
//            new RenameArguments(target.getName(), args.getUpdateReferences()));
//      } else {
//        // This is a little tricky. The problem is that the refactoring participants
//        // don't support a generic move like the resource API does. So for the delta
//        // we generate one move, however for the participants we have to generate single
//        // moves and deletes.
//        try {
//          getResourceModifications().ignoreForDelta();
//          addAllResourceModifications(rootPackage, args, renameSubPackages, allSubPackages);
//        } finally {
//          getResourceModifications().trackForDelta();
//        }
//        getResourceModifications().addDelta(
//            new ResourceModifications.MoveDescription(container, target.getFullPath()));
//      }
//    } else {
//      addAllResourceModifications(rootPackage, args, renameSubPackages, allSubPackages);
//    }
//  }

  @Override
  public RefactoringParticipant[] loadParticipants(RefactoringStatus status,
      RefactoringProcessor owner, String[] natures, SharableParticipants shared) {
    List<RefactoringParticipant> result = new ArrayList<RefactoringParticipant>();
    for (int i = 0; i < fRename.size(); i++) {
      result.addAll(Arrays.asList(ParticipantManager.loadRenameParticipants(
          status,
          owner,
          fRename.get(i),
          (RenameArguments) fRenameArguments.get(i),
          fParticipantDescriptorFilter.get(i),
          natures,
          shared)));
    }
    result.addAll(Arrays.asList(getResourceModifications().getParticipants(
        status,
        owner,
        natures,
        shared)));
    return result.toArray(new RefactoringParticipant[result.size()]);
  }

  public void rename(CompilationUnit unit, RenameArguments args) {
    add(unit, args, null);
    if (unit.getResource() != null) {
      getResourceModifications().addRename(
          unit.getResource(),
          new RenameArguments(args.getNewName(), args.getUpdateReferences()));
    }
  }

  public void rename(DartProject project, RenameArguments args) {
    // TODO(scheglov) implement
    throw new RuntimeException("Not implemented");
//    add(project, args, null);
//    IProject rProject = project.getProject();
//    if (rProject != null) {
//      getResourceModifications().addRename(rProject, args);
//      IProject[] referencingProjects = rProject.getReferencingProjects();
//      for (int i = 0; i < referencingProjects.length; i++) {
//        IFile classpath = getClasspathFile(referencingProjects[i]);
//        if (classpath != null) {
//          getResourceModifications().addChanged(classpath);
//        }
//      }
//    }
  }

  public void rename(DartVariableDeclaration variable, RenameArguments args) {
    add(variable, args, null);
  }

  public void rename(Field field, RenameArguments args) {
    add(field, args, null);
  }

//  public void rename(ITypeParameter typeParameter, RenameArguments arguments) {
//  	add(typeParameter, arguments, null);
//  }

  public void rename(IResource resource, RenameArguments args) {
    add(resource, args, null);
  }

  public void rename(Method method, RenameArguments args) {
    add(method, args, null);
  }

  public void rename(Type type, RenameTypeArguments args, IParticipantDescriptorFilter filter) {
    add(type, args, filter);
  }

  private void add(Object element, RefactoringArguments args, IParticipantDescriptorFilter filter) {
    Assert.isNotNull(element);
    Assert.isNotNull(args);
    fRename.add(element);
    fRenameArguments.add(args);
    fParticipantDescriptorFilter.add(filter);
  }

//  private void addAllResourceModifications(IPackageFragment rootPackage,
//      RenameArguments args,
//      boolean renameSubPackages,
//      IPackageFragment[] allSubPackages) throws CoreException {
//    IFolder target = addResourceModifications(rootPackage, args, rootPackage, renameSubPackages);
//    if (renameSubPackages) {
//      IContainer container = (IContainer) rootPackage.getResource();
//      if (container == null)
//        return;
//      boolean removeContainer = !container.contains(target);
//      for (int i = 0; i < allSubPackages.length; i++) {
//        IPackageFragment pack = allSubPackages[i];
//        IFolder subTarget = addResourceModifications(rootPackage, args, pack, renameSubPackages);
//        if (container.contains(subTarget))
//          removeContainer = false;
//      }
//      if (removeContainer) {
//        getResourceModifications().addDelete(container);
//      }
//    }
//  }

//  private IFolder addResourceModifications(IPackageFragment rootPackage,
//      RenameArguments args,
//      IPackageFragment pack,
//      boolean renameSubPackages) throws CoreException {
//    IContainer container = (IContainer) pack.getResource();
//    if (container == null)
//      return null;
//    IFolder target = computeTargetFolder(rootPackage, args, pack);
//    createIncludingParents(target);
//    MoveArguments arguments = new MoveArguments(target, args.getUpdateReferences());
//    IResource[] resourcesToMove = collectResourcesOfInterest(pack);
//    Set<IResource> allMembers = new HashSet<IResource>(Arrays.asList(container.members()));
//    for (int i = 0; i < resourcesToMove.length; i++) {
//      IResource toMove = resourcesToMove[i];
//      getResourceModifications().addMove(toMove, arguments);
//      allMembers.remove(toMove);
//    }
//    for (Iterator<IResource> iter = allMembers.iterator(); iter.hasNext();) {
//      IResource element = iter.next();
//      if (element instanceof IFile) {
//        getResourceModifications().addDelete(element);
//        iter.remove();
//      }
//    }
//    if (!renameSubPackages && allMembers.isEmpty()) {
//      getResourceModifications().addDelete(container);
//    }
//    return target;
//  }

//  private boolean canMove(IContainer source, IContainer target) {
//    return !target.exists() && !source.getFullPath().isPrefixOf(target.getFullPath());
//  }

//  private IPackageFragment[] getSubpackages(IPackageFragment pack) throws CoreException {
//    IPackageFragmentRoot root = (IPackageFragmentRoot) pack.getParent();
//    DartElement[] allPackages = root.getChildren();
//    if (pack.isDefaultPackage())
//      return new IPackageFragment[0];
//    ArrayList<IPackageFragment> result = new ArrayList<IPackageFragment>();
//    String prefix = pack.getElementName() + '.';
//    for (int i = 0; i < allPackages.length; i++) {
//      IPackageFragment currentPackage = (IPackageFragment) allPackages[i];
//      if (currentPackage.getElementName().startsWith(prefix))
//        result.add(currentPackage);
//    }
//    return result.toArray(new IPackageFragment[result.size()]);
//  }

//  private IFolder computeTargetFolder(IPackageFragment rootPackage, RenameArguments args,
//      IPackageFragment pack) {
//    IPath path = pack.getParent().getPath();
//    path = path.append(getNewPackageName(rootPackage, args.getNewName(), pack.getElementName()).replace(
//        '.', IPath.SEPARATOR));
//    IFolder target = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
//    return target;
//  }
//
//  private String getNewPackageName(IPackageFragment rootPackage, String newPackageName,
//      String oldSubPackageName) {
//    String oldPackageName = rootPackage.getElementName();
//    return newPackageName + oldSubPackageName.substring(oldPackageName.length());
//  }
}
