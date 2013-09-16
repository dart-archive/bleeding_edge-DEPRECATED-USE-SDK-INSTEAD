/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentassist;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.osgi.framework.Bundle;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Wraps an {@link ICompletionProposalComputer} provided by an extension to the
 * <code>org.eclipse.wst.sse.ui.completionProposal</code> extension point. Instances are immutable.
 * Instances can be obtained from a {@link CompletionProposalComputerRegistry}.
 * 
 * @see CompletionProposalComputerRegistry
 */
final class CompletionProposalComputerDescriptor {
  /** The default category id. */
  private static final String DEFAULT_CATEGORY_ID = "org.eclipse.wst.sse.ui.defaultProposalCategory"; //$NON-NLS-1$

  /** The extension schema name of the category id attribute. */
  private static final String ATTR_CATEGORY_ID = "categoryId"; //$NON-NLS-1$

  /** The extension schema name for element ID attributes */
  private static final String ATTR_ID = "id"; //$NON-NLS-1$

  /** The extension schema name for element name attributes */
  private static final String ATTR_NAME = "name"; //$NON-NLS-1$

  /** The extension schema name of the class attribute. */
  private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  /** The extension schema name of the activate attribute. */
  private static final String ATTRACTIVATE = "activate"; //$NON-NLS-1$

  /** The extension schema name of the content type child elements. */
  private static final String ELEM_CONTENT_TYPE = "contentType"; //$NON-NLS-1$

  /** The extension schema name of the partition type child elements. */
  private static final String ELEM_PARTITION_TYPE = "partitionType"; //$NON-NLS-1$

  /** The name of the performance event used to trace extensions. */
  private static final String PERFORMANCE_EVENT = SSEUIPlugin.ID
      + "/perf/content_assist/extensions"; //$NON-NLS-1$

  /**
   * If <code>true</code>, execution time of extensions is measured and the data forwarded to core's
   * {@link PerformanceStats} service.
   */
  private static final boolean MEASURE_PERFORMANCE = PerformanceStats.isEnabled(PERFORMANCE_EVENT);

  /**
   * Independently of the {@link PerformanceStats} service, any operation that takes longer than *
   * {@value} milliseconds will be flagged as an violation. This timeout does not apply to the first
   * invocation, as it may take longer due to plug-in initialization etc. See also
   * {@link #fIsReportingDelay}.
   */
  private static final long MAX_DELAY = 500000;

  /* log constants */
  private static final String COMPUTE_COMPLETION_PROPOSALS = "computeCompletionProposals()"; //$NON-NLS-1$
  private static final String COMPUTE_CONTEXT_INFORMATION = "computeContextInformation()"; //$NON-NLS-1$
  private static final String SESSION_STARTED = "sessionStarted()"; //$NON-NLS-1$
  private static final String SESSION_ENDED = "sessionEnded()"; //$NON-NLS-1$

  /** The identifier of the extension. */
  private final String fId;

  /** The name of the extension. */
  private final String fName;

  /** The class name of the provided <code>ISSECompletionProposalComputer</code>. */
  private final String fClass;

  /** The activate attribute value. */
  private final boolean fActivate;

  /** The configuration element of this extension. */
  private final IConfigurationElement fElement;

  /** The computer, if instantiated, <code>null</code> otherwise. */
  private ICompletionProposalComputer fComputer;

  /** The UI category. */
  private final CompletionProposalCategory fCategory;

  /** The first error message in the most recent operation, or <code>null</code>. */
  private String fLastError;

  /**
   * Tells whether to inform the user when <code>MAX_DELAY</code> has been exceeded. We start timing
   * execution after the first session because the first may take longer due to plug-in activation
   * and initialization.
   */
  private boolean fIsReportingDelay = false;

  /** The start of the last operation. */
  private long fStart;

  /**
   * Tells whether we tried to load the computer.
   * 
   * @since 3.4
   */
  boolean fTriedLoadingComputer = false;

