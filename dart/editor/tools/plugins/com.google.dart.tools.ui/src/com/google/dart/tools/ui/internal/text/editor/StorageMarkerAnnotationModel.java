/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.editor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

/**
 * Source editor resource marker annotation model implementation
 */
public class StorageMarkerAnnotationModel extends ResourceMarkerAnnotationModel {
  public final static String SECONDARY_ID_KEY = "org.eclipse.wst.sse.ui.extensions.breakpoint.path"; //$NON-NLS-1$
  protected IResource fMarkerResource;
  protected String fSecondaryMarkerAttributeValue;

  /**
   * Constructor
   * 
   * @param resource
   */
  public StorageMarkerAnnotationModel(IResource resource) {
    super(resource);
    fMarkerResource = resource;
  }

  public StorageMarkerAnnotationModel(IResource resource, String secondaryID) {
    super(resource);
    fMarkerResource = resource;
    fSecondaryMarkerAttributeValue = secondaryID;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel#createMarkerAnnotation
   * (org.eclipse.core.resources.IMarker)
   */
//	protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
//		/*
//		 * We need to do some special processing if marker is a validation
//		 * (aka problem) marker or if marker is a breakpoint marker so create
//		 * a special marker annotation for those markers. Otherwise, use
//		 * default.
//		 */
//		if (MarkerUtilities.isMarkerType(marker, IMarker.PROBLEM)) {
//			return new StructuredMarkerAnnotation(marker);
//		}
//		return super.createMarkerAnnotation(marker);
//	}

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel#getMarkerPosition
   * (org.eclipse.core.resources.IMarker)
   */
  @Override
  public Position getMarkerPosition(IMarker marker) {
    Position pos = super.getMarkerPosition(marker);

    // if ((pos == null || pos.getLength() == 0) && marker.getType() ==
    // IInternalDebugUIConstants.ANN_INSTR_POINTER_CURRENT) {
    if (pos == null || pos.getLength() == 0) {
      // We probably should create position from marker if marker
      // attributes specify a valid position
      pos = createPositionFromMarker(marker);
    }

    return pos;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel#isAcceptable(org
   * .eclipse.core.resources.IMarker)
   */
  @Override
  protected boolean isAcceptable(IMarker marker) {
//		try {
//			Object attr = marker.getAttribute(IBreakpointConstants.ATTR_HIDDEN);
//			if (attr != null && ((Boolean) attr).equals(Boolean.TRUE))
//				return false;
//		}
//		catch (CoreException e) {
//			// ignore
//		}

    if (fSecondaryMarkerAttributeValue == null) {
      return super.isAcceptable(marker);
    }
    String markerSecondaryMarkerAttributeValue = marker.getAttribute(SECONDARY_ID_KEY, ""); //$NON-NLS-1$
    boolean isSameFile = fSecondaryMarkerAttributeValue.equalsIgnoreCase(markerSecondaryMarkerAttributeValue);
    return super.isAcceptable(marker) && isSameFile;
  }
}
