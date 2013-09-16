/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentassist;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.sse.core.internal.util.Assert;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationReader;
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Describes a category extension to the <code>org.eclipse.wst.sse.ui.completionProposal</code>
 * extension point.
 */
public final class CompletionProposalCategory {
  /** The extension schema name of the icon attribute. */
  private static final String ICON = "icon"; //$NON-NLS-1$

  /** The extension schema name of the ID attribute. */
  private static final String ID = "id"; //$NON-NLS-1$

  /** The extension schema name of the name attribute. */
  private static final String NAME = "name"; //$NON-NLS-1$

  /** ID of this completion category */
  private final String fId;

  /** Name of this completion category */
  private final String fName;

  /** The image descriptor for this category, or <code>null</code> if none specified. */
  private final ImageDescriptor fImage;

  /** The last error reported by this category */
  private String fLastError = null;

  /**
   * <p>
   * Construct the category by parsing the given element.
   * </p>
   * 
   * @param element {@link IConfigurationElement} containing the configuration of this category
   * @throws CoreException if the given {@link IConfigurationElement} does not contain the correct
   *           elements and attributes.
   */
  CompletionProposalCategory(IConfigurationElement element) throws CoreException {
    Assert.isLegal(element != null);

    //get & verify ID
    fId = element.getAttribute(ID);
    ContentAssistUtils.checkExtensionAttributeNotNull(fId, ID, element);

    //get & verify optional name
    String name = element.getAttribute(NAME);
    if (name == null) {
      fName = fId;
    } else {
      fName = name;
    }

    //get & verify optional icon
    String icon = element.getAttribute(ICON);
    ImageDescriptor img = null;
    if (icon != null) {
      Bundle bundle = ContentAssistUtils.getBundle(element);
      if (bundle != null) {
        img = AbstractUIPlugin.imageDescriptorFromPlugin(bundle.getSymbolicName(), icon);
      }
    }
    fImage = img;
  }

  /**
   * <p>
   * Creates a category with the given name and ID
   * </p>
   * 
   * @param id the unique ID of the new category
   * @param name the name of the new category
   */
  CompletionProposalCategory(String id, String name) {
    fId = id;
    fName = name;
    fImage = null;
  }

  /**
   * <p>
   * Returns the unique identifier of the category
   * </p>
   * 
   * @return Returns the id
   */
  public String getId() {
    return fId;
  }

  /**
   * <p>
   * Returns the human readable name of the category. It may contain mnemonics.
   * </p>
   * 
   * @return Returns the name
   */
  public String getName() {
    return fName;
  }

  /**
   * <p>
   * Returns the human readable name of the category without mnemonic hint in order to be displayed
   * in a message.
   * </p>
   * 
   * @return Returns the name
   */
  public String getDisplayName() {
    return LegacyActionTools.removeMnemonics(fName);
  }

  /**
   * <p>
   * Returns the image descriptor of the category.
   * </p>
   * 
   * @return the image descriptor of the category
   */
  public ImageDescriptor getImageDescriptor() {
    return fImage;
  }

  /**
   * @return <code>true</code> if this category should be displayed on its own content assist page,
   *         <code>false</code> otherwise
   */
  public boolean isDisplayedOnOwnPage(String contentTypeID) {
    boolean displayOnOwnPage = ICompletionProposalCategoriesConfigurationReader.DEFAULT_DISPLAY_ON_OWN_PAGE;

    ICompletionProposalCategoriesConfigurationReader properties = CompletionProposoalCatigoriesConfigurationRegistry.getDefault().getReadableConfiguration(
        contentTypeID);
    if (properties != null) {
      displayOnOwnPage = properties.shouldDisplayOnOwnPage(this.fId);
    }

    return displayOnOwnPage;
  }

