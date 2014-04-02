/**
 * 
 */
package com.xored.glance.internal.ui.sources;

import com.xored.glance.internal.ui.GlancePlugin;
import com.xored.glance.ui.sources.ITextSourceDescriptor;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yuri Strot
 */
public class TextSourceManager {

  private final static String ATTR_PRIORITY = "priority";

  private final static String ATTR_CLASS = "class";

  private final static String EXT_SOURCES = GlancePlugin.PLUGIN_ID + ".sources";

  private final static String EXT_EXCLUDED_SOURCES = GlancePlugin.PLUGIN_ID + ".excludedSources";

  private static TextSourceManager instance;

  public static TextSourceManager getInstance() {
    if (instance == null) {
      instance = new TextSourceManager();
    }
    return instance;
  }

  private TextSourceListener listener;

  private ITextSourceDescriptor[] descriptors;

  private TextSourceManager() {
  }

  public void addSourceProviderListener(ISourceProviderListener listener) {
    assert listener != null;
    getListener().addSourceProviderListener(listener);
  }

  public TextSourceMaker getSource() {
    return getListener().getSelection();
  }

  public void removeSourceProviderListener(ISourceProviderListener listener) {
    getListener().removeSourceProviderListener(listener);
  }

  private ITextSourceDescriptor[] getDescriptors() {
    if (descriptors == null) {

      List<Class<?>> excludedClasses = new ArrayList<Class<?>>();

      IConfigurationElement[] excludedConfigs = Platform.getExtensionRegistry().getConfigurationElementsFor(
          EXT_EXCLUDED_SOURCES);

      for (IConfigurationElement config : excludedConfigs) {
        try {
          ITextSourceDescriptor descriptor = (ITextSourceDescriptor) config.createExecutableExtension(ATTR_CLASS);
          excludedClasses.add(descriptor.getClass());
        } catch (Exception e) {
          GlancePlugin.log(e);
        }
      }

      IConfigurationElement[] configs = Platform.getExtensionRegistry().getConfigurationElementsFor(
          EXT_SOURCES);
      List<ITextSourceDescriptor> list = new ArrayList<ITextSourceDescriptor>();
      final Map<ITextSourceDescriptor, Integer> prior = new HashMap<ITextSourceDescriptor, Integer>();

      for (IConfigurationElement config : configs) {
        try {
          ITextSourceDescriptor descriptor = (ITextSourceDescriptor) config.createExecutableExtension(ATTR_CLASS);
          if (excludedClasses.contains(descriptor.getClass())) {
            continue;
          }
          int priority = toInt(config.getAttribute(ATTR_PRIORITY));
          list.add(descriptor);
          prior.put(descriptor, priority);
        } catch (Exception e) {
          GlancePlugin.log(e);
        }
      }

      Collections.sort(list, new Comparator<ITextSourceDescriptor>() {

        @Override
        public int compare(ITextSourceDescriptor o1, ITextSourceDescriptor o2) {
          return prior.get(o2) - prior.get(o1);
        }

      });
      descriptors = list.toArray(new ITextSourceDescriptor[list.size()]);
    }
    return descriptors;
  }

  private TextSourceListener getListener() {
    if (listener == null) {
      listener = new TextSourceListener(getDescriptors());
    }
    return listener;
  }

  private int toInt(String priority) {
    try {
      if (priority == null || priority.length() == 0) {
        return 1;
      }
      return Integer.parseInt(priority);
    } catch (Exception e) {
      GlancePlugin.log(e);
    }
    return 1;
  }

}