  /**
   * <p>
   * Creates a new descriptor.
   * </p>
   * <p>
   * <b>NOTE: </b> This will not add this new descriptor to the given
   * {@link CompletionProposalComputerRegistry}. That can not be done until this descriptor is done
   * being constructed. Therefore be sure to call {@link #addToRegistry()} after creating a new
   * descriptor.
   * </p>
   * 
   * @param element the configuration element to read
   * @param categories the categories
   * @throws InvalidRegistryObjectException if this extension is no longer valid
   * @throws CoreException if the extension contains invalid values
   */
  CompletionProposalComputerDescriptor(IConfigurationElement element, List categories)
      throws InvalidRegistryObjectException, CoreException {
    Assert.isLegal(element != null);
    fElement = element;

    //get & verify ID
    fId = fElement.getAttribute(ATTR_ID);
    ContentAssistUtils.checkExtensionAttributeNotNull(fId, ATTR_ID, fElement);

    //get & verify optional name
    String name = fElement.getAttribute(ATTR_NAME);
    if (name == null) {
      fName = fId;
    } else {
      fName = name;
    }

    //get & verify activate plugin attribute
    String activateAttribute = fElement.getAttribute(ATTRACTIVATE);
    fActivate = Boolean.valueOf(activateAttribute).booleanValue();

    //get & verify class
    fClass = fElement.getAttribute(ATTR_CLASS);
    ContentAssistUtils.checkExtensionAttributeNotNull(fClass, ATTR_CLASS, fElement);

    //get & verify optional category id
    String categoryId = fElement.getAttribute(ATTR_CATEGORY_ID);
    if (categoryId == null) {
      categoryId = DEFAULT_CATEGORY_ID;
    }

    //find the category with the determined category id
    CompletionProposalCategory category = null;
    for (Iterator it = categories.iterator(); it.hasNext();) {
      CompletionProposalCategory cat = (CompletionProposalCategory) it.next();
      if (cat.getId().equals(categoryId)) {
        category = cat;
        break;
      }
    }

    /*
     * create a category if it does not exist else just set the category
     */
    if (category == null) {
      fCategory = new CompletionProposalCategory(categoryId, fName);

      /*
       * will add the new category to the registers list of categories, by the magic of object
       * references
       */
      categories.add(fCategory);
    } else {
      fCategory = category;
    }
  }

  /**
   * <p>
   * Adds this descriptor to the {@link CompletionProposalComputerRegistry}.
   * </p>
   * <p>
   * <b>NOTE: </b>Must be done after descriptor creation or the descriptor will not be added to the
   * registry. Can not be done in constructor because descriptor must be constructed before it can
   * be added to the registry
   * </p>
   * 
   * @throws InvalidRegistryObjectException
   * @throws CoreException
   */
  void addToRegistry() throws InvalidRegistryObjectException, CoreException {
    parseActivationAndAddToRegistry(this.fElement, this);
  }

  /**
   * @return the category that the wrapped {@link ICompletionProposalComputer} is associated with.
   */
  CompletionProposalCategory getCategory() {
    return fCategory;
  }

  /**
   * @return the contributor of the described {@link ICompletionProposalComputer}
   */
  IContributor getContributor() {
    try {
      return fElement.getContributor();
    } catch (InvalidRegistryObjectException e) {
      return null;
    }
  }

  /**
   * @return Returns the id of the described {@link ICompletionProposalComputer}
   */
  public String getId() {
    return fId;
  }

  /**
   * @return the name of the described {@link ICompletionProposalComputer}
   */
  public String getName() {
    return fName;
  }

  /**
   * Returns a new instance of the computer as described in the extension's xml. Note that the
   * safest way to access the computer is by using the
   * {@linkplain #computeCompletionProposals(ContentAssistInvocationContext, IProgressMonitor)
   * computeCompletionProposals} and
   * {@linkplain #computeContextInformation(ContentAssistInvocationContext, IProgressMonitor)
   * computeContextInformation} methods. These delegate the functionality to the contributed
   * computer, but handle instance creation and any exceptions thrown.
   * 
   * @return a new instance of the completion proposal computer as described by this descriptor
   * @throws CoreException if the creation fails
   * @throws InvalidRegistryObjectException if the extension is not valid any longer (e.g. due to
   *           plug-in unloading)
   */
  public ICompletionProposalComputer createComputer() throws CoreException,
      InvalidRegistryObjectException {
    return (ICompletionProposalComputer) fElement.createExecutableExtension(ATTR_CLASS);
  }

