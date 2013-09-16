/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.extension;

import com.ibm.icu.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.ISourceEditingTextTools;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.breakpoint.IBreakpointProvider;
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads breakpoint extension registory and returns breakpoint provider instances
 */
public class BreakpointProviderBuilder extends RegistryReader {

  private static final String ATT_CLASS = "class"; //$NON-NLS-1$
  // private static final String ATT_ID = "id"; //$NON-NLS-1$
  private static final String ATT_CONTENT_TYPES = "contentTypes"; //$NON-NLS-1$
  private static final String ATT_EXTENSIONS = "extensions"; //$NON-NLS-1$
  private static BreakpointProviderBuilder instance;
  private static final String PL_BREAKPOINT = "breakpoint"; //$NON-NLS-1$

  private static final String PLUGIN_ID = "org.eclipse.wst.sse.ui"; //$NON-NLS-1$

  private static final String TAG_BREAKPOINT_CONTRIBUTION = "breakpointContribution"; //$NON-NLS-1$
  private static final String TAG_PROVIDER = "provider"; //$NON-NLS-1$

  /*
   * Creates an executable extension. @param element the config element defining the extension
   * @param classAttribute the name of the attribute carrying the class @return the extension object
   * @throws CoreException
   */
  static Object createExecutableExtension(final IConfigurationElement element,
      final String classAttribute) throws CoreException {
    return element.createExecutableExtension(classAttribute);
  }

  /**
   * Creates an extension. If the extension plugin has not been loaded a busy cursor will be
   * activated during the duration of the load.
   * 
   * @param element the config element defining the extension
   * @param classAttribute the name of the attribute carrying the class
   * @return the extension object
   * @throws CoreException
   */
  public static Object createExtension(final IConfigurationElement element,
      final String classAttribute) {
    // If plugin has been loaded create extension.
    // Otherwise, show busy cursor then create extension.
    final Object[] result = new Object[1];
    String pluginId = element.getDeclaringExtension().getNamespace();
    Bundle bundle = Platform.getBundle(pluginId);
    if (bundle.getState() == Bundle.ACTIVE) {
      try {
        result[0] = createExecutableExtension(element, classAttribute);
      } catch (Exception e) {
        handleCreateExecutableException(result, e);
      }
    } else {
      BusyIndicator.showWhile(null, new Runnable() {
        public void run() {
          try {
            result[0] = createExecutableExtension(element, classAttribute);
          } catch (CoreException e) {
            handleCreateExecutableException(result, e);
          }
        }
      });

    }
    return result[0];
  }

  /**
   * returns singleton instance of BreakpointProviderBuilder
   * 
   * @return BreakpointProviderBuilder
   */
  public synchronized static BreakpointProviderBuilder getInstance() {
    if (instance == null) {
      instance = new BreakpointProviderBuilder();
    }
    return instance;
  }

  /**
   * @param result
   * @param e
   */
  private static void handleCreateExecutableException(Object[] result, Throwable e) {
    Logger.logException(e);
    result[0] = null;

  }

  protected List cache;
  private Map map = new HashMap();

  protected String targetContributionTag;

  /*
   * Constructor
   */
  private BreakpointProviderBuilder() {
    super();
  }

  /*
   * Creates a breakpoint provider object to given element @param element configuration element
   * object @return IBreakpointProvider
   */
  protected IBreakpointProvider createBreakpointProvider(IConfigurationElement element) {
    Object obj = createExtension(element, ATT_CLASS);
    if (obj == null)
      return null;
    return (obj instanceof IBreakpointProvider) ? (IBreakpointProvider) obj : null;
  }

  /*
   * Creates an array of breakpoint providers matching the given key to the value of the
   * IConfigurationElement attribute "attrName" @return IBreakpointProvider[]
   */
  protected IBreakpointProvider[] createBreakpointProviders(String attrName, String key) {
    if (cache == null)
      return new IBreakpointProvider[0];

    final int num = cache.size();
    if (num == 0)
      return new IBreakpointProvider[0];

    IBreakpointProvider[] bp = new IBreakpointProvider[num];
    int j = 0;
    for (int i = 0; i < num; i++) {
      Object obj = cache.get(i);
      if (!(obj instanceof IConfigurationElement))
        continue;

      IConfigurationElement element = (IConfigurationElement) obj;
      if (!TAG_PROVIDER.equals(element.getName()))
        continue;

      boolean doCreate = false;

      String attrValues = element.getAttribute(attrName);

      if (attrValues != null) {
        StringTokenizer tokenizer = new StringTokenizer(attrValues, ","); //$NON-NLS-1$
        while (tokenizer.hasMoreTokens()) {
          String type = tokenizer.nextToken();
          if (type.trim().equalsIgnoreCase(key.trim())) {
            doCreate = true;
            break;
          }
        }
      }

      if (doCreate) {
        IBreakpointProvider b = createBreakpointProvider(element);
        if (b != null) {
          bp[j] = b;
          j++;
        }
      }
    }

    IBreakpointProvider[] bp2 = new IBreakpointProvider[j];
    for (int i = 0; i < j; i++) {
      bp2[i] = bp[i];
    }

    return bp2;
  }