  /**
   * @return <code>true</code> if this category should be displayed in the default content assist
   *         page, <code>false</code> otherwise
   */
  public boolean isIncludedOnDefaultPage(String contentTypeID) {
    boolean includeOnDefaultPage = ICompletionProposalCategoriesConfigurationReader.DEFAULT_INCLUDE_ON_DEFAULTS_PAGE;

    ICompletionProposalCategoriesConfigurationReader properties = CompletionProposoalCatigoriesConfigurationRegistry.getDefault().getReadableConfiguration(
        contentTypeID);
    if (properties != null) {
      includeOnDefaultPage = properties.shouldDisplayOnDefaultPage(this.fId);
    }

    return includeOnDefaultPage;
  }

  /**
   * <p>
   * Given a content type ID determines the rank of this category for sorting the content assist
   * pages
   * </p>
   * 
   * @return the sort rank of this category
   */
  public int getPageSortRank(String contentTypeID) {
    int sortOrder = ICompletionProposalCategoriesConfigurationReader.DEFAULT_SORT_ORDER;

    ICompletionProposalCategoriesConfigurationReader properties = CompletionProposoalCatigoriesConfigurationRegistry.getDefault().getReadableConfiguration(
        contentTypeID);
    if (properties != null) {
      sortOrder = properties.getPageSortOrder(this.fId);
    }

    return sortOrder;
  }

  /**
   * <p>
   * Given a content type ID determines the rank of this category for sorting on the default content
   * assist page with other categories
   * </p>
   * 
   * @return the sort rank of this category
   */
  public int getDefaultPageSortRank(String contentTypeID) {
    int sortOrder = ICompletionProposalCategoriesConfigurationReader.DEFAULT_SORT_ORDER;

    ICompletionProposalCategoriesConfigurationReader properties = CompletionProposoalCatigoriesConfigurationRegistry.getDefault().getReadableConfiguration(
        contentTypeID);
    if (properties != null) {
      sortOrder = properties.getDefaultPageSortOrder(this.fId);
    }

    return sortOrder;
  }

  /**
   * <p>
   * <b>NOTE: </b> enablement is not the same as weather a category should be displayed on its own
   * page or the default page, it describes if the category should be used at all. Currently
   * categories are always enabled. There maybe cases in the future where a category should be
   * disabled entirely though.
   * </p>
   * 
   * @return <code>true</code> if this category is enabled, <code>false</code> otherwise
   */
  public boolean isEnabled() {
    return true;
  }