  /**
   * <p>
   * Safely computes completion proposals through the described extension.
   * </p>
   * 
   * @param context the invocation context passed on to the extension
   * @param monitor the progress monitor passed on to the extension
   * @return the list of computed completion proposals (element type:
   *         {@link org.eclipse.jface.text.contentassist.ICompletionProposal})
   */
  public List computeCompletionProposals(CompletionProposalInvocationContext context,
      IProgressMonitor monitor) {
    List completionProposals = Collections.EMPTY_LIST;
    if (isEnabled()) {
      IStatus status = null;
      try {
        // plugin must be active to get computer
        ICompletionProposalComputer computer = getComputer(true);
        if (computer != null) {
          try {
            PerformanceStats stats = startMeter(context, computer);
            //ask the computer for the proposals
            List proposals = computer.computeCompletionProposals(context, monitor);
            stopMeter(stats, COMPUTE_COMPLETION_PROPOSALS);

            if (proposals != null) {
              fLastError = computer.getErrorMessage();
              completionProposals = proposals;
            } else {
              status = createAPIViolationStatus(COMPUTE_COMPLETION_PROPOSALS);
            }
          } finally {
            fIsReportingDelay = true;
          }
        }
      } catch (InvalidRegistryObjectException x) {
        status = createExceptionStatus(x);
      } catch (CoreException x) {
        status = createExceptionStatus(x);
      } catch (RuntimeException x) {
        status = createExceptionStatus(x);
      } finally {
        monitor.done();
      }

      if (status != null) {
        Logger.log(status);
      }
    }

    return completionProposals;
  }

  /**
   * <p>
   * Safely computes context information objects through the described extension.
   * </p>
   * 
   * @param context the invocation context passed on to the extension
   * @param monitor the progress monitor passed on to the extension
   * @return the list of computed context information objects (element type:
   *         {@link org.eclipse.jface.text.contentassist.IContextInformation})
   */
  public List computeContextInformation(CompletionProposalInvocationContext context,
      IProgressMonitor monitor) {
    List contextInformation = Collections.EMPTY_LIST;
    if (isEnabled()) {
      IStatus status = null;
      try {
        // plugin must be active to get computer
        ICompletionProposalComputer computer = getComputer(true);
        if (computer != null) {
          PerformanceStats stats = startMeter(context, computer);
          List proposals = computer.computeContextInformation(context, monitor);
          stopMeter(stats, COMPUTE_CONTEXT_INFORMATION);

          if (proposals != null) {
            fLastError = computer.getErrorMessage();
            contextInformation = proposals;
          } else {
            status = createAPIViolationStatus(COMPUTE_CONTEXT_INFORMATION);
          }
        }
      } catch (InvalidRegistryObjectException x) {
        status = createExceptionStatus(x);
      } catch (CoreException x) {
        status = createExceptionStatus(x);
      } catch (RuntimeException x) {
        status = createExceptionStatus(x);
      } finally {
        monitor.done();
      }

      if (status != null) {
        Logger.log(status);
      }
    }

    return contextInformation;
  }

  /**
   * <p>
   * Notifies the described extension of a proposal computation session start.
   * </p>
   * <p>
   * <b>Note: </b>This method is called every time code assist is invoked and is
   * <strong>not</strong> filtered by content type or partition type.
   * </p>
   */
  public void sessionStarted() {
    if (isEnabled()) {
      IStatus status = null;
      try {
        // plugin must be active to get computer
        ICompletionProposalComputer computer = getComputer(true);
        if (computer != null) {
          PerformanceStats stats = startMeter(SESSION_STARTED, computer);
          computer.sessionStarted();
          stopMeter(stats, SESSION_ENDED);
        }
      } catch (InvalidRegistryObjectException x) {
        status = createExceptionStatus(x);
      } catch (CoreException x) {
        status = createExceptionStatus(x);
      } catch (RuntimeException x) {
        status = createExceptionStatus(x);
      }

      if (status != null) {
        Logger.log(status);
      }
    }
  }

  /**
   * <p>
   * Notifies the described extension of a proposal computation session end.
   * </p>
   * <p>
   * <b>Note: </b>This method is called every time code assist is invoked and is
   * <strong>not</strong> filtered by content type or partition type.
   * </p>
   */
  public void sessionEnded() {
    if (isEnabled()) {
      IStatus status = null;
      try {
        // plugin must be active to get computer
        ICompletionProposalComputer computer = getComputer(true);
        if (computer != null) {
          PerformanceStats stats = startMeter(SESSION_ENDED, computer);
          computer.sessionEnded();
          stopMeter(stats, SESSION_ENDED);
        }
      } catch (InvalidRegistryObjectException x) {
        status = createExceptionStatus(x);
      } catch (CoreException x) {
        status = createExceptionStatus(x);
      } catch (RuntimeException x) {
        status = createExceptionStatus(x);
      }

      if (status != null) {
        Logger.log(status);
      }
    }
  }

