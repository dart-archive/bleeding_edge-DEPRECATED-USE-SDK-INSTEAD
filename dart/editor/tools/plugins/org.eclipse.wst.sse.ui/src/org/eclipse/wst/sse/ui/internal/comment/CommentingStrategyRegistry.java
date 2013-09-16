/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.comment;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * The registry of {@link CommentingStrategy}s defined by the
 * <code>org.eclipse.wst.sse.ui.commentinStrategy</code> extension point.
 * </p>
 */
public class CommentingStrategyRegistry {
  /** The extension schema name of the extension point */
  private static final String EXTENSION_POINT = "commentingStrategy"; //$NON-NLS-1$

  /** The extension schema name of proposal block comment child elements. */
  private static final String ELEM_PROPOSAL_BLOCK_COMMENTING_STRATEGY = "blockCommentingStrategy"; //$NON-NLS-1$

  /** The extension schema name of proposal line comment child elements. */
  private static final String ELEM_PROPOSAL_LINE_COMMENTING_STRATEGY = "lineCommentingStrategy"; //$NON-NLS-1$

  /** The extension schema name of the content type child elements. */
  private static final String ELEM_CONTENT_TYPE = "contentType"; //$NON-NLS-1$

  /** The extension schema name of the required partition types child elements */
  private static final String ELEM_REQUIRED_PARTITION_TYPES = "requiredPartitionTypes"; //$NON-NLS-1$

  /** The extension schema name of the allowable partition types child elements */
  private static final String ELEM_ALLOWABLE_PARTITION_TYPES = "allowablePartitionTypes"; //$NON-NLS-1$

  /** The extension schema name of partition type child elements */
  private static final String ELEM_PARTITION_TYPE = "partitionType"; //$NON-NLS-1$

  /** The extension schema name of the prefix attribute */
  private static final String ATTR_PREFIX = "prefix"; //$NON-NLS-1$

  /** The extension schema name of the suffix attribute */
  private static final String ATTR_SUFFIX = "suffix"; //$NON-NLS-1$

  /** The extension schema name of the associatedCommentPartitionTypeID attribute */
  private static final String ATTR_ASSOCIATED_COMMENT_PARTITION_TPYPE_ID = "associatedCommentPartitionTypeID"; //$NON-NLS-1$

  /** The extension schema name of the anyPartitionType attribute */
  private static final String ATTR_ANY_PARTITION_TYPE = "anyPartitionType"; //$NON-NLS-1$

  /** The extension schema name for ID attribute */
  private static final String ATTR_ID = "id"; //$NON-NLS-1$

  /** the singleton instance of the registry */
  private static CommentingStrategyRegistry fSingleton = null;

  /** <code>true</code> if this registry has been loaded. */
  private boolean fLoaded;

  /**
   * <p>
   * Registry of content type IDs to {@link BlockCommentingStrategy}s
   * </p>
   * <code>{@link Map}&lt{@link String}, {@link List}&lt{@link BlockContentType}&gt&gt</code>
   * <ul>
   * <li><b>key:</b> content type ID</li>
   * <li><b>value:</b> {@link List} of associated {@link BlockContentType}s</li>
   * <ul>
   */
  private Map fBlockCommentTypes;

  /**
   * <p>
   * Registry of content type IDs to {@link LineCommentingStrategy}s
   * </p>
   * <code>{@link Map}&lt{@link String}, {@link List}&lt{@link LineContentType}&gt&gt</code>
   * <ul>
   * <li><b>key:</b> content type ID</li>
   * <li><b>value:</b> {@link List} of associated {@link LineContentType}s</li>
   * <ul>
   */
  private Map fLineCommentTypes;

  /**
   * @return the single instance of the {@link CommentingStrategyRegistry}
   */
  public static synchronized CommentingStrategyRegistry getDefault() {
    if (fSingleton == null) {
      fSingleton = new CommentingStrategyRegistry();
    }

    return fSingleton;
  }

  /**
   * Singleton constructor for the registry
   */
  private CommentingStrategyRegistry() {
    this.fLoaded = false;
    this.fBlockCommentTypes = new HashMap();
    this.fLineCommentTypes = new HashMap();
  }

  /**
   * @param contentTypeID get only {@link BlockCommentingStrategy}s associated with this content
   *          type
   * @param regions get only {@link BlockCommentingStrategy}s associated with these types of regions
   * @return all the {@link BlockCommentingStrategy}s associated with the given content type and
   *         regions
   */
  public CommentingStrategy getBlockCommentingStrategy(String contentTypeID, ITypedRegion[] regions) {
    return getCommentingStrategy(contentTypeID, regions, this.fBlockCommentTypes);
  }

  /**
   * @param contentTypeID get only {@link LineCommentingStrategy}s associated with this content type
   * @param regions get only {@link LineCommentingStrategy}s associated with these types of regions
   * @return all the {@link LineCommentingStrategy}s associated with the given content type and
   *         regions
   */
  public CommentingStrategy getLineCommentingStrategy(String contentTypeID, ITypedRegion[] regions) {
    return getCommentingStrategy(contentTypeID, regions, this.fLineCommentTypes);
  }