  /*
   * Returns a matching array of extension points matching this key. Doesn't cause instantiation of
   * providers. @return IBreakpointProvider[]
   */
  protected IConfigurationElement[] findElements(String key) {
    initCache();

    if (cache == null || cache.size() == 0)
      return new IConfigurationElement[0];

    int num = cache.size();
    List elements = new ArrayList(1);
    for (int i = 0; i < num; i++) {
      Object obj = cache.get(i);
      if (!(obj instanceof IConfigurationElement))
        continue;

      IConfigurationElement element = (IConfigurationElement) obj;
      if (!TAG_PROVIDER.equals(element.getName()))
        continue;

      boolean add = false;
      String types = element.getAttribute(ATT_CONTENT_TYPES);
      String exts = element.getAttribute(ATT_EXTENSIONS);

      if (types == null && exts == null) {
        add = true;
      }

      if (!add && types != null && types.length() > 0) {
        IContentType testType = Platform.getContentTypeManager().getContentType(key);
        StringTokenizer tokenizer = new StringTokenizer(types, ","); //$NON-NLS-1$
        while (tokenizer.hasMoreTokens()) {
          String type = tokenizer.nextToken();
          IContentType contentType = Platform.getContentTypeManager().getContentType(type);
          if (contentType != null && testType != null && testType.isKindOf(contentType)) {
            add = true;
            break;
          }
        }
      }

      if (!add && exts != null) {
        StringTokenizer tokenizer = new StringTokenizer(exts, ","); //$NON-NLS-1$
        while (tokenizer.hasMoreTokens()) {
          String ext = tokenizer.nextToken();
          if (ext.trim().equals(key.trim())) {
            add = true;
            break;
          }
        }
      }

      if (add) {
        elements.add(element);
      }
    }
    return (IConfigurationElement[]) elements.toArray(new IConfigurationElement[0]);
  }

  /**
   * Returns an array of breakpoint providers for a specified content type handler
   * 
   * @param handler a content type handler
   * @param ext file extension
   * @return IBreakpointProvider[]
   */
  public IBreakpointProvider[] getBreakpointProviders(IEditorPart editorpart, String contentTypeID,
      String ext) {
    initCache();

    // Get breakpoint providers for this content type handler
    IBreakpointProvider[] providers1 = new IBreakpointProvider[0];
    IContentType contentType = Platform.getContentTypeManager().getContentType(contentTypeID);
    List holdProviders = new ArrayList(2);
    while (contentType != null) {
      IBreakpointProvider[] providers = (IBreakpointProvider[]) map.get(contentType.getId());
      if (providers == null) {
        providers = createBreakpointProviders(ATT_CONTENT_TYPES, contentType.getId());
        if (providers != null) {
          map.put(contentType.getId(), providers);
        }
      }
      // providers were retrieved from cache or newly created
      if (providers != null) {
        holdProviders.addAll(Arrays.asList(providers));
      }
      contentType = contentType.getBaseType();
    }
    providers1 = (IBreakpointProvider[]) holdProviders.toArray(new IBreakpointProvider[holdProviders.size()]);

    // Get breakpoint providers for this extension
    IBreakpointProvider[] providers2 = new IBreakpointProvider[0];
    if (ext != null) {
      providers2 = (IBreakpointProvider[]) map.get(ext);
      if (providers2 == null) {
        providers2 = createBreakpointProviders(ATT_EXTENSIONS, ext);
        if (providers2 != null) {
          map.put(ext, providers2);
        }
      }
    }

    // create single hash set to remove duplication
    Set s = new HashSet();
    s.addAll(Arrays.asList(providers1));
    s.addAll(Arrays.asList(providers2));

    // create IBreakpointProvider[] to return
    IBreakpointProvider[] providers = new IBreakpointProvider[s.size()];
    Iterator itr = s.iterator();
    int i = 0;
    ISourceEditingTextTools tools = null;
    if (editorpart != null && itr.hasNext())
      tools = (ISourceEditingTextTools) editorpart.getAdapter(ISourceEditingTextTools.class);
    while (itr.hasNext()) {
      providers[i] = (IBreakpointProvider) itr.next();
      providers[i].setSourceEditingTextTools(tools);
      i++;
    }
    return providers;
  }

  /**
   * Returns corresponding resource from given parameters
   * 
   * @param input
   * @param handler
   * @param ext
   * @return IResource
   */
  public IResource getResource(IEditorInput input, String contentType, String ext) {
    IBreakpointProvider[] providers = getBreakpointProviders(null, contentType, ext);
    IResource res = null;
    for (int i = 0; i < providers.length; i++) {
      res = providers[i].getResource(input);
      if (res != null) {
        break;
      }
    }
    return res;
  }

  private void initCache() {
    if (cache == null) {
      cache = new ArrayList();
      readContributions(TAG_BREAKPOINT_CONTRIBUTION, PL_BREAKPOINT);
    }
  }

  /**
   * Returns an array of breakpoint providers for a specified content type handler
   * 
   * @param contentType a content type ID or null
   * @param ext a filename extension or null
   * @return boolean
   */
  public boolean isAvailable(String contentType, String ext) {
    boolean available = false;
    if (ext != null)
      available = findElements(ext).length > 0;
    if (!available && contentType != null)
      available = findElements(contentType).length > 0;
    return available;
  }

  /**
   * Reads the contributions from the registry for the provided workbench part and the provided
   * extension point ID.
   * 
   * @param tag
   * @param extensionPoint
   */
  protected void readContributions(String tag, String extensionPoint) {
    targetContributionTag = tag;
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    readRegistry(registry, PLUGIN_ID, extensionPoint);
  }

  protected boolean readElement(IConfigurationElement element) {
    String tag = element.getName();
    if (tag.equals(targetContributionTag)) {
      readElementChildren(element);
      return true;
    } else if (tag.equals(TAG_PROVIDER)) {
      cache.add(element);
      return true; // just cache the element - don't go into it
    }
    return false;
  }
}
