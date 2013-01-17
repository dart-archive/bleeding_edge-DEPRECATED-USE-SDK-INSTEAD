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

package com.google.dart.tools.debug.ui.internal.presentation;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.dartium.DartiumDebugThread;
import com.google.dart.tools.debug.core.dartium.DartiumDebugVariable;
import com.google.dart.tools.debug.core.server.ServerDebugThread;
import com.google.dart.tools.debug.core.server.ServerDebugVariable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.internal.ui.model.elements.DebugElementLabelProvider;
import org.eclipse.debug.internal.ui.model.elements.ViewerInputProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerInputProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.ui.IDebugUIConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * An adaptor factory to map from Dartium debug elements to presentation label providers.
 * 
 * @see DartiumVariableLabelProvider
 */
@SuppressWarnings("restriction")
public class DebugElementAdapterFactory implements IAdapterFactory {

  static class DartThreadInputProvider extends ViewerInputProvider {
    @Override
    protected Object getViewerInput(Object source, IPresentationContext context,
        IViewerUpdate update) throws CoreException {
      if (source instanceof ServerDebugThread) {
        ServerDebugThread thread = (ServerDebugThread) source;

        return thread.getIsolateVarsPseudoFrame();
      } else if (source instanceof DartiumDebugThread) {
        DartiumDebugThread thread = (DartiumDebugThread) source;

        return thread.getIsolateVarsPseudoFrame();
      }

      return null;
    }

    @Override
    protected boolean supportsContextId(String id) {
      return IDebugUIConstants.ID_VARIABLE_VIEW.equals(id);
    }
  }

  public static void init() {
    IAdapterManager manager = Platform.getAdapterManager();

    DebugElementAdapterFactory factory = new DebugElementAdapterFactory();

    manager.registerAdapters(factory, DartiumDebugVariable.class);
    manager.registerAdapters(factory, DartiumDebugThread.class);
    manager.registerAdapters(factory, ServerDebugVariable.class);
    manager.registerAdapters(factory, ServerDebugThread.class);

    if (!DartCore.isPluginsBuild()) {
      manager.registerAdapters(factory, Launch.class);
    }
  }

  private IAdapterFactory defaultAdapter = new org.eclipse.debug.internal.ui.views.launch.DebugElementAdapterFactory();

  private DartiumVariableLabelProvider dartiumLabelProvider = new DartiumVariableLabelProvider();
  private ServerVariableLabelProvider serverLabelProvider = new ServerVariableLabelProvider();

  private static IElementContentProvider launchContentProvider = new DartLaunchContentProvider();
  private static IElementLabelProvider launchLabelProvider = new DartLaunchElementLabelProvider();

  private static IElementLabelProvider threadLabelProvider = new DebugElementLabelProvider();
  private static IViewerInputProvider threadInputProvider = new DartThreadInputProvider();

  public DebugElementAdapterFactory() {

  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (adapterType.equals(IElementContentProvider.class)) {
      if (adaptableObject instanceof ILaunch) {
        return launchContentProvider;
      }

      // If we don't return the default debug adapter we won't be able to expand any variables.
      return defaultAdapter.getAdapter(adaptableObject, adapterType);
    }

    if (adaptableObject instanceof DartiumDebugVariable) {
      if (adapterType == IElementLabelProvider.class) {
        return dartiumLabelProvider;
      }
    }

    if (adaptableObject instanceof ServerDebugVariable) {
      if (adapterType == IElementLabelProvider.class) {
        return serverLabelProvider;
      }
    }

    if (adapterType.equals(IElementLabelProvider.class)) {
      if (adaptableObject instanceof ILaunch) {
        return launchLabelProvider;
      }
    }

    if (adaptableObject instanceof ServerDebugThread
        || adaptableObject instanceof DartiumDebugThread) {
      // IElementLabelProvider
      if (adapterType.equals(IElementLabelProvider.class)) {
        return threadLabelProvider;
      }

      // IViewerInputProvider
      if (adapterType.equals(IViewerInputProvider.class)) {
        return threadInputProvider;
      }
    }

    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Class[] getAdapterList() {
    List<Class> adapterClasses = new ArrayList<Class>();

    adapterClasses.add(IElementLabelProvider.class);
    adapterClasses.add(IViewerInputProvider.class);

    // For the RCP, we override the content provider for ILaunches on order to shave
    // one level off the process/thread/stack-frame tree.
    if (!DartCore.isPluginsBuild()) {
      adapterClasses.add(IElementContentProvider.class);
    }

    return adapterClasses.toArray(new Class[adapterClasses.size()]);
  }

}
