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
import com.google.dart.tools.debug.core.dartium.DartiumDebugVariable;
import com.google.dart.tools.debug.core.server.ServerDebugVariable;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementLabelProvider;

/**
 * An adaptor factory to map from Dartium debug elements to presentation label providers.
 * 
 * @see DartiumVariableLabelProvider
 */
@SuppressWarnings("restriction")
public class DebugElementAdapterFactory implements IAdapterFactory {

  public static void init() {
    IAdapterManager manager = Platform.getAdapterManager();

    DebugElementAdapterFactory factory = new DebugElementAdapterFactory();

    manager.registerAdapters(factory, DartiumDebugVariable.class);
    manager.registerAdapters(factory, ServerDebugVariable.class);

    if (!DartCore.isPluginsBuild()) {
      manager.registerAdapters(factory, Launch.class);
    }
  }

  private DartiumVariableLabelProvider dartiumLabelProvider = new DartiumVariableLabelProvider();
  private ServerVariableLabelProvider serverLabelProvider = new ServerVariableLabelProvider();

  private static IElementContentProvider launchContentProvider = new DartLaunchContentProvider();
  private static IElementLabelProvider debugElementLabelProvider = new DartLaunchElementLabelProvider();

  public DebugElementAdapterFactory() {

  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Object adaptableObject, Class adapterType) {
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

    if (adapterType.equals(IElementContentProvider.class)) {
      if (adaptableObject instanceof ILaunch) {
        return launchContentProvider;
      }
    }

    if (adapterType.equals(IElementLabelProvider.class)) {
      if (adaptableObject instanceof ILaunch) {
        return debugElementLabelProvider;
      }
    }

    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Class[] getAdapterList() {
    return new Class[] {IElementLabelProvider.class, IElementContentProvider.class};
  }

}
