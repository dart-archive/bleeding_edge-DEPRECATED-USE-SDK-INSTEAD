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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.dart.ContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposalComputer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.osgi.framework.Bundle;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The description of an extension to the
 * <code>com.google.dart.tools.ui.javaCompletionProposalComputer</code> extension point. Instances
 * are immutable. Instances can be obtained from a {@link CompletionProposalComputerRegistry}.
 * 
 * @see CompletionProposalComputerRegistry
 */
final class CompletionProposalComputerDescriptor {
  /** The default category id. */
  private static final String DEFAULT_CATEGORY_ID = "com.google.dart.tools.ui.defaultProposalCategory"; //$NON-NLS-1$
  /** The extension schema name of the category id attribute. */
  private static final String CATEGORY_ID = "categoryId"; //$NON-NLS-1$
  /** The extension schema name of the partition type attribute. */
  private static final String TYPE = "type"; //$NON-NLS-1$
  /** The extension schema name of the class attribute. */
  private static final String CLASS = "class"; //$NON-NLS-1$
  /** The extension schema name of the activate attribute. */
  private static final String ACTIVATE = "activate"; //$NON-NLS-1$
  /** The extension schema name of the partition child elements. */
  private static final String PARTITION = "partition"; //$NON-NLS-1$
  /** Set of Java partition types. */
  private static final Set<String> PARTITION_SET;
  /** The name of the performance event used to trace extensions. */
  private static final String PERFORMANCE_EVENT = DartToolsPlugin.getPluginId()
      + "/perf/content_assist/extensions"; //$NON-NLS-1$
  /**
   * If <code>true</code>, execution time of extensions is measured and the data forwarded to core's
   * {@link PerformanceStats} service.
   */
  private static final boolean MEASURE_PERFORMANCE = PerformanceStats.isEnabled(PERFORMANCE_EVENT);
  /**
   * Independently of the {@link PerformanceStats} service, any operation that takes longer than * *
   * * {@value} milliseconds will be flagged as an violation. This timeout does not apply to the
   * first invocation, as it may take longer due to plug-in initialization etc. See also
   * {@link #fIsReportingDelay}.
   */
  private static final long MAX_DELAY = 5000;

  /* log constants */
  private static final String COMPUTE_COMPLETION_PROPOSALS = "computeCompletionProposals()"; //$NON-NLS-1$
  private static final String COMPUTE_CONTEXT_INFORMATION = "computeContextInformation()"; //$NON-NLS-1$
  private static final String SESSION_STARTED = "sessionStarted()"; //$NON-NLS-1$
  private static final String SESSION_ENDED = "sessionEnded()"; //$NON-NLS-1$

  static {
    Set<String> partitions = new HashSet<String>();
    partitions.add(IDocument.DEFAULT_CONTENT_TYPE);
    partitions.add(DartPartitions.DART_DOC);
    partitions.add(DartPartitions.DART_MULTI_LINE_COMMENT);
    partitions.add(DartPartitions.DART_SINGLE_LINE_COMMENT);
    partitions.add(DartPartitions.DART_SINGLE_LINE_DOC);
    partitions.add(DartPartitions.DART_STRING);
    partitions.add(DartPartitions.DART_MULTI_LINE_STRING);

    PARTITION_SET = Collections.unmodifiableSet(partitions);
  }