  /**
   * @return <code>true</code> if the category contains any computers, <code>false</code> otherwise
   */
  public boolean hasComputers() {
    List descriptors = CompletionProposalComputerRegistry.getDefault().getProposalComputerDescriptors();
    for (Iterator it = descriptors.iterator(); it.hasNext();) {
      CompletionProposalComputerDescriptor desc = (CompletionProposalComputerDescriptor) it.next();
      if (desc.getCategory() == this) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param contentTypeID the content type ID
   * @param partitionTypeID the partition
   * @return <code>true</code> if the category contains any computers, in the given partition type
   *         in the given content type, <code>false</code> otherwise
   */
  public boolean hasComputers(String contentTypeID, String partitionTypeID) {
    List descriptors = CompletionProposalComputerRegistry.getDefault().getProposalComputerDescriptors(
        contentTypeID, partitionTypeID);
    for (Iterator it = descriptors.iterator(); it.hasNext();) {
      CompletionProposalComputerDescriptor desc = (CompletionProposalComputerDescriptor) it.next();
      if (desc.getCategory() == this) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param contentTypeID the content type ID
   * @return <code>true</code> if the category contains any computers, in the given partition type
   *         in the given content type, <code>false</code> otherwise
   */
  public boolean hasComputers(String contentTypeID) {
    List descriptors = CompletionProposalComputerRegistry.getDefault().getProposalComputerDescriptors(
        contentTypeID);
    for (Iterator it = descriptors.iterator(); it.hasNext();) {
      CompletionProposalComputerDescriptor desc = (CompletionProposalComputerDescriptor) it.next();
      if (desc.getCategory() == this) {
        return true;
      }
    }
    return false;
  }

  /**
   * <p>
   * Safely computes completion proposals of all computers of this category through their extension.
   * </p>
   * 
   * @param context the invocation context passed on to the extension
   * @param contentTypeID the content type ID where the invocation occurred
   * @param partitionTypeID the partition type where the invocation occurred
   * @param monitor the progress monitor passed on to the extension
   * @return the list of computed completion proposals (element type:
   *         {@link org.eclipse.jface.text.contentassist.ICompletionProposal})
   */
  public List computeCompletionProposals(CompletionProposalInvocationContext context,
      String contentTypeID, String partitionTypeID, SubProgressMonitor monitor) {

    fLastError = null;
    List result = new ArrayList();
    List descriptors = new ArrayList(
        CompletionProposalComputerRegistry.getDefault().getProposalComputerDescriptors(
            contentTypeID, partitionTypeID));
    for (Iterator it = descriptors.iterator(); it.hasNext();) {
      CompletionProposalComputerDescriptor desc = (CompletionProposalComputerDescriptor) it.next();
      if (desc.getCategory() == this) {
        result.addAll(desc.computeCompletionProposals(context, monitor));
      }

      if (fLastError == null && desc.getErrorMessage() != null) {
        fLastError = desc.getErrorMessage();
      }
    }
    return result;
  }

  /**
   * <p>
   * Safely computes context information objects of all computers of this category through their
   * extension.
   * </p>
   * 
   * @param context the invocation context passed on to the extension
   * @param contentTypeID the content type ID where the invocation occurred
   * @param partitionTypeID the partition type where the invocation occurred
   * @param monitor the progress monitor passed on to the extension
   * @return the list of computed context information objects (element type:
   *         {@link org.eclipse.jface.text.contentassist.IContextInformation})
   */
  public List computeContextInformation(CompletionProposalInvocationContext context,
      String contentTypeID, String partitionTypeID, SubProgressMonitor monitor) {

    fLastError = null;
    List result = new ArrayList();
    List descriptors = new ArrayList(
        CompletionProposalComputerRegistry.getDefault().getProposalComputerDescriptors(
            contentTypeID, partitionTypeID));
    for (Iterator it = descriptors.iterator(); it.hasNext();) {
      CompletionProposalComputerDescriptor desc = (CompletionProposalComputerDescriptor) it.next();
      if (desc.getCategory() == this
          && (isIncludedOnDefaultPage(contentTypeID) || isDisplayedOnOwnPage(contentTypeID))) {
        result.addAll(desc.computeContextInformation(context, monitor));
      }
      if (fLastError == null) {
        fLastError = desc.getErrorMessage();
      }
    }
    return result;
  }

  /**
   * @return the last error message reported by a computer in this category
   */
  public String getErrorMessage() {
    return fLastError;
  }

  /**
   * <p>
   * Notifies the computers in this category of a proposal computation session start.
   * </p>
   */
  public void sessionStarted() {
    List descriptors = new ArrayList(
        CompletionProposalComputerRegistry.getDefault().getProposalComputerDescriptors());
    for (Iterator it = descriptors.iterator(); it.hasNext();) {
      CompletionProposalComputerDescriptor desc = (CompletionProposalComputerDescriptor) it.next();
      if (desc.getCategory() == this)
        desc.sessionStarted();
      if (fLastError == null)
        fLastError = desc.getErrorMessage();
    }
  }

  /**
   * <p>
   * Notifies the computers in this category of a proposal computation session end.
   * </p>
   */
  public void sessionEnded() {
    List descriptors = new ArrayList(
        CompletionProposalComputerRegistry.getDefault().getProposalComputerDescriptors());
    for (Iterator it = descriptors.iterator(); it.hasNext();) {
      CompletionProposalComputerDescriptor desc = (CompletionProposalComputerDescriptor) it.next();
      if (desc.getCategory() == this)
        desc.sessionEnded();
      if (fLastError == null)
        fLastError = desc.getErrorMessage();
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return fId + ": " + fName; //$NON-NLS-1$
  }
}
