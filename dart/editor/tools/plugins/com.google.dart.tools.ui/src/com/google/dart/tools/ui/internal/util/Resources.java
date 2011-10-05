/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.util;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.text.DartStatusConstants;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Resources {

  /**
   * Checks if the given resource is in sync with the underlying file system.
   * 
   * @param resource the resource to be checked
   * @return IStatus status describing the check's result. If <code>status.
   * isOK()</code> returns <code>true</code> then the resource is in sync
   */
  public static IStatus checkInSync(IResource resource) {
    return checkInSync(new IResource[] {resource});
  }

  /**
   * Checks if the given resources are in sync with the underlying file system.
   * 
   * @param resources the resources to be checked
   * @return IStatus status describing the check's result. If <code>status.
   *  isOK() </code> returns <code>true</code> then the resources are in sync
   */
  public static IStatus checkInSync(IResource[] resources) {
    IStatus result = null;
    for (int i = 0; i < resources.length; i++) {
      IResource resource = resources[i];
      if (!resource.isSynchronized(IResource.DEPTH_INFINITE)) {
        result = addOutOfSync(result, resource);
      }
    }
    if (result != null) {
      return result;
    }
    return new Status(IStatus.OK, DartToolsPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$		
  }

  /**
   * This method is used to generate a list of local locations to be used in DnD for file transfers.
   * 
   * @param resources the array of resources to get the local locations for
   * @return the local locations
   */
  public static String[] getLocationOSStrings(IResource[] resources) {
    List<String> result = new ArrayList<String>(resources.length);
    for (int i = 0; i < resources.length; i++) {
      IPath location = resources[i].getLocation();
      if (location != null) {
        result.add(location.toOSString());
      }
    }
    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the location of the given resource. For local resources this is the OS path in the
   * local file system. For remote resource this is the URI.
   * 
   * @param resource the resource
   * @return the location string or <code>null</code> if the location URI of the resource is
   *         <code>null</code>
   */
  public static String getLocationString(IResource resource) {
    URI uri = resource.getLocationURI();
    if (uri == null) {
      return null;
    }
    return EFS.SCHEME_FILE.equalsIgnoreCase(uri.getScheme()) ? new File(uri).getAbsolutePath()
        : uri.toString();
  }

  public static boolean isReadOnly(IResource resource) {
    ResourceAttributes resourceAttributes = resource.getResourceAttributes();
    if (resourceAttributes == null) {
      // resource
      return false;
    }
    return resourceAttributes.isReadOnly();
  }

  /**
   * Makes the given resource committable. Committable means that it is writeable and that its
   * content hasn't changed by calling <code>validateEdit</code> for the given resource on
   * <tt>IWorkspace</tt>.
   * 
   * @param resource the resource to be checked
   * @param context the context passed to <code>validateEdit</code>
   * @return status describing the method's result. If <code>status.isOK()</code> returns
   *         <code>true</code> then the resources are committable.
   * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[],
   *      java.lang.Object)
   */
  public static IStatus makeCommittable(IResource resource, Object context) {
    return makeCommittable(new IResource[] {resource}, context);
  }

  /**
   * Makes the given resources committable. Committable means that all resources are writeable and
   * that the content of the resources hasn't changed by calling <code>validateEdit</code> for a
   * given file on <tt>IWorkspace</tt>.
   * 
   * @param resources the resources to be checked
   * @param context the context passed to <code>validateEdit</code>
   * @return IStatus status describing the method's result. If <code>status.
   * isOK()</code> returns <code>true</code> then the add resources are committable
   * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[],
   *      java.lang.Object)
   */
  public static IStatus makeCommittable(IResource[] resources, Object context) {
    List<IResource> readOnlyFiles = new ArrayList<IResource>();
    for (int i = 0; i < resources.length; i++) {
      IResource resource = resources[i];
      if (resource.getType() == IResource.FILE && isReadOnly(resource)) {
        readOnlyFiles.add(resource);
      }
    }
    if (readOnlyFiles.size() == 0) {
      return new Status(IStatus.OK, DartToolsPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
    }

    Map<IFile, Long> oldTimeStamps = createModificationStampMap(readOnlyFiles);
    IStatus status = ResourcesPlugin.getWorkspace().validateEdit(
        readOnlyFiles.toArray(new IFile[readOnlyFiles.size()]), context);
    if (!status.isOK()) {
      return status;
    }

    IStatus modified = null;
    Map<IFile, Long> newTimeStamps = createModificationStampMap(readOnlyFiles);
    for (Iterator<IFile> iter = oldTimeStamps.keySet().iterator(); iter.hasNext();) {
      IFile file = iter.next();
      if (!oldTimeStamps.get(file).equals(newTimeStamps.get(file))) {
        modified = addModified(modified, file);
      }
    }
    if (modified != null) {
      return modified;
    }
    return new Status(IStatus.OK, DartToolsPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
  }

  static void setReadOnly(IResource resource, boolean readOnly) {
    ResourceAttributes resourceAttributes = resource.getResourceAttributes();
    if (resourceAttributes == null) {
      // resource
      return;
    }

    resourceAttributes.setReadOnly(readOnly);
    try {
      resource.setResourceAttributes(resourceAttributes);
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    }
  }

  private static IStatus addModified(IStatus status, IFile file) {
    IStatus entry = new Status(IStatus.ERROR, DartToolsPlugin.PLUGIN_ID,
        DartStatusConstants.VALIDATE_EDIT_CHANGED_CONTENT, Messages.format(
            CorextMessages.Resources_fileModified, file.getFullPath().toString()), null);
    if (status == null) {
      return entry;
    } else if (status.isMultiStatus()) {
      ((MultiStatus) status).add(entry);
      return status;
    } else {
      MultiStatus result = new MultiStatus(DartToolsPlugin.PLUGIN_ID,
          DartStatusConstants.VALIDATE_EDIT_CHANGED_CONTENT,
          CorextMessages.Resources_modifiedResources, null);
      result.add(status);
      result.add(entry);
      return result;
    }
  }

  private static IStatus addOutOfSync(IStatus status, IResource resource) {
    IStatus entry = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES,
        IResourceStatus.OUT_OF_SYNC_LOCAL, Messages.format(CorextMessages.Resources_outOfSync,
            resource.getFullPath().toString()), null);
    if (status == null) {
      return entry;
    } else if (status.isMultiStatus()) {
      ((MultiStatus) status).add(entry);
      return status;
    } else {
      MultiStatus result = new MultiStatus(ResourcesPlugin.PI_RESOURCES,
          IResourceStatus.OUT_OF_SYNC_LOCAL, CorextMessages.Resources_outOfSyncResources, null);
      result.add(status);
      result.add(entry);
      return result;
    }
  }

  private static Map<IFile, Long> createModificationStampMap(List<IResource> files) {
    Map<IFile, Long> map = new HashMap<IFile, Long>();
    for (Iterator<IResource> iter = files.iterator(); iter.hasNext();) {
      IFile file = (IFile) iter.next();
      map.put(file, new Long(file.getModificationStamp()));
    }
    return map;
  }

  private Resources() {
  }
}
