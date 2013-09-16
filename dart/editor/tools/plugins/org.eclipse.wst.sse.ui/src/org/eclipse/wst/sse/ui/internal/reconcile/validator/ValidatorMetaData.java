/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile.validator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.reconcile.ReconcileAnnotationKey;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Object that holds information relevant to the creation of a validator for the reconciling
 * framework.
 * 
 * @author pavery,nitind
 */
public class ValidatorMetaData {
  private String fClass = null;
  private IConfigurationElement fConfigurationElement = null;
  private String fId = null;
  private String fScope;

  // a hash map of explicitly declared content type Ids (String) that points to lists of
  // partition types (List of Strings)
  // contentTypeId -> List(paritionType, paritionType, partitionType, ...)
  // contentTypeId2 -> List(partitionType, partitionType, ...)
  // ...
  private HashMap fMatrix = null;

  public ValidatorMetaData(IConfigurationElement element, String vId, String vClass, String vScope) {
    fId = vId;
    fClass = vClass;
    fConfigurationElement = element;
    fScope = vScope;
    fMatrix = new HashMap();
  }

  /**
   * TODO: This exact method is also in ValidatorStrategy. Should be in a common place.
   * 
   * @param contentTypeId
   * @return
   */
  private String[] calculateParentContentTypeIds(String contentTypeId) {

    Set parentTypes = new HashSet();

    IContentTypeManager ctManager = Platform.getContentTypeManager();
    IContentType ct = ctManager.getContentType(contentTypeId);
    String id = contentTypeId;

    while (ct != null && id != null) {

      parentTypes.add(id);
      ct = ctManager.getContentType(id);
      if (ct != null) {
        IContentType baseType = ct.getBaseType();
        id = (baseType != null) ? baseType.getId() : null;
      }
    }
    return (String[]) parentTypes.toArray(new String[parentTypes.size()]);
  }

  public void addContentTypeId(String contentTypeId) {
    if (!fMatrix.containsKey(contentTypeId))
      fMatrix.put(contentTypeId, new ArrayList());
  }

  public void addParitionType(String contentTypeId, String partitionType) {
    if (!fMatrix.containsKey(contentTypeId))
      fMatrix.put(contentTypeId, new ArrayList());

    List partitionList = (List) fMatrix.get(contentTypeId);
    partitionList.add(partitionType);
  }

  /**
   * @param contentType
   * @return whether this validator explicitly declared that it could handle this content type or
   *         any of its parent content types
   */
  public boolean canHandleContentType(String contentType) {
    // need to iterate hierarchy
    String[] contentHierarchy = calculateParentContentTypeIds(contentType);
    for (int i = 0; i < contentHierarchy.length; i++) {
      if (fMatrix.containsKey(contentHierarchy[i]))
        return true;
    }
    return false;
  }

  /**
   * @param contentType
   * @return whether this validator explicitly declared that it could handle this content type
   */
  public boolean mustHandleContentType(String contentType) {
    return fMatrix.containsKey(contentType);
  }

  /**
   * @param contentTypeIds
   * @param partitionType
   * @return whether this validator declared that it could handle this content type, or one of its
   *         parent content types, and partition type
   */
  public boolean canHandlePartitionType(String contentTypeIds[], String paritionType) {
    for (int i = 0; i < contentTypeIds.length; i++) {
      List partitions = (List) fMatrix.get(contentTypeIds[i]);
      if (partitions != null) {
        for (int j = 0; j < partitions.size(); j++) {
          if (paritionType.equals(partitions.get(j)))
            return true;
        }
      }
    }
    return false;
  }

  /**
   * @param element
   * @param classAttribute
   * @return Object
   * @throws CoreException
   */
  Object createExecutableExtension(final IConfigurationElement element, final String classAttribute)
      throws CoreException {
    Object obj = null;
    obj = element.createExecutableExtension(classAttribute);
    return obj;
  }

  /**
   * Creates an extension. If the extension plugin has not been loaded a busy cursor will be
   * activated during the duration of the load.
   * 
   * @param element
   * @param classAttribute
   * @return Object
   * @throws CoreException
   */
  public Object createExtension() {
    // If plugin has been loaded create extension.
    // Otherwise, show busy cursor then create extension.
    final IConfigurationElement element = getConfigurationElement();
    final Object[] result = new Object[1];
    String pluginId = element.getDeclaringExtension().getNamespace();
    Bundle bundle = Platform.getBundle(pluginId);
    if (bundle.getState() == Bundle.ACTIVE) {
      try {
        return createExecutableExtension(element, "class"); //$NON-NLS-1$
      } catch (CoreException e) {
        handleCreateExecutableException(result, e);
      }
    } else {
      BusyIndicator.showWhile(null, new Runnable() {
        public void run() {
          try {
            result[0] = createExecutableExtension(element, "class"); //$NON-NLS-1$
          } catch (Exception e) {
            handleCreateExecutableException(result, e);
          }
        }
      });
    }
    return result[0];
  }

  /**
   * @return a validator instance based on this ValidatorMetaData instance
   */
  public IValidator createValidator() {
    Object obj = null;
    obj = createExtension();
    if (obj == null) {
      return null;
    }
    return (obj instanceof IValidator) ? (IValidator) obj : null;
  }

  public IConfigurationElement getConfigurationElement() {
    return fConfigurationElement;
  }

  public String getValidatorClass() {
    return fClass;
  }

  public String getValidatorId() {
    return fId;
  }

  /**
   * @param result
   * @param e
   */
  void handleCreateExecutableException(Object[] result, Throwable e) {
    Logger.logException(e);
    e.printStackTrace();
    result[0] = null;
  }

  /**
   * ReconcileAnnotationKey.TOTAL or ReconcileAnnotationKey.PARTIAL
   * 
   * @return
   */
  public int getValidatorScope() {
    return fScope.equalsIgnoreCase("total") ? ReconcileAnnotationKey.TOTAL : ReconcileAnnotationKey.PARTIAL; //$NON-NLS-1$
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringBuffer debugString = new StringBuffer("ValidatorMetaData:"); //$NON-NLS-1$
    if (fId != null)
      debugString.append(" [id:" + fId + "]"); //$NON-NLS-1$ //$NON-NLS-2$
    return debugString.toString();
  }
}