  /**
   * <p>
   * get all the {@link CommentingStrategy}s associated with the given content type and regions from
   * the given registry
   * </p>
   * 
   * @param contentTypeID get only {@link CommentingStrategy}s associated with this content type
   * @param regions get only {@link CommentingStrategy}s associated with these types of regions
   * @param registry get the {@link CommentingStrategy}s from this registry
   * @return all the {@link CommentingStrategy}s associated with the given content type and regions
   *         from the given registry
   */
  private CommentingStrategy getCommentingStrategy(String contentTypeID, ITypedRegion[] regions,
      Map registry) {
    ensureExtensionPointRead();

    CommentingStrategy match = null;
    IContentType contentType = Platform.getContentTypeManager().getContentType(contentTypeID);

    /*
     * get all the commenting strategies for the given content type id, including those registered
     * for parent content types
     */
    List possibleCommentingStrategies = new ArrayList();
    while (contentType != null) {
      List contentTypeCommentingStrategies = (List) registry.get(contentType.getId());
      if (contentTypeCommentingStrategies != null && contentTypeCommentingStrategies.size() > 0) {
        possibleCommentingStrategies.addAll(contentTypeCommentingStrategies);
      }
      contentType = contentType.getBaseType();
    }

    /*
     * find the commenting strategy applicable for the given regions, because strategies were added
     * starting from the most specific content type first, the most specific strategy will always be
     * chosen
     */
    for (int i = 0; i < possibleCommentingStrategies.size() && match == null; ++i) {
      CommentingStrategy commentType = (CommentingStrategy) possibleCommentingStrategies.get(i);
      if (commentType.isApplicableFor(regions)) {
        match = commentType;
      }
    }

    return match;
  }

  /**
   * <p>
   * Ensures that the extensions are read and this registry is built
   * </p>
   */
  private void ensureExtensionPointRead() {
    if (!fLoaded) {
      load();
      fLoaded = true;
    }
  }

  /**
   * <p>
   * Load the extension points into the registry
   * </p>
   */
  private void load() {
    IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
    List extensionElements = new ArrayList(
        Arrays.asList(extensionRegistry.getConfigurationElementsFor(SSEUIPlugin.ID, EXTENSION_POINT)));

    //for each extension
    for (Iterator iter = extensionElements.iterator(); iter.hasNext();) {
      IConfigurationElement element = (IConfigurationElement) iter.next();
      try {
        CommentingStrategy newCommentingStrategy = null;
        Map commentingStrategyRegistry = null;
        //either a block or line commenting strategy
        if (element.getName().equals(ELEM_PROPOSAL_BLOCK_COMMENTING_STRATEGY)) {
          String prefix = element.getAttribute(ATTR_PREFIX);
          checkExtensionAttributeNotNull(prefix, ATTR_PREFIX, element);

          String suffix = element.getAttribute(ATTR_SUFFIX);
          checkExtensionAttributeNotNull(suffix, ATTR_SUFFIX, element);

          if (prefix != null && suffix != null) {
            newCommentingStrategy = new BlockCommentingStrategy(prefix, suffix);
            commentingStrategyRegistry = this.fBlockCommentTypes;
          }
        } else if (element.getName().equals(ELEM_PROPOSAL_LINE_COMMENTING_STRATEGY)) {
          String prefix = element.getAttribute(ATTR_PREFIX);
          checkExtensionAttributeNotNull(prefix, ATTR_PREFIX, element);

          if (prefix != null) {
            newCommentingStrategy = new LineCommentingStrategy(prefix);
            commentingStrategyRegistry = this.fLineCommentTypes;
          }
        }

        //add the new strategy to the registry
        if (commentingStrategyRegistry != null && newCommentingStrategy != null) {
          addCommentingStrategyToRegistry(element, commentingStrategyRegistry,
              newCommentingStrategy);
        } else {
          Logger.log(Logger.WARNING, "Invalid CommentingStrategy extension: " + element); //$NON-NLS-1$
        }
      } catch (CoreException e) {
        Logger.logException(e);
      } catch (InvalidRegistryObjectException x) {
        /*
         * Element is not valid any longer as the contributing plug-in was unloaded or for some
         * other reason. Do not include the extension in the list and log it
         */
        String message = "The extension ''" + element.toString() + "'' is invalid."; //$NON-NLS-1$ //$NON-NLS-2$
        IStatus status = new Status(IStatus.WARNING, SSEUIPlugin.ID, IStatus.WARNING, message, x);
        Logger.log(status);
      }
    }
  }

  /**
   * <p>
   * Checks that the given attribute value is not <code>null</code>.
   * </p>
   * 
   * @param value the object to check if not null
   * @param attribute the attribute
   * @throws InvalidRegistryObjectException if the registry element is no longer valid
   * @throws CoreException if <code>value</code> is <code>null</code>
   */
  private static void checkExtensionAttributeNotNull(Object value, String attribute,
      IConfigurationElement element) throws InvalidRegistryObjectException, CoreException {

    if (value == null) {
      String message = "The extension \"" + element.getDeclaringExtension().getUniqueIdentifier() + //$NON-NLS-1$
          "\" from plug-in \"" + element.getContributor().getName() + //$NON-NLS-1$
          "\" did not specify a value for the required \"" + attribute + //$NON-NLS-1$
          "\" attribute for the element \"" + element.getName() + "\". Disabling the extension."; //$NON-NLS-1$ //$NON-NLS-2$
      IStatus status = new Status(IStatus.WARNING, SSEUIPlugin.ID, IStatus.OK, message, null);
      throw new CoreException(status);
    }
  }

