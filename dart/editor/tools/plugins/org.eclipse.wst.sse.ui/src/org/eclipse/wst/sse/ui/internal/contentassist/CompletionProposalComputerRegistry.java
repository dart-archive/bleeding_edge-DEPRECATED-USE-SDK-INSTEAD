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
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wst.sse.ui.contentassist.AutoActivationDelegate;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * A registry for all extensions to the <code>org.eclipse.wst.sse.ui.completionProposal</code>
 * extension point.
 * </p>
 */
public final class CompletionProposalComputerRegistry {

  /** The extension schema name of the extension point */
  private static final String EXTENSION_POINT = "completionProposal"; //$NON-NLS-1$

  /** The extension schema name of proposal category child elements. */
  private static final String ELEM_PROPOSAL_CATEGORY = "proposalCategory"; //$NON-NLS-1$

  /** The extension schema name of proposal computer child elements. */
  private static final String ELEM_PROPOSAL_COMPUTER = "proposalComputer"; //$NON-NLS-1$

  /** The extension schema name of proposal computer activation child elements. */
  private static final String ELEM_PROPOSAL_COMPUTER_EXTENDED_ACTIVATION = "proposalComputerExtendedActivation"; //$NON-NLS-1$

  /** The extension schema name for element ID attributes */
  private static final String ATTR_ID = "id"; //$NON-NLS-1$

  /** The extension schema name for the partition's auto-activation class */
  private static final String ATTR_AUTO_ACTIVATION_CLASS = "autoActivationDelegate"; //$NON-NLS-1$

  /** preference key to keep track of the last known number of content assist computers */
  private static final String NUM_COMPUTERS_PREF_KEY = "content_assist_number_of_computers"; //$NON-NLS-1$

  /**
   * A fake partition type ID stating used to say a {@link CompletionProposalComputerDescriptor}
   * should be associated with all partition types in a given content type.
   */
  private static final String ALL_PARTITION_TYPES_ID = "all_partition_types_fake_ID"; //$NON-NLS-1$

  /** State where in the registry has not yet been loaded */
  private static final byte NONE = 0;

  /** State where the registry is being initialized */
  private static final byte INITIALIZING = 1;

  /** State where in the registry is currently being loaded */
  private static final byte LOADING = 2;

  /** State where in the registry has been loaded */
  private static final byte LOADED = 3;

  /** The singleton instance. */
  private static CompletionProposalComputerRegistry fgSingleton = null;

  /**
   * @return the singleton instance of the registry
   */
  public static synchronized CompletionProposalComputerRegistry getDefault() {
    if (fgSingleton == null) {
      fgSingleton = new CompletionProposalComputerRegistry();
    }

    return fgSingleton;
  }

  /**
   * <code>{@link Map}&lt{@link String}, {@link CompletionProposalContentTypeContext}&gt</code>
   * <ul>
   * <li><b>key:</b> content type ID</li>
   * <li><b>value:</b> the context for the associated content type ID</li>
   * <ul>
   */
  private final Map fActivationContexts;

  /**
   * <code>{@link Map}&lt{@link String}, {@link CompletionProposalComputerDescriptor}&gt</code>
   * <ul>
   * <li><b>key:</b> descriptor ID</li>
   * <li><b>value:</b> descriptor</li>
   * <ul>
   */
  private final Map fDescriptors = new HashMap();

  /**
   * A map maintaining the relationship between content types, partition types and their associated
   * {@link AutoActivationDelegate}s
   */
  private Map fAutoActivators = new HashMap();

  /** The {@link CompletionProposalCategory}s tracked by this registry */
  private final List fCategories = new ArrayList();

  /** Unmodifiable public list of the {@link CompletionProposalCategory}s tracked by this registry */
  private final List fPublicCategories = Collections.unmodifiableList(fCategories);

  /**
   * <p>
   * The current state of the registry
   * </p>
   * 
   * @see #NONE
   * @see #LOADING
   * @see #LOADED
   */
  private byte fState;

