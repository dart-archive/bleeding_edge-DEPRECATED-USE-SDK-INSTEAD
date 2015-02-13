package com.google.dart.tools.ui.internal.projects;

import com.google.common.collect.Lists;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.pub.PubCacheManager_NEW;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.util.List;

/**
 * Provides content for a tree viewer that shows only containers.
 */
public class ContainerContentProvider implements ITreeContentProvider {
  @Override
  public void dispose() {
  }

  @Override
  public Object[] getChildren(Object element) {
    if (element instanceof IWorkspace) {
      List<IProject> accessibleProjects = Lists.newArrayList();
      IProject[] allProjects = ((IWorkspace) element).getRoot().getProjects();
      for (IProject project : allProjects) {
        if (!project.isOpen()) {
          continue;
        }
        if (PubCacheManager_NEW.isPubCacheProject(project)) {
          continue;
        }
        accessibleProjects.add(project);
      }
      return accessibleProjects.toArray();
    } else if (element instanceof IContainer) {
      IContainer container = (IContainer) element;
      if (container.isAccessible()) {
        try {
          List<IResource> children = Lists.newArrayList();
          IResource[] members = container.members();
          for (IResource member : members) {
            if (member.getType() == IResource.FILE) {
              continue;
            }
            if (DartCore.PACKAGES_DIRECTORY_NAME.equals(member.getName())) {
              continue;
            }
            children.add(member);
          }
          return children.toArray();
        } catch (CoreException e) {
          // this should never happen because we call #isAccessible before invoking #members
        }
      }
    }
    return new Object[0];
  }

  @Override
  public Object[] getElements(Object element) {
    return getChildren(element);
  }

  @Override
  public Object getParent(Object element) {
    if (element instanceof IResource) {
      return ((IResource) element).getParent();
    }
    return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  }
}