  /** The identifier of the extension. */
  private final String fId;
  /** The name of the extension. */
  private final String fName;
  /**
   * The class name of the provided <code>IDartCompletionProposalComputer</code> .
   */
  private final String fClass;
  /** The activate attribute value. */
  private final boolean fActivate;
  /** The partition of the extension (element type: {@link String}). */
  private final Set<String> fPartitions;
  /** The configuration element of this extension. */
  private final IConfigurationElement fElement;
  /** The registry we are registered with. */
  private final CompletionProposalComputerRegistry fRegistry;
  /** The computer, if instantiated, <code>null</code> otherwise. */
  private IDartCompletionProposalComputer fComputer;
  /** The ui category. */
  private final CompletionProposalCategory fCategory;
  /**
   * The first error message in the most recent operation, or <code>null</code>.
   */
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
   * Creates a new descriptor.
   * 
   * @param element the configuration element to read
   * @param registry the computer registry creating this descriptor
   */
  CompletionProposalComputerDescriptor(IConfigurationElement element,
      CompletionProposalComputerRegistry registry, List<CompletionProposalCategory> categories)
      throws InvalidRegistryObjectException {
    Assert.isLegal(registry != null);
    Assert.isLegal(element != null);

    fRegistry = registry;
    fElement = element;
    IExtension extension = element.getDeclaringExtension();
    fId = extension.getUniqueIdentifier();
    checkNotNull(fId, "id"); //$NON-NLS-1$

    String name = extension.getLabel();
    if (name.length() == 0) {
      fName = fId;
    } else {
      fName = name;
    }

    Set<String> partitions = new HashSet<String>();
    IConfigurationElement[] children = element.getChildren(PARTITION);
    if (children.length == 0) {
      fPartitions = PARTITION_SET; // add to all partition types if no partition
// is configured
    } else {
      for (int i = 0; i < children.length; i++) {
        String type = children[i].getAttribute(TYPE);
        checkNotNull(type, TYPE);
        partitions.add(type);
      }
      fPartitions = Collections.unmodifiableSet(partitions);
    }

    String activateAttribute = element.getAttribute(ACTIVATE);
    fActivate = Boolean.valueOf(activateAttribute).booleanValue();

    fClass = element.getAttribute(CLASS);
    checkNotNull(fClass, CLASS);

    String categoryId = element.getAttribute(CATEGORY_ID);
    if (categoryId == null) {
      categoryId = DEFAULT_CATEGORY_ID;
    }
    CompletionProposalCategory category = null;
    for (Iterator<CompletionProposalCategory> it = categories.iterator(); it.hasNext();) {
      CompletionProposalCategory cat = it.next();
      if (cat.getId().equals(categoryId)) {
        category = cat;
        break;
      }
    }
    if (category == null) {
      // create a category if it does not exist
      fCategory = new CompletionProposalCategory(categoryId, fName, registry);
      categories.add(fCategory);
    } else {
      fCategory = category;
    }
  }

  /**
   * Safely computes completion proposals through the described extension. If the extension is
   * disabled, throws an exception or otherwise does not adhere to the contract described in
   * {@link IDartCompletionProposalComputer}, an empty list is returned.
   * 
   * @param context the invocation context passed on to the extension
   * @param monitor the progress monitor passed on to the extension
   * @return the list of computed completion proposals (element type:
   *         {@link org.eclipse.jface.text.contentassist.ICompletionProposal})
   */
  public List<ICompletionProposal> computeCompletionProposals(
      ContentAssistInvocationContext context, IProgressMonitor monitor) {
    if (!isEnabled()) {
      return Collections.emptyList();
    }

    IStatus status;
    try {
      IDartCompletionProposalComputer computer = getComputer();
      if (computer == null) {
        return Collections.emptyList();
      }

      try {
        PerformanceStats stats = startMeter(context, computer);
        List<ICompletionProposal> proposals = computer.computeCompletionProposals(context, monitor);
        stopMeter(stats, COMPUTE_COMPLETION_PROPOSALS);

        if (proposals != null) {
          fLastError = computer.getErrorMessage();
          return proposals;
        }
      } finally {
        fIsReportingDelay = true;
      }
      status = createAPIViolationStatus(COMPUTE_COMPLETION_PROPOSALS);
    } catch (InvalidRegistryObjectException x) {
      status = createExceptionStatus(x);
    } catch (CoreException x) {
      status = createExceptionStatus(x);
    } catch (RuntimeException x) {
      status = createExceptionStatus(x);
    } finally {
      monitor.done();
    }

    fRegistry.informUser(this, status);

    return Collections.emptyList();
  }