  /**
   * Lock that is held while loading, operations waiting for load to finish can lock on this as well
   */
  private final Object fLoadingLock;

  /** <code>true</code> if computers have been uninstalled since last load */
  private boolean fHasUninstalledComputers = false;

  /**
   * Creates a new instance.
   */
  private CompletionProposalComputerRegistry() {
    this.fActivationContexts = new HashMap();
    this.fState = CompletionProposalComputerRegistry.NONE;
    this.fLoadingLock = new Object();
  }

  /**
   * <p>
   * Calling this when the registry has not yet been loaded will start a {@link Thread} to start
   * loading the registry. All other operations on the registry will be blocked until the loading
   * has completed. If the registry has already been loaded or is loading then this method does
   * nothing.
   * </p>
   */
  public synchronized void initialize() {
    if (fState == CompletionProposalComputerRegistry.NONE) {
      fState = CompletionProposalComputerRegistry.INITIALIZING;
      new Thread() {
        public void run() {
          CompletionProposalComputerRegistry.this.load();
        }
      }.start();
    }
  }

  /**
   * <p>
   * <b>NOTE: </b>The returned list is read-only and is sorted in the order that the extensions were
   * read in. There are no duplicate elements in the returned list. The returned list may change if
   * plug-ins are loaded or unloaded while the application is running.
   * </p>
   * 
   * @return the list of proposal categories contributed to the
   *         <code>org.eclipse.wst.sse.ui.completionProposal</code> extension point (element type:
   *         {@link CompletionProposalCategory})
   */
  public List getProposalCategories() {
    internalEnsureLoaded();
    return fPublicCategories;
  }