  /**
   * @return the error message from the described {@link ICompletionProposalComputer}
   */
  public String getErrorMessage() {
    return fLastError;
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return fId + ": " + fName; //$NON-NLS-1$
  }

  /**
   * <p>
   * Parses the given configuration element for its activation context, that is to say the content
   * types and partiton types and updates the registry to associated the given computer descriptor
   * with the parsed activation contexts.
   * </P>
   * <p>
   * This is useful for parsing both <tt>proposalComputer</tt> elements and
   * <tt>proposalComputerExtendedActivation</tt> elements.
   * </p>
   * 
   * @param element {@link IConfigurationElement} containing the activation context
   * @param desc {@link CompletionProposalComputerDescriptor} to associate with the parsed
   *          activation context
   * @throws InvalidRegistryObjectException
   * @throws CoreException
   */
  protected static void parseActivationAndAddToRegistry(IConfigurationElement element,
      CompletionProposalComputerDescriptor desc) throws InvalidRegistryObjectException,
      CoreException {

    /*
     * if this descriptor is specific to a content type/s add it to the registry as such else add to
     * registry for all content types
     */
    IConfigurationElement[] contentTypes = element.getChildren(ELEM_CONTENT_TYPE);
    if (contentTypes.length > 0) {
      for (int contentTypeIndex = 0; contentTypeIndex < contentTypes.length; ++contentTypeIndex) {
        String contentTypeID = contentTypes[contentTypeIndex].getAttribute(ATTR_ID);
        ContentAssistUtils.checkExtensionAttributeNotNull(contentTypeID, ATTR_ID,
            contentTypes[contentTypeIndex]);

        /*
         * if this descriptor is for specific partition types in the content type add it to the
         * registry as such else add to the registry for all partition types in the content type
         */
        IConfigurationElement[] partitionTypes = contentTypes[contentTypeIndex].getChildren(ELEM_PARTITION_TYPE);
        if (partitionTypes.length > 0) {
          for (int partitionTypeIndex = 0; partitionTypeIndex < partitionTypes.length; ++partitionTypeIndex) {
            String partitionTypeID = partitionTypes[partitionTypeIndex].getAttribute(ATTR_ID);
            ContentAssistUtils.checkExtensionAttributeNotNull(partitionTypeID, ATTR_ID,
                partitionTypes[partitionTypeIndex]);

            CompletionProposalComputerRegistry.getDefault().putDescription(contentTypeID,
                partitionTypeID, desc);
            CompletionProposalComputerRegistry.getDefault().putAutoActivator(contentTypeID,
                partitionTypeID, partitionTypes[partitionTypeIndex]);
          }
        } else {
          CompletionProposalComputerRegistry.getDefault().putDescription(contentTypeID, null, desc);
        }
      }
    } else {
      Logger.log(Logger.WARNING,
          "The configuration element: " + element + " does not contain any content types."); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  /**
   * @return <code>true</code> if the plugin that contains the {@link IConfigurationElement}
   *         associated with this descriptor is loaded, <code>false</code> otherwise.
   */
  private boolean isPluginLoaded() {
    Bundle bundle = getBundle();
    return bundle != null && bundle.getState() == Bundle.ACTIVE;
  }

  /**
   * @return the {@link Bundle} that contains the {@link IConfigurationElement} associated with this
   *         descriptor
   */
  private Bundle getBundle() {
    String namespace = fElement.getDeclaringExtension().getContributor().getName();
    Bundle bundle = Platform.getBundle(namespace);
    return bundle;
  }

  /**
   * <p>
   * Returns a cached instance of the computer as described in the extension's xml. If the computer
   * is not yet created and <code>canCreate</code> is <code>true</code> then
   * {@link #createComputer()} is called and the result cached.
   * </p>
   * 
   * @param canCreate <code>true</code> if the proposal computer can be created
   * @return a new instance of the completion proposal computer as described by this descriptor
   * @throws CoreException if the creation fails
   * @throws InvalidRegistryObjectException if the extension is not valid any longer (e.g. due to
   *           plug-in unloading)
   */
  private synchronized ICompletionProposalComputer getComputer(boolean canCreate)
      throws CoreException, InvalidRegistryObjectException {
    if (fComputer == null && canCreate && !fTriedLoadingComputer && (fActivate || isPluginLoaded())) {
      fTriedLoadingComputer = true;
      fComputer = createComputer();
    }
    return fComputer;
  }

  /**
   * @return the enablement state of the category this describer is associated with
   */
  private boolean isEnabled() {
    return fCategory.isEnabled();
  }

  /**
   * <p>
   * Starts the meter for measuring the computers performance
   * </p>
   * 
   * @param context
   * @param computer
   * @return
   */
  private PerformanceStats startMeter(Object context, ICompletionProposalComputer computer) {
    final PerformanceStats stats;
    if (MEASURE_PERFORMANCE) {
      stats = PerformanceStats.getStats(PERFORMANCE_EVENT, computer);
      stats.startRun(context.toString());
    } else {
      stats = null;
    }

    if (fIsReportingDelay) {
      fStart = System.currentTimeMillis();
    }

    return stats;
  }

  /**
   * <p>
   * Stops the meter for measuring the computers performance
   * </p>
   * 
   * @param context
   * @param computer
   * @return
   */
  private void stopMeter(final PerformanceStats stats, String operation) {
    if (MEASURE_PERFORMANCE) {
      stats.endRun();
      if (stats.isFailure()) {
        IStatus status = createPerformanceStatus(operation);
        Logger.log(status);
        return;
      }
    }

    if (fIsReportingDelay) {
      long current = System.currentTimeMillis();
      if (current - fStart > MAX_DELAY) {
        IStatus status = createPerformanceStatus(operation);
        Logger.log(status);
      }
    }
  }

  /**
   * @return A message explaining that the described {@link ICompletionProposalComputer} failed in
   *         some way
   */
  private String createBlameMessage() {
    return "The ''" + getName() + "'' proposal computer from the ''" + //$NON-NLS-1$ //$NON-NLS-2$
        fElement.getDeclaringExtension().getContributor().getName()
        + "'' plug-in did not complete normally."; //$NON-NLS-1$
  }

  /**
   * <p>
   * Create a status message describing that the extension has become invalid
   * </p>
   * 
   * @param x the associated {@link InvalidRegistryObjectException}
   * @return the created {@link IStatus}
   */
  private IStatus createExceptionStatus(InvalidRegistryObjectException x) {
    String blame = createBlameMessage();
    String reason = "The extension has become invalid."; //$NON-NLS-1$
    return new Status(IStatus.INFO, SSEUIPlugin.ID, IStatus.OK, blame + " " + reason, x); //$NON-NLS-1$
  }

  /**
   * <p>
   * create a status message explaining that the extension could not be instantiated
   * </p>
   * 
   * @param x the associated {@link CoreException}
   * @return the created {@link IStatus}
   */
  private IStatus createExceptionStatus(CoreException x) {
    String blame = createBlameMessage();
    String reason = "Unable to instantiate the extension."; //$NON-NLS-1$
    return new Status(IStatus.ERROR, SSEUIPlugin.ID, IStatus.OK, blame + " " + reason, x); //$NON-NLS-1$
  }

  /**
   * <p>
   * Create a status message explaining the extension has thrown a runtime exception
   * </p>
   * 
   * @param x the associated {@link RuntimeException}
   * @return the created {@link IStatus}
   */
  private IStatus createExceptionStatus(RuntimeException x) {
    String blame = createBlameMessage();
    String reason = "The extension has thrown a runtime exception."; //$NON-NLS-1$
    return new Status(IStatus.WARNING, SSEUIPlugin.ID, IStatus.OK, blame + " " + reason, x); //$NON-NLS-1$
  }

  /**
   * <p>
   * Create a status message explaining the extension has violated the API of the extension point
   * </p>
   * 
   * @param operation the operation that created the API violation
   * @return the created {@link IStatus}
   */
  private IStatus createAPIViolationStatus(String operation) {
    String blame = createBlameMessage();
    String reason = "The extension violated the API contract of the ''" + operation + "'' operation."; //$NON-NLS-1$ //$NON-NLS-2$
    return new Status(IStatus.WARNING, SSEUIPlugin.ID, IStatus.OK, blame + " " + reason, null); //$NON-NLS-1$
  }

  /**
   * <p>
   * Create a status message explaining that the extension took to long during an operation
   * </p>
   * 
   * @param operation the operation that took to long
   * @return the created {@link IStatus}
   */
  private IStatus createPerformanceStatus(String operation) {
    String blame = createBlameMessage();
    String reason = "The extension took too long to return from the ''" + operation + "'' operation."; //$NON-NLS-1$ //$NON-NLS-2$
    return new Status(IStatus.WARNING, SSEUIPlugin.ID, IStatus.OK, blame + " " + reason, null); //$NON-NLS-1$
  }
}