  /**
   * <p>
   * Using the content type element children of the given element add a copy of the given base
   * commenting strategy to the given registry
   * </p>
   * 
   * @param element a {@link IConfigurationElement} with contentType element children
   * @param commentingStrategyRegistry {@link Map} of content type ids to {@link CommentingStrategy}
   *          s to register the given {@link CommentingStrategy} with based on the
   *          <code>element</code>
   * @param baseCommentingStrategy {@link CommentingStrategy} that will be cloned and configured for
   *          each content type in the given <code>element</code>
   */
  private static void addCommentingStrategyToRegistry(IConfigurationElement element,
      Map commentingStrategyRegistry, CommentingStrategy baseCommentingStrategy) {

    //get all the content type elements
    IConfigurationElement[] contentTypeElements = element.getChildren(ELEM_CONTENT_TYPE);
    if (contentTypeElements.length > 0) {
      for (int contentTypeIndex = 0; contentTypeIndex < contentTypeElements.length; ++contentTypeIndex) {
        try {
          String contentTypeID = contentTypeElements[contentTypeIndex].getAttribute(ATTR_ID);
          checkExtensionAttributeNotNull(contentTypeID, ATTR_ID,
              contentTypeElements[contentTypeIndex]);

          List commentTypes = (List) commentingStrategyRegistry.get(contentTypeID);
          if (commentTypes == null) {
            commentTypes = new ArrayList();
            commentingStrategyRegistry.put(contentTypeID, commentTypes);
          }

          //this element is required
          List allowablePartitionTypeIDs = new ArrayList();
          IConfigurationElement[] allowablePartitionTypes = contentTypeElements[contentTypeIndex].getChildren(ELEM_ALLOWABLE_PARTITION_TYPES);
          boolean anyPartitionType = false;
          //determine anyPartitionType attribute value
          String anyPartitionTypeValue = allowablePartitionTypes[0].getAttribute(ATTR_ANY_PARTITION_TYPE);
          if (anyPartitionTypeValue != null) {
            anyPartitionType = Boolean.valueOf(anyPartitionTypeValue).booleanValue();
          }

          //get the optional partition types
          allowablePartitionTypes = allowablePartitionTypes[0].getChildren(ELEM_PARTITION_TYPE);
          if (allowablePartitionTypes.length > 0) {
            for (int partitionTypeIndex = 0; partitionTypeIndex < allowablePartitionTypes.length; ++partitionTypeIndex) {
              String partitionTypeID = allowablePartitionTypes[partitionTypeIndex].getAttribute(ATTR_ID);
              checkExtensionAttributeNotNull(partitionTypeID, ATTR_ID,
                  allowablePartitionTypes[partitionTypeIndex]);

              allowablePartitionTypeIDs.add(partitionTypeID);
            }
          }

          //this element is optional
          List requiredPartitionTypeIDs = new ArrayList();
          IConfigurationElement[] requiredPartitionTypes = contentTypeElements[contentTypeIndex].getChildren(ELEM_REQUIRED_PARTITION_TYPES);
          if (requiredPartitionTypes.length > 0) {
            //get the required partition types
            requiredPartitionTypes = requiredPartitionTypes[0].getChildren(ELEM_PARTITION_TYPE);
            if (requiredPartitionTypes.length > 0) {
              for (int partitionTypeIndex = 0; partitionTypeIndex < requiredPartitionTypes.length; ++partitionTypeIndex) {
                String partitionTypeID = requiredPartitionTypes[partitionTypeIndex].getAttribute(ATTR_ID);
                checkExtensionAttributeNotNull(partitionTypeID, ATTR_ID,
                    requiredPartitionTypes[partitionTypeIndex]);

                requiredPartitionTypeIDs.add(partitionTypeID);
              }
            }
          }

          //get the optional associated comment partition type ID
          String associatedCommentPartitionTypeID = contentTypeElements[contentTypeIndex].getAttribute(ATTR_ASSOCIATED_COMMENT_PARTITION_TPYPE_ID);

          //register the strategy
          CommentingStrategy newCommentingStrategy = (CommentingStrategy) baseCommentingStrategy.clone();
          newCommentingStrategy.setPartitionInformation(allowablePartitionTypeIDs,
              anyPartitionType, requiredPartitionTypeIDs, associatedCommentPartitionTypeID);
          commentTypes.add(newCommentingStrategy);
        } catch (CoreException e) {
          Logger.logException(e);
        }
      }
    } else {
      Logger.log(Logger.WARNING, "The commmenting strategy element: " + element + //$NON-NLS-1$
          " does not contain any required " + ELEM_CONTENT_TYPE + "s"); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }
}