  /**
   * <p>
   * <b>NOTE: </b>The returned list is read-only and is sorted in the order that the extensions were
   * read in. There are no duplicate elements in the returned list. The returned list may change if
   * plug-ins are loaded or unloaded while the application is running.
   * </p>
   * 
   * @param contentTypeID get the {@link CompletionProposalCategory}s associated with this ID
   * @return the {@link CompletionProposalCategory}s associated with the given content type ID
   */
  public List getProposalCategories(String contentTypeID) {
    internalEnsureLoaded();
    List result = new ArrayList();
    for (int i = 0; i < fCategories.size(); ++i) {
      CompletionProposalCategory category = ((CompletionProposalCategory) fCategories.get(i));
      if (category.hasComputers(contentTypeID)) {
        result.add(category);
      }
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * @return <code>true</code> if the registry detected that computers got uninstalled since the
   *         last run <code>false</code> otherwise or if {@link #resetUnistalledComputers()} has
   *         been called
   */
  public boolean hasUninstalledComputers() {
    return fHasUninstalledComputers;
  }

  /**
   * <p>
   * Clears the setting that uninstalled computers have been detected. This setting is used to
   * decide weather a helpful message should be displayed to the user
   * </p>
   */
  public void resetUnistalledComputers() {
    fHasUninstalledComputers = false;
  }

  /**
   * <p>
   * Adds the given {@link CompletionProposalComputerDescriptor} to the registry.
   * </p>
   * 
   * @param contentTypeID the ID of the content type to associated the descriptor with
   * @param partitionTypeID the ID of the partition type in the content type to associate the
   *          descriptor with, or <code>null</code> to associate with all partition types in the
   *          content type.
   * @param descriptor the {@link CompletionProposalComputerDescriptor} to associate with the given
   *          content type and partition type
   */
  void putDescription(String contentTypeID, String partitionTypeID,
      CompletionProposalComputerDescriptor descriptor) {

    if (partitionTypeID == null) {
      partitionTypeID = ALL_PARTITION_TYPES_ID;
    }

    CompletionProposalContentTypeContext context = getContext(contentTypeID);
    context.putDescriptor(partitionTypeID, descriptor);
  }

  void putAutoActivator(String contentTypeID, String partitionTypeID, IConfigurationElement element) {
    String autoActivationClass = element.getAttribute(ATTR_AUTO_ACTIVATION_CLASS);
    if (autoActivationClass == null)
      return;

    Map partitionMap = (Map) fAutoActivators.get(contentTypeID);
    if (partitionMap == null) {
      partitionMap = new HashMap();
      fAutoActivators.put(contentTypeID, partitionMap);
    }
    partitionMap.put(partitionTypeID, new Activator(element));
  }

  public AutoActivationDelegate getActivator(String contentTypeID, String partitionTypeID) {
    Map partitionMap = (Map) fAutoActivators.get(contentTypeID);
    if (partitionMap != null) {
      Activator activator = (Activator) partitionMap.get(partitionTypeID);
      if (activator != null) {
        return activator.createAutoActivation();
      }
    }
    return null;
  }

  /**
   * @param contentTypeID get only descriptors associated with this content type
   * @param partitionTypeID get only descriptors associated with this partition type as well as
   *          describers associated with any partition type in the given content type
   * @return all of the {@link CompletionProposalComputerDescriptor}s associated with the given
   *         content type and partition type (including any describers associated with all partition
   *         types in the given content type)
   */
  List getProposalComputerDescriptors(String contentTypeID, String partitionTypeID) {
    internalEnsureLoaded();

    Set descriptorsSet = new HashSet();
    List contexts = this.getContexts(contentTypeID);
    for (int i = 0; i < contexts.size(); ++i) {
      CompletionProposalContentTypeContext contentSpecificContext = (CompletionProposalContentTypeContext) contexts.get(i);

      //add all descriptors specific to the given content type and the given partition type
      descriptorsSet.addAll(contentSpecificContext.getDescriptors(partitionTypeID));

      //add all descriptors specific to the given content type but not specific to a partition type
      descriptorsSet.addAll(contentSpecificContext.getDescriptors(ALL_PARTITION_TYPES_ID));
    }

    List descriptors = new ArrayList(descriptorsSet);
    return descriptors != null ? Collections.unmodifiableList(descriptors) : Collections.EMPTY_LIST;
  }

  /**
   * @param contentTypeID get only descriptors associated with this content type
   * @return all of the {@link CompletionProposalComputerDescriptor}s associated with the given
   *         content type
   */
  List getProposalComputerDescriptors(String contentTypeID) {
    internalEnsureLoaded();

    Set descriptorsSet = new HashSet();

    List contexts = this.getContexts(contentTypeID);
    for (int i = 0; i < contexts.size(); ++i) {
      CompletionProposalContentTypeContext contentSpecificContext = (CompletionProposalContentTypeContext) contexts.get(i);

      //add all descriptors specific to the given content type
      descriptorsSet.addAll(contentSpecificContext.getDescriptors());
    }

    List descriptors = new ArrayList(descriptorsSet);
    return descriptors != null ? Collections.unmodifiableList(descriptors) : Collections.EMPTY_LIST;
  }

  /**
   * @return Unmodifiable list of all of the {@link CompletionProposalComputerDescriptor}s
   *         associated with this registry
   */
  List getProposalComputerDescriptors() {
    internalEnsureLoaded();
    return Collections.unmodifiableList(new ArrayList(fDescriptors.values()));
  }

  /**
   * <p>
   * This method performs differently depending on the state of the registry
   * </p>
   * <ul>
   * <li>{@link #NONE} - forces the registry to load now on the current thread</li>
   * <li>{@link #LOADING} - blocks until the registry has finished loading on whichever thread it is
   * being loaded on</li>
   * <li>{@link #LOADED} - immediately returns</li>
   * </ul>
   */
  private void internalEnsureLoaded() {
    switch (fState) {
      case CompletionProposalComputerRegistry.NONE:
      case CompletionProposalComputerRegistry.INITIALIZING: {
        load();
        break;
      }
      case CompletionProposalComputerRegistry.LOADING: {
        //as soon as this lock is released the loading process is done
        synchronized (fLoadingLock) {
          //sanity check
          Assert.isTrue(fState == CompletionProposalComputerRegistry.LOADED,
              "The state of the registry should be guaranteed to be LOADED " + //$NON-NLS-1$
                  " once the loading lock has been released."); //$NON-NLS-1$
        }
        break;
      }
    }
  }

  /**
   * <p>
   * Loads the completion proposal extension points if they have not been loaded already
   * </p>
   */
  private void load() {
    /*
     * hold this lock while loading, others waiting for load to finish can synchronize on this lock
     * to be guaranteed the load will be done when the lock is released
     */
    synchronized (fLoadingLock) {
      if (fState == CompletionProposalComputerRegistry.NONE
          || fState == CompletionProposalComputerRegistry.INITIALIZING) {
        //update the state
        fState = CompletionProposalComputerRegistry.LOADING;

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        List extensionElements = new ArrayList(Arrays.asList(registry.getConfigurationElementsFor(
            SSEUIPlugin.ID, EXTENSION_POINT)));

        Map loadedDescriptors = new HashMap();
        List extendedComputerActivations = new ArrayList();

        //get the categories and remove them from the extension elements
        List categories = getCategories(extensionElements);

        //deal with the proposal computers and set aside the proposal computer activation extensions
        for (Iterator iter = extensionElements.iterator(); iter.hasNext();) {
          IConfigurationElement element = (IConfigurationElement) iter.next();
          try {
            if (element.getName().equals(ELEM_PROPOSAL_COMPUTER)) {
              //create the descriptor and add it to the registry
              CompletionProposalComputerDescriptor desc = new CompletionProposalComputerDescriptor(
                  element, categories);
              desc.addToRegistry();
              loadedDescriptors.put(desc.getId(), desc);
            } else if (element.getName().equals(ELEM_PROPOSAL_COMPUTER_EXTENDED_ACTIVATION)) {
              extendedComputerActivations.add(element);
            }

          } catch (InvalidRegistryObjectException x) {
            /*
             * Element is not valid any longer as the contributing plug-in was unloaded or for some
             * other reason. Do not include the extension in the list and log it
             */
            String message = "The extension ''" + element.toString() + "'' is invalid."; //$NON-NLS-1$ //$NON-NLS-2$
            IStatus status = new Status(IStatus.WARNING, SSEUIPlugin.ID, IStatus.OK, message, x);
            Logger.log(status);
          } catch (CoreException x) {
            Logger.log(x.getStatus());
          }
        }

        //deal with extended computer activations
        for (int i = 0; i < extendedComputerActivations.size(); ++i) {
          IConfigurationElement element = (IConfigurationElement) extendedComputerActivations.get(i);
          String proposalComputerID = element.getAttribute(ATTR_ID);
          CompletionProposalComputerDescriptor descriptor = (CompletionProposalComputerDescriptor) loadedDescriptors.get(proposalComputerID);
          if (descriptor != null) {
            try {
              //add the extra activation contexts to the registry
              CompletionProposalComputerDescriptor.parseActivationAndAddToRegistry(element,
                  descriptor);
            } catch (InvalidRegistryObjectException x) {
              /*
               * Element is not valid any longer as the contributing plug-in was unloaded or for
               * some other reason. Do not include the extension in the list and log it
               */
              String message = "The extension ''" + element.toString() + "'' is invalid."; //$NON-NLS-1$ //$NON-NLS-2$
              IStatus status = new Status(IStatus.WARNING, SSEUIPlugin.ID, IStatus.OK, message, x);
              Logger.log(status);
            } catch (CoreException x) {
              Logger.log(x.getStatus());
            }

          } else {
            //activation extension has invalid computer ID
            Logger.log(Logger.WARNING, "Configuration element " + element + //$NON-NLS-1$
                " intented to extend an existing completion proposal computer" + //$NON-NLS-1$
                " specified an invalid completion proposal computer ID " + //$NON-NLS-1$
                proposalComputerID);
          }
        }

        fCategories.clear();
        fCategories.addAll(categories);

        fDescriptors.clear();
        fDescriptors.putAll(loadedDescriptors);

        updateUninstalledComputerCount();

        //update the state
        fState = CompletionProposalComputerRegistry.LOADED;
      }
    }
  }

  /**
   * <p>
   * Updates the uninstalled computer count
   * </p>
   */
  private void updateUninstalledComputerCount() {
    IPreferenceStore preferenceStore = SSEUIPlugin.getDefault().getPreferenceStore();
    int lastNumberOfComputers = preferenceStore.getInt(NUM_COMPUTERS_PREF_KEY);
    int currNumber = fDescriptors.size();
    fHasUninstalledComputers = lastNumberOfComputers > currNumber;
    preferenceStore.putValue(NUM_COMPUTERS_PREF_KEY, Integer.toString(currNumber));
  }

  /**
   * <p>
   * Configures the categories found in the given {@link IConfigurationElement}s and removes them
   * from the given list.
   * </p>
   * 
   * @param extensionElements {@link IConfigurationElement}s that include proposal category
   *          extensions
   * @return {@link CompletionProposalCategory}s created from the given
   *         {@link IConfigurationElement}s that defined new proposal categories.
   */
  private List getCategories(List extensionElements) {
    List categories = new ArrayList();
    for (Iterator iter = extensionElements.iterator(); iter.hasNext();) {
      IConfigurationElement element = (IConfigurationElement) iter.next();
      try {
        if (element.getName().equals(ELEM_PROPOSAL_CATEGORY)) {
          iter.remove(); // remove from list to leave only computers

          CompletionProposalCategory category = new CompletionProposalCategory(element);
          categories.add(category);
        }
      } catch (InvalidRegistryObjectException x) {
        /*
         * Element is not valid any longer as the contributing plug-in was unloaded or for some
         * other reason. Do not include the extension in the list and log it
         */
        String message = "The extension ''" + element.toString() + "'' has become invalid."; //$NON-NLS-1$ //$NON-NLS-2$
        IStatus status = new Status(IStatus.WARNING, SSEUIPlugin.ID, IStatus.OK, message, x);
        Logger.log(status);
      } catch (CoreException x) {
        Logger.log(x.getStatus());
      }
    }

    return categories;
  }

  /**
   * <p>
   * Gets the {@link CompletionProposalContentTypeContext} associated with the given content type,
   * if one does not already exist then one is created
   * </p>
   * 
   * @param contentTypeID get the {@link CompletionProposalContentTypeContext} associated with this
   *          content type
   * @return the existing or new {@link CompletionProposalContentTypeContext} associated with the
   *         given content type
   */
  private CompletionProposalContentTypeContext getContext(String contentTypeID) {
    CompletionProposalContentTypeContext context = (CompletionProposalContentTypeContext) this.fActivationContexts.get(contentTypeID);
    if (context == null) {
      context = new CompletionProposalContentTypeContext(contentTypeID);
      this.fActivationContexts.put(contentTypeID, context);
    }

    return context;
  }

  /**
   * <p>
   * Gets all of the {@link CompletionProposalContentTypeContext}s associated with the given content
   * type ID. A context is considered associated if its associated content type ID is either the
   * given content type ID or is a base content type ID of the given content type ID.
   * </p>
   * 
   * @param contentTypeID get the contexts for this content type ID
   * @return {@link List} of {@link CompletionProposalContentTypeContext}s associated with the given
   *         content type ID
   */
  private List getContexts(String contentTypeID) {
    List contexts = new ArrayList();
    IContentType contentType = Platform.getContentTypeManager().getContentType(contentTypeID);

    while (contentType != null) {
      Object context = this.fActivationContexts.get(contentType.getId());
      if (context != null) {
        contexts.add(context);
      }

      contentType = contentType.getBaseType();
    }

    return contexts;
  }

  private static class Activator {
    IConfigurationElement fElement;

    public Activator(IConfigurationElement element) {
      fElement = element;
    }

    AutoActivationDelegate createAutoActivation() {
      AutoActivationDelegate activation = null;
      if (fElement != null) {
        try {
          activation = (AutoActivationDelegate) fElement.createExecutableExtension(ATTR_AUTO_ACTIVATION_CLASS);
        } catch (CoreException e) {
        }
      }
      return activation;
    }

  }
}