  /**
   * Safely computes context information objects through the described extension. If the extension
   * is disabled, throws an exception or otherwise does not adhere to the contract described in
   * {@link IDartCompletionProposalComputer}, an empty list is returned.
   * 
   * @param context the invocation context passed on to the extension
   * @param monitor the progress monitor passed on to the extension
   * @return the list of computed context information objects (element type:
   *         {@link org.eclipse.jface.text.contentassist.IContextInformation})
   */
  public List<IContextInformation> computeContextInformation(
      ContentAssistInvocationContext context, IProgressMonitor monitor) {
    if (!isEnabled()) {
      return Collections.emptyList();
    }

    IStatus status;
    try {
      IDartCompletionProposalComputer computer = getComputer();
      if (computer == null) {
        return Collections.emptyList();
      }

      PerformanceStats stats = startMeter(context, computer);
      List<IContextInformation> proposals = computer.computeContextInformation(context, monitor);
      stopMeter(stats, COMPUTE_CONTEXT_INFORMATION);

      if (proposals != null) {
        fLastError = computer.getErrorMessage();
        return proposals;
      }

      status = createAPIViolationStatus(COMPUTE_CONTEXT_INFORMATION);
    } catch (InvalidRegistryObjectException x) {
      status = createExceptionStatus(x);
    } catch (CoreException x) {
      status = createExceptionStatus(x);
    } catch (RuntimeException x) {
      status = createExceptionStatus(x);
    } finally {
      monitor.done();
    }

    fRegistry.informUser(this, status);

    return Collections.emptyList();
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
  public IDartCompletionProposalComputer createComputer() throws CoreException,
      InvalidRegistryObjectException {
    return (IDartCompletionProposalComputer) fElement.createExecutableExtension(CLASS);
  }

  /**
   * Returns the error message from the described extension.
   * 
   * @return the error message from the described extension
   */
  public String getErrorMessage() {
    return fLastError;
  }

  /**
   * Returns the identifier of the described extension.
   * 
   * @return Returns the id
   */
  public String getId() {
    return fId;
  }

  /**
   * Returns the name of the described extension.
   * 
   * @return Returns the name
   */
  public String getName() {
    return fName;
  }

  /**
   * Returns the partition types of the described extension.
   * 
   * @return the set of partition types (element type: {@link String})
   */
  public Set<String> getPartitions() {
    return fPartitions;
  }

  /**
   * Notifies the described extension of a proposal computation session end.
   * <p>
   * <em>
   * Note: This method is called every time code assist is invoked and
   * is <strong>not</strong> filtered by partition type.
   * </em>
   * </p>
   */
  public void sessionEnded() {
    if (!isEnabled()) {
      return;
    }

    IStatus status;
    try {
      IDartCompletionProposalComputer computer = getComputer();
      if (computer == null) {
        return;
      }

      PerformanceStats stats = startMeter(SESSION_ENDED, computer);
      computer.sessionEnded();
      stopMeter(stats, SESSION_ENDED);

      return;
    } catch (InvalidRegistryObjectException x) {
      status = createExceptionStatus(x);
    } catch (CoreException x) {
      status = createExceptionStatus(x);
    } catch (RuntimeException x) {
      status = createExceptionStatus(x);
    }

    fRegistry.informUser(this, status);
  }

  /**
   * Notifies the described extension of a proposal computation session start.
   * <p>
   * <em>
   * Note: This method is called every time code assist is invoked and
   * is <strong>not</strong> filtered by partition type.
   * </em>
   * </p>
   */
  public void sessionStarted() {
    if (!isEnabled()) {
      return;
    }

    IStatus status;
    try {
      IDartCompletionProposalComputer computer = getComputer();
      if (computer == null) {
        return;
      }

      PerformanceStats stats = startMeter(SESSION_STARTED, computer);
      computer.sessionStarted();
      stopMeter(stats, SESSION_ENDED);

      return;
    } catch (InvalidRegistryObjectException x) {
      status = createExceptionStatus(x);
    } catch (CoreException x) {
      status = createExceptionStatus(x);
    } catch (RuntimeException x) {
      status = createExceptionStatus(x);
    }

    fRegistry.informUser(this, status);
  }

  CompletionProposalCategory getCategory() {
    return fCategory;
  }

  /**
   * Returns the contributor of the described extension.
   * 
   * @return the contributor of the described extension
   */
  IContributor getContributor() {
    try {
      return fElement.getContributor();
    } catch (InvalidRegistryObjectException e) {
      return null;
    }
  }

  /**
   * Checks an element that must be defined according to the extension point schema. Throws an
   * <code>InvalidRegistryObjectException</code> if <code>obj</code> is <code>null</code>.
   */
  private void checkNotNull(Object obj, String attribute) throws InvalidRegistryObjectException {
    if (obj == null) {
      Object[] args = {getId(), fElement.getContributor().getName(), attribute};
      String message = Messages.format(
          DartTextMessages.CompletionProposalComputerDescriptor_illegal_attribute_message,
          args);
      IStatus status = new Status(
          IStatus.WARNING,
          DartToolsPlugin.getPluginId(),
          IStatus.OK,
          message,
          null);
      DartToolsPlugin.log(status);
      throw new InvalidRegistryObjectException();
    }
  }

  private IStatus createAPIViolationStatus(String operation) {
    String blame = createBlameMessage();
    Object[] args = {operation};
    String reason = Messages.format(
        DartTextMessages.CompletionProposalComputerDescriptor_reason_API,
        args);
    return new Status(IStatus.WARNING, DartToolsPlugin.getPluginId(), IStatus.OK, blame
        + " " + reason, null); //$NON-NLS-1$
  }

  private String createBlameMessage() {
    Object[] args = {getName(), fElement.getDeclaringExtension().getContributor().getName()};
    String disable = Messages.format(
        DartTextMessages.CompletionProposalComputerDescriptor_blame_message,
        args);
    return disable;
  }

  private IStatus createExceptionStatus(CoreException x) {
    // unable to instantiate the extension - log & disable
    String blame = createBlameMessage();
    String reason = DartTextMessages.CompletionProposalComputerDescriptor_reason_instantiation;
    return new Status(IStatus.ERROR, DartToolsPlugin.getPluginId(), IStatus.OK, blame
        + " " + reason, x); //$NON-NLS-1$
  }

  private IStatus createExceptionStatus(InvalidRegistryObjectException x) {
    // extension has become invalid - log & disable
    String blame = createBlameMessage();
    String reason = DartTextMessages.CompletionProposalComputerDescriptor_reason_invalid;
    return new Status(
        IStatus.INFO,
        DartToolsPlugin.getPluginId(),
        IStatus.OK,
        blame + " " + reason, x); //$NON-NLS-1$
  }

  private IStatus createExceptionStatus(RuntimeException x) {
    // misbehaving extension - log & disable
    String blame = createBlameMessage();
    String reason = DartTextMessages.CompletionProposalComputerDescriptor_reason_runtime_ex;
    return new Status(IStatus.WARNING, DartToolsPlugin.getPluginId(), IStatus.OK, blame
        + " " + reason, x); //$NON-NLS-1$
  }

  private IStatus createPerformanceStatus(String operation) {
    String blame = createBlameMessage();
    Object[] args = {operation};
    String reason = Messages.format(
        DartTextMessages.CompletionProposalComputerDescriptor_reason_performance,
        args);
    return new Status(IStatus.WARNING, DartToolsPlugin.getPluginId(), IStatus.OK, blame
        + " " + reason, null); //$NON-NLS-1$
  }

  private Bundle getBundle() {
    String namespace = fElement.getDeclaringExtension().getContributor().getName();
    Bundle bundle = Platform.getBundle(namespace);
    return bundle;
  }

  /**
   * Returns a cached instance of the computer as described in the extension's xml. The computer is
   * {@link #createComputer() created} the first time that this method is called and then cached.
   * 
   * @return a new instance of the completion proposal computer as described by this descriptor
   * @throws CoreException if the creation fails
   * @throws InvalidRegistryObjectException if the extension is not valid any longer (e.g. due to
   *           plug-in unloading)
   */
  private synchronized IDartCompletionProposalComputer getComputer() throws CoreException,
      InvalidRegistryObjectException {
    if (fComputer == null && (fActivate || isPluginLoaded())) {
      fComputer = createComputer();
    }
    return fComputer;
  }

  /**
   * Returns the enablement state of the described extension.
   * 
   * @return the enablement state of the described extension
   */
  private boolean isEnabled() {
    return fCategory.isEnabled();
  }

  private boolean isPluginLoaded() {
    Bundle bundle = getBundle();
    return bundle != null && bundle.getState() == Bundle.ACTIVE;
  }

  private PerformanceStats startMeter(Object context, IDartCompletionProposalComputer computer) {
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

  private void stopMeter(final PerformanceStats stats, String operation) {
    if (MEASURE_PERFORMANCE) {
      stats.endRun();
      if (stats.isFailure()) {
        IStatus status = createPerformanceStatus(operation);
        fRegistry.informUser(this, status);
        return;
      }
    }

    if (fIsReportingDelay) {
      long current = System.currentTimeMillis();
      if (current - fStart > MAX_DELAY) {
        IStatus status = createPerformanceStatus(operation);
        fRegistry.informUser(this, status);
      }
    }
  }

}
