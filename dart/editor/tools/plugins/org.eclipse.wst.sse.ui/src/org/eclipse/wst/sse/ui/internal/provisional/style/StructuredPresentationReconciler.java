/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.sse.ui.internal.provisional.style;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.DocumentPartitioningChangedEvent;
import org.eclipse.jface.text.DocumentRewriteSessionEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioningListener;
import org.eclipse.jface.text.IDocumentPartitioningListenerExtension;
import org.eclipse.jface.text.IDocumentPartitioningListenerExtension2;
import org.eclipse.jface.text.IDocumentRewriteSessionListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.IPresentationReconcilerExtension;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.wst.sse.core.internal.provisional.events.IStructuredDocumentListener;
import org.eclipse.wst.sse.core.internal.provisional.events.NewDocumentEvent;
import org.eclipse.wst.sse.core.internal.provisional.events.NoChangeEvent;
import org.eclipse.wst.sse.core.internal.provisional.events.RegionChangedEvent;
import org.eclipse.wst.sse.core.internal.provisional.events.RegionsReplacedEvent;
import org.eclipse.wst.sse.core.internal.provisional.events.StructuredDocumentRegionsReplacedEvent;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegionList;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StructuredPresentationReconciler implements IPresentationReconciler,
    IPresentationReconcilerExtension {

  /** Prefix of the name of the position category for tracking damage regions. */
  final static String TRACKED_PARTITION = "__reconciler_tracked_partition"; //$NON-NLS-1$

  private final static boolean _trace = Boolean.valueOf(
      Platform.getDebugOption("org.eclipse.wst.sse.ui/structuredPresentationReconciler")).booleanValue(); //$NON-NLS-1$
  private final static boolean _traceTime = Boolean.valueOf(
      Platform.getDebugOption("org.eclipse.wst.sse.ui/structuredPresentationReconciler/time")).booleanValue(); //$NON-NLS-1$
  private static final String TRACE_PREFIX = "StructuredPresentationReconciler: ";
  private long time0;
  private long time1;

  /**
   * Internal listener class.
   */
  class InternalListener implements ITextInputListener, IDocumentListener, ITextListener,
      IStructuredDocumentListener, IDocumentPartitioningListener,
      IDocumentPartitioningListenerExtension, IDocumentPartitioningListenerExtension2,
      IDocumentRewriteSessionListener {

    /** Set to <code>true</code> if between a document about to be changed and a changed event. */
    private boolean fDocumentChanging = false;

    /** Flag for the document being in a rewrite session */
    private boolean fInRewriteSession = false;

    /** Flag for some kind of changes being applied during a document rewrite session */
    private boolean fHasIncomingChanges = false;

    /**
     * The cached redraw state of the text viewer.
     * 
     * @since 3.0
     */
    private boolean fCachedRedrawState = true;

    public void newModel(NewDocumentEvent structuredDocumentEvent) {
      if (fInRewriteSession) {
        fHasIncomingChanges = true;
        return;
      }
      if (_trace) {
        time1 = System.currentTimeMillis();
      }
      int length = structuredDocumentEvent.getLength();
      recordDamage(new Region(0, length), structuredDocumentEvent.getDocument());
      if (_trace) {
        System.out.println("\n" + TRACE_PREFIX + "calculated damage for NewDocumentEvent: (length=" + length + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.flush();
      }
      if (_trace && _traceTime) {
        System.out.println(TRACE_PREFIX
            + "calculated damage for NewDocumentEvent in " + (System.currentTimeMillis() - time1) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.flush();
      }
    }

    public void noChange(NoChangeEvent structuredDocumentEvent) {
      if (fInRewriteSession) {
        fHasIncomingChanges = true;
        return;
      }
      if (_trace) {
        time1 = System.currentTimeMillis();
      }
      if (structuredDocumentEvent.reason == NoChangeEvent.NO_CONTENT_CHANGE) {
        IRegion damage = new Region(structuredDocumentEvent.fOffset,
            structuredDocumentEvent.fLength);
        recordDamage(damage, structuredDocumentEvent.fDocument);
      }
      if (_trace && _traceTime) {
        System.out.println("\n" + TRACE_PREFIX + "calculated damage for NoChangeEvent in " + (System.currentTimeMillis() - time1) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.flush();
      }
    }

    public void nodesReplaced(StructuredDocumentRegionsReplacedEvent structuredDocumentEvent) {
      if (fInRewriteSession) {
        fHasIncomingChanges = true;
        return;
      }
      if (_trace) {
        time1 = System.currentTimeMillis();
      }
      IRegion damage;
      IStructuredDocumentRegionList newDocumentRegions = structuredDocumentEvent.getNewStructuredDocumentRegions();
      if (newDocumentRegions.getLength() > 0) {
        int startOffset = newDocumentRegions.item(0).getStartOffset();
        int length = newDocumentRegions.item(newDocumentRegions.getLength() - 1).getEndOffset()
            - startOffset;
        damage = new Region(startOffset, length);

      } else {
        damage = new Region(structuredDocumentEvent.fOffset, structuredDocumentEvent.getLength());
      }
      recordDamage(damage, structuredDocumentEvent.fDocument);
      if (_trace) {
        System.out.println("\n" + TRACE_PREFIX + "calculated damage for StructuredDocumentRegionsReplacedEvent: [" + damage.getOffset() + ":" + damage.getLength() + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
        System.out.flush();
      }
      if (_trace && _traceTime) {
        System.out.println(TRACE_PREFIX
            + "calculated damage for StructuredDocumentRegionsReplacedEvent in " + (System.currentTimeMillis() - time1) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.flush();
      }
    }

    public void regionChanged(RegionChangedEvent structuredDocumentEvent) {
      if (fInRewriteSession) {
        fHasIncomingChanges = true;
        return;
      }
      if (_trace) {
        time1 = System.currentTimeMillis();
      }
      IStructuredDocumentRegion documentRegion = structuredDocumentEvent.getStructuredDocumentRegion();
      ITextRegion changedRegion = structuredDocumentEvent.getRegion();
      int startOffset = documentRegion.getStartOffset(changedRegion);
      int length = changedRegion.getLength();
      IRegion damage = new Region(startOffset, length);

      recordDamage(damage, structuredDocumentEvent.fDocument);
      if (_trace) {
        System.out.println("\n" + TRACE_PREFIX + "calculated damage for RegionChangedEvent: [" + damage.getOffset() + ":" + damage.getLength() + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
        System.out.flush();
      }
      if (_trace && _traceTime) {
        System.out.println(TRACE_PREFIX
            + "calculated damage for RegionChangedEvent in " + (System.currentTimeMillis() - time1) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.flush();
      }
    }

    public void regionsReplaced(RegionsReplacedEvent structuredDocumentEvent) {
      if (fInRewriteSession) {
        fHasIncomingChanges = true;
        return;
      }
      if (_trace) {
        time1 = System.currentTimeMillis();
      }
      IRegion damage;
      IStructuredDocumentRegion documentRegion = structuredDocumentEvent.getStructuredDocumentRegion();
      ITextRegionList newRegions = structuredDocumentEvent.getNewRegions();
      if (newRegions.size() > 0) {
        ITextRegion firstChangedRegion = newRegions.get(0);
        ITextRegion lastChangedRegion = newRegions.get(newRegions.size() - 1);
        int startOffset = documentRegion.getStartOffset(firstChangedRegion);
        int length = documentRegion.getEndOffset(lastChangedRegion) - startOffset;
        damage = new Region(startOffset, length);
      } else {
        damage = new Region(documentRegion.getStartOffset(), documentRegion.getLength());
      }
      recordDamage(damage, structuredDocumentEvent.fDocument);
      if (_trace) {
        System.out.println("\n" + TRACE_PREFIX + "calculated damage for RegionsReplacedEvent: [" + damage.getOffset() + ":" + damage.getLength() + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
        System.out.flush();
      }
      if (_trace && _traceTime) {
        System.out.println(TRACE_PREFIX
            + "calculated damage for RegionsReplacedEvent in " + (System.currentTimeMillis() - time1) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.flush();
      }
    }

    /*
     * @see ITextInputListener#inputDocumentAboutToBeChanged(IDocument, IDocument)
     */
    public void inputDocumentAboutToBeChanged(IDocument oldDocument, IDocument newDocument) {
      if (oldDocument != null) {
        try {

          fViewer.removeTextListener(this);
          oldDocument.removeDocumentListener(this);
          oldDocument.removeDocumentPartitioningListener(this);
          if (oldDocument instanceof IStructuredDocument) {
            ((IStructuredDocument) oldDocument).removeDocumentChangedListener(this);
          }

          oldDocument.removePositionUpdater(fPositionUpdater);
          oldDocument.removePositionCategory(fPositionCategory);

        } catch (BadPositionCategoryException x) {
          // should not happened for former input documents;
        }
      }
    }

    /*
     * @see ITextInputListener#inputDocumenChanged(IDocument, IDocument)
     */
    public void inputDocumentChanged(IDocument oldDocument, IDocument newDocument) {
      if (_trace || _traceTime) {
        time1 = System.currentTimeMillis();
      }

      fDocumentChanging = false;
      fCachedRedrawState = true;

      if (newDocument != null) {
        newDocument.addPositionCategory(fPositionCategory);
        newDocument.addPositionUpdater(fPositionUpdater);

        if (newDocument instanceof IStructuredDocument) {
          newDocument.addDocumentPartitioningListener(this);
          newDocument.addDocumentListener(this);
          ((IStructuredDocument) newDocument).addDocumentChangedListener(this);
        }
        fViewer.addTextListener(this);

        if (newDocument instanceof IStructuredDocument) {
          setDocumentToDamagers(newDocument);
          setDocumentToRepairers(newDocument);
          processDamage(new Region(0, newDocument.getLength()), newDocument);
        }
      }
      if (_trace) {
        System.out.println(TRACE_PREFIX
            + "processed damage for inputDocumentChanged in " + (System.currentTimeMillis() - time1) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }

    /*
     * @see IDocumentPartitioningListener#documentPartitioningChanged(IDocument)
     */
    public void documentPartitioningChanged(IDocument document) {
      if (_traceTime) {
        time0 = System.currentTimeMillis();
      }
      if (!fDocumentChanging && fCachedRedrawState)
        processDamage(new Region(0, document.getLength()), document);
      else
        fDocumentPartitioningChanged = true;
      if (_trace) {
        System.out.println(TRACE_PREFIX
            + "processed damage for documentPartitioningChanged [full document]"); //$NON-NLS-1$
      }
      if (_trace && _traceTime) {
        System.out.println(TRACE_PREFIX
            + "processed damage for documentPartitioningChanged in " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }

    /*
     * @see IDocumentPartitioningListenerExtension#documentPartitioningChanged(IDocument, IRegion)
     * 
     * @since 2.0
     */
    public void documentPartitioningChanged(IDocument document, IRegion changedRegion) {
      if (_traceTime) {
        time0 = System.currentTimeMillis();
      }
      if (!fDocumentChanging && fCachedRedrawState) {
        processDamage(new Region(changedRegion.getOffset(), changedRegion.getLength()), document);
      } else {
        fDocumentPartitioningChanged = true;
        fChangedDocumentPartitions = changedRegion;
      }
      if (_trace) {
        System.out.println(TRACE_PREFIX
            + "processed damage for documentPartitioningChanged [" + changedRegion.getOffset() + ":" + changedRegion.getLength() + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }
      if (_trace && _traceTime) {
        System.out.println(TRACE_PREFIX
            + "processed damage for documentPartitioningChanged in " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }

    /*
     * @see
     * org.eclipse.jface.text.IDocumentPartitioningListenerExtension2#documentPartitioningChanged
     * (org.eclipse.jface.text.DocumentPartitioningChangedEvent)
     * 
     * @since 3.0
     */
    public void documentPartitioningChanged(DocumentPartitioningChangedEvent event) {
      IRegion changedRegion = event.getChangedRegion(getDocumentPartitioning());
      if (changedRegion != null)
        documentPartitioningChanged(event.getDocument(), changedRegion);
    }

    /*
     * @see IDocumentListener#documentAboutToBeChanged(DocumentEvent)
     */
    public void documentAboutToBeChanged(DocumentEvent e) {

      fDocumentChanging = true;
      if (fCachedRedrawState) {
        try {
          int offset = e.getOffset() + e.getLength();
          ITypedRegion region = getPartition(e.getDocument(), offset);
          fRememberedPosition = new TypedPosition(region);
          e.getDocument().addPosition(fPositionCategory, fRememberedPosition);
        } catch (BadLocationException x) {
          // can not happen
        } catch (BadPositionCategoryException x) {
          // should not happen on input elements
        }
      }
    }

    /*
     * @see IDocumentListener#documentChanged(DocumentEvent)
     */
    public void documentChanged(DocumentEvent e) {
      if (fCachedRedrawState) {
        try {
          e.getDocument().removePosition(fPositionCategory, fRememberedPosition);
        } catch (BadPositionCategoryException x) {
          // can not happen on input documents
        }
      }
      fDocumentChanging = false;
    }

    /*
     * @see ITextListener#textChanged(TextEvent)
     */
    public void textChanged(TextEvent e) {
      if (fInRewriteSession) {
        fHasIncomingChanges = true;
        return;
      }
      fCachedRedrawState = e.getViewerRedrawState();
      if (!fCachedRedrawState) {
        if (_trace) {
          System.out.println("\n" + TRACE_PREFIX + "returned early from textChanged(TextEvent)"); //$NON-NLS-1$	 //$NON-NLS-2$	
        }
        return;
      }
      if (_trace) {
        System.out.println("\n" + TRACE_PREFIX + "entering textChanged(TextEvent)"); //$NON-NLS-1$ //$NON-NLS-2$
        time0 = System.currentTimeMillis();
      }

      IRegion damage = null;
      IDocument document = null;

      if (e.getDocumentEvent() == null) {
        document = fViewer.getDocument();
        if (document != null) {
          if (e.getOffset() == 0 && e.getLength() == 0 && e.getText() == null) {
            // redraw state change, damage the whole document
            damage = new Region(0, document.getLength());
          } else {
            IRegion region = widgetRegion2ModelRegion(e);
            try {
              String text = document.get(region.getOffset(), region.getLength());
              DocumentEvent de = new DocumentEvent(document, region.getOffset(),
                  region.getLength(), text);
              damage = getDamage(de, false);
            } catch (BadLocationException x) {
              /* ignored in platform PresentationReconciler, too */
            }
          }
        }
      } else {
        DocumentEvent de = e.getDocumentEvent();
        document = de.getDocument();
        damage = getDamage(de, true);
      }
      if (_trace && _traceTime) {
        System.out.println(TRACE_PREFIX
            + "calculated simple text damage at " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.flush();
      }

      boolean damageOverlaps = processRecordedDamages(damage, document);
      if (_trace && _traceTime) {
        System.out.println(TRACE_PREFIX
            + "processed recorded structured text damage at " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
      }

      if (damage != null && document != null && !damageOverlaps) {
        processDamage(damage, document);
        if (_trace && _traceTime) {
          System.out.println(TRACE_PREFIX
              + "processed simple text damage at " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
          System.out.flush();
        }
      }

      fDocumentPartitioningChanged = false;
      fChangedDocumentPartitions = null;
      if (_trace) {
        System.out.println(TRACE_PREFIX
            + "finished textChanged(TextEvent) in " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }

    /**
     * Translates the given text event into the corresponding range of the viewer's document.
     * 
     * @param e the text event
     * @return the widget region corresponding the region of the given event
     * @since 2.1
     */
    protected IRegion widgetRegion2ModelRegion(TextEvent e) {

      String text = e.getText();
      int length = text == null ? 0 : text.length();

      if (fViewer instanceof ITextViewerExtension5) {
        ITextViewerExtension5 extension = (ITextViewerExtension5) fViewer;
        return extension.widgetRange2ModelRange(new Region(e.getOffset(), length));
      }

      IRegion visible = fViewer.getVisibleRegion();
      IRegion region = new Region(e.getOffset() + visible.getOffset(), length);
      return region;
    }

    public void documentRewriteSessionChanged(DocumentRewriteSessionEvent event) {
      fInRewriteSession = (event != null && event.fChangeType == DocumentRewriteSessionEvent.SESSION_START);
      if (!fInRewriteSession && fHasIncomingChanges && event != null) {
        if (_trace)
          time0 = System.currentTimeMillis();
        processDamage(new Region(0, event.fDocument.getLength()), event.fDocument);
        if (_trace && _traceTime)
          System.out.println(TRACE_PREFIX
              + " processed damaged after ending document rewrite session at " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
        fHasIncomingChanges = false;
      }
    }

  }

  private static class RecordedDamage {
    IRegion damage;
    IDocument document;

    RecordedDamage(IRegion damage, IDocument document) {
      this.damage = damage;
      this.document = document;
    }
  }

  /** The map of presentation damagers. */
  private Map fDamagers;
  /** The map of presentation repairers. */
  private Map fRepairers;
  /** The target viewer. */
  private ITextViewer fViewer;
  /** The internal listener. */
  private InternalListener fInternalListener = new InternalListener();
  /** The name of the position category to track damage regions. */
  private String fPositionCategory;
  /** The position updated for the damage regions' position category. */
  private IPositionUpdater fPositionUpdater;
  /** The positions representing the damage regions. */
  private TypedPosition fRememberedPosition;
  /** Flag indicating the receipt of a partitioning changed notification. */
  private boolean fDocumentPartitioningChanged = false;
  /** The range covering the changed partitioning. */
  private IRegion fChangedDocumentPartitions = null;

  /**
   * Because structured document events fire before textChanged(), it's possible the widget isn't
   * fully aware of the changes to its contents. "Record" the damage to process after textChanged()
   * has been fired.
   */
  private List fRecordedDamages = new ArrayList(2);

  /**
   * The partitioning used by this presentation reconciler.
   * 
   * @since 3.0
   */
  private String fPartitioning;

  private IDocument fLastDocument;

  /**
   * Creates a new presentation reconciler. There are no damagers or repairers registered with this
   * reconciler by default. The default partitioning
   * <code>IDocumentExtension3.DEFAULT_PARTITIONING</code> is used.
   */
  public StructuredPresentationReconciler() {
    super();
    fPartitioning = IDocumentExtension3.DEFAULT_PARTITIONING;
    fPositionCategory = TRACKED_PARTITION + hashCode();
    fPositionUpdater = new DefaultPositionUpdater(fPositionCategory);
  }

  /**
   * Sets the document partitioning for this presentation reconciler.
   * 
   * @param partitioning the document partitioning for this presentation reconciler.
   * @since 3.0
   */
  public void setDocumentPartitioning(String partitioning) {
    Assert.isNotNull(partitioning);
    fPartitioning = partitioning;
  }

  /*
   * @see
   * org.eclipse.jface.text.presentation.IPresentationReconcilerExtension#geDocumenttPartitioning()
   * 
   * @since 3.0
   */
  public String getDocumentPartitioning() {
    return fPartitioning;
  }

  /**
   * Registers the given presentation damager for a particular content type. If there is already a
   * damager registered for this type, the old damager is removed first.
   * 
   * @param damager the presentation damager to register, or <code>null</code> to remove an existing
   *          one
   * @param contentType the content type under which to register
   */
  public void setDamager(IPresentationDamager damager, String contentType) {

    Assert.isNotNull(contentType);

    if (fDamagers == null)
      fDamagers = new HashMap();

    if (damager == null)
      fDamagers.remove(contentType);
    else
      fDamagers.put(contentType, damager);
  }

  /**
   * Registers the given presentation repairer for a particular content type. If there is already a
   * repairer registered for this type, the old repairer is removed first.
   * 
   * @param repairer the presentation repairer to register, or <code>null</code> to remove an
   *          existing one
   * @param contentType the content type under which to register
   */
  public void setRepairer(IPresentationRepairer repairer, String contentType) {

    Assert.isNotNull(contentType);

    if (fRepairers == null)
      fRepairers = new HashMap();

    if (repairer == null)
      fRepairers.remove(contentType);
    else
      fRepairers.put(contentType, repairer);
  }

  /*
   * @see IPresentationReconciler#install(ITextViewer)
   */
  public void install(ITextViewer viewer) {
    if (_trace) {
      time0 = System.currentTimeMillis();
    }
    Assert.isNotNull(viewer);

    fViewer = viewer;
    fViewer.addTextInputListener(fInternalListener);

    IDocument document = viewer.getDocument();
    if (document != null) {
      fInternalListener.inputDocumentChanged(null, document);
      if (document instanceof IStructuredDocument) {
        ((IStructuredDocument) document).addDocumentChangedListener(fInternalListener);
      }
      if (document instanceof IDocumentExtension4) {
        ((IDocumentExtension4) document).addDocumentRewriteSessionListener(fInternalListener);
      }
    }
    if (_trace) {
      System.out.println(TRACE_PREFIX
          + "installed to text viewer in " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  /*
   * @see IPresentationReconciler#uninstall()
   */
  public void uninstall() {
    if (_trace) {
      time0 = System.currentTimeMillis();
    }
    fViewer.removeTextInputListener(fInternalListener);

    IDocument document = null;
    if ((document = fViewer.getDocument()) instanceof IStructuredDocument) {
      ((IStructuredDocument) document).removeDocumentChangedListener(fInternalListener);
    }
    if (document instanceof IDocumentExtension4) {
      ((IDocumentExtension4) document).removeDocumentRewriteSessionListener(fInternalListener);
    }
    // Ensure we uninstall all listeners
    fInternalListener.inputDocumentAboutToBeChanged(fViewer.getDocument(), null);
    if (_trace) {
      System.out.println(TRACE_PREFIX
          + "uninstalled from text viewer in " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  /*
   * @see IPresentationReconciler#getDamager(String)
   */
  public IPresentationDamager getDamager(String contentType) {

    if (fDamagers == null)
      return null;

    final int idx = contentType.indexOf(':');
    if (idx > 0) {
      contentType = contentType.substring(0, idx + 1);
    }
    return (IPresentationDamager) fDamagers.get(contentType);
  }

  /*
   * @see IPresentationReconciler#getRepairer(String)
   */
  public IPresentationRepairer getRepairer(String contentType) {

    if (fRepairers == null)
      return null;

    final int idx = contentType.indexOf(':');
    if (idx > 0) {
      contentType = contentType.substring(0, idx + 1);
    }
    return (IPresentationRepairer) fRepairers.get(contentType);
  }

  /**
   * Informs all registered damagers about the document on which they will work.
   * 
   * @param document the document on which to work
   */
  protected void setDocumentToDamagers(IDocument document) {
    if (fDamagers != null) {
      Iterator e = fDamagers.values().iterator();
      while (e.hasNext()) {
        IPresentationDamager damager = (IPresentationDamager) e.next();
        damager.setDocument(document);
      }
    }
  }

  /**
   * Informs all registered repairers about the document on which they will work.
   * 
   * @param document the document on which to work
   */
  protected void setDocumentToRepairers(IDocument document) {
    if (fRepairers != null) {
      Iterator e = fRepairers.values().iterator();
      while (e.hasNext()) {
        IPresentationRepairer repairer = (IPresentationRepairer) e.next();
        repairer.setDocument(document);
      }
    }
  }

  /**
   * Constructs a "repair description" for the given damage and returns this description as a text
   * presentation. For this, it queries the partitioning of the damage region and asks the
   * appropriate presentation repairer for each partition to construct the "repair description" for
   * this partition.
   * 
   * @param damage the damage to be repaired
   * @param document the document whose presentation must be repaired
   * @return the presentation repair description as text presentation or <code>null</code> if the
   *         partitioning could not be computed
   */
  protected TextPresentation createPresentation(IRegion damage, IDocument document) {
    try {
      int validLength = Math.min(damage.getLength(), document.getLength() - damage.getOffset());

      if (fRepairers == null || fRepairers.isEmpty()) {
        TextPresentation presentation = new TextPresentation(damage, 1);
        presentation.setDefaultStyleRange(new StyleRange(damage.getOffset(), validLength, null,
            null));
        return presentation;
      }

      TextPresentation presentation = new TextPresentation(damage, 1000);

      ITypedRegion[] partitions = TextUtilities.computePartitioning(document,
          getDocumentPartitioning(), damage.getOffset(), validLength, false);
      for (int i = 0; i < partitions.length; i++) {
        ITypedRegion r = partitions[i];
        IPresentationRepairer repairer = getRepairer(r.getType());
        if (repairer != null)
          repairer.createPresentation(presentation, r);
      }

      return presentation;

    } catch (BadLocationException x) {
      /* ignored in platform PresentationReconciler, too */
    }

    return null;
  }

  /**
   * Checks for the first and the last affected partition affected by a document event and calls
   * their damagers. Invalidates everything from the start of the damage for the first partition
   * until the end of the damage for the last partition.
   * 
   * @param e the event describing the document change
   * @param optimize <code>true</code> if partition changes should be considered for optimization
   * @return the damaged caused by the change or <code>null</code> if computing the partitioning
   *         failed
   * @since 3.0
   */
  IRegion getDamage(DocumentEvent e, boolean optimize) {
    int length = e.getText() == null ? 0 : e.getText().length();

    if (fDamagers == null || fDamagers.isEmpty()) {
      length = Math.max(e.getLength(), length);
      length = Math.min(e.getDocument().getLength() - e.getOffset(), length);
      return new Region(e.getOffset(), length);
    }

    boolean isDeletion = length == 0;
    IRegion damage = null;
    try {
      int offset = e.getOffset();
      if (isDeletion)
        offset = Math.max(0, offset - 1);
      ITypedRegion partition = getPartition(e.getDocument(), offset);
      IPresentationDamager damager = getDamager(partition.getType());
      if (damager == null)
        return null;

      IRegion r = damager.getDamageRegion(partition, e, fDocumentPartitioningChanged);

      if (!fDocumentPartitioningChanged && optimize && !isDeletion) {
        damage = r;
      } else {

        int damageEnd = getDamageEndOffset(e);

        int parititionDamageEnd = -1;
        if (fChangedDocumentPartitions != null)
          parititionDamageEnd = fChangedDocumentPartitions.getOffset()
              + fChangedDocumentPartitions.getLength();

        int end = Math.max(damageEnd, parititionDamageEnd);

        damage = end == -1 ? r : new Region(r.getOffset(), end - r.getOffset());
      }

    } catch (BadLocationException x) {
      /* ignored in platform PresentationReconciler, too */
    }

    return damage;
  }

  /**
   * Returns the end offset of the damage. If a partition has been split by the given document event
   * also the second half of the original partition must be considered. This is achieved by using
   * the remembered partition range.
   * 
   * @param e the event describing the change
   * @return the damage end offset (excluding)
   * @exception BadLocationException if method accesses invalid offset
   */
  int getDamageEndOffset(DocumentEvent e) throws BadLocationException {

    IDocument d = e.getDocument();

    int length = 0;
    if (e.getText() != null) {
      length = e.getText().length();
      if (length > 0)
        --length;
    }

    ITypedRegion partition = getPartition(d, e.getOffset() + length);
    int endOffset = partition.getOffset() + partition.getLength();
    if (endOffset == e.getOffset())
      return -1;

    int end = fRememberedPosition == null ? -1 : fRememberedPosition.getOffset()
        + fRememberedPosition.getLength();
    if (endOffset < end && end < d.getLength())
      partition = getPartition(d, end);

    //if there is not damager for the partition then use the endOffset of the partition
    IPresentationDamager damager = getDamager(partition.getType());
    if (damager != null) {
      IRegion r = damager.getDamageRegion(partition, e, fDocumentPartitioningChanged);
      endOffset = r.getOffset() + r.getLength();
    }

    return endOffset;
  }

  void processRecordedDamages() {
    processRecordedDamages(null, null);
  }

  boolean processRecordedDamages(IRegion damage, IDocument document) {
    RecordedDamage[] recordings = null;
    boolean recordingOverlaps = false;
    synchronized (fRecordedDamages) {
      recordings = (RecordedDamage[]) fRecordedDamages.toArray(new RecordedDamage[fRecordedDamages.size()]);
      fRecordedDamages.clear();
    }
    for (int i = 0; i < recordings.length; i++) {
      if (isOverlappingRegion(damage, recordings[i].damage) && document == recordings[i].document)
        recordingOverlaps = true;
      processDamage(recordings[i].damage, recordings[i].document);
    }
    return recordingOverlaps;
  }

  private boolean isOverlappingRegion(IRegion base, IRegion damage) {
    if (base == null || damage == null)
      return false;

    int baseEnd = base.getOffset() + base.getLength();
    int damageEnd = damage.getOffset() + damage.getLength();

    return damage.getOffset() <= base.getOffset() && (damageEnd >= baseEnd);
  }

  /**
   * Processes the given damage.
   * 
   * @param damage the damage to be repaired
   * @param document the document whose presentation must be repaired
   */
  void processDamage(IRegion damage, IDocument document) {
    if (damage != null && damage.getLength() > 0) {
      TextPresentation p = createPresentation(damage, document);
      if (p != null) {
        /**
         * 229749 - Read-Only highlighting support missing 272981 - Read-only highlighting moved to
         * semantic highlighting
         */
        applyTextRegionCollection(p);
      }
    }
  }

  /**
   * Processes the given damage.
   * 
   * @param damage the damage to be repaired
   * @param document the document whose presentation must be repaired
   */
  void recordDamage(IRegion damage, IDocument document) {
    if (damage != null && damage.getLength() > 0) {
      synchronized (fRecordedDamages) {
        fRecordedDamages.add(new RecordedDamage(damage, document));
      }
    }
  }

  /**
   * Applies the given text presentation to the text viewer the presentation reconciler is installed
   * on.
   * 
   * @param presentation the text presentation to be applied to the text viewer
   */
  void applyTextRegionCollection(TextPresentation presentation) {
    fViewer.changeTextPresentation(presentation, false);
  }

  /**
   * Returns the partition for the given offset in the given document.
   * 
   * @param document the document
   * @param offset the offset
   * @return the partition
   * @throws BadLocationException if offset is invalid in the given document
   * @since 3.0
   */
  ITypedRegion getPartition(IDocument document, int offset) throws BadLocationException {
    return TextUtilities.getPartition(document, getDocumentPartitioning(), offset, false);
  }

  /**
   * Constructs a "repair description" for the given damage and returns this description as a text
   * presentation, essentially making {@link #createPresentation(IRegion, IDocument)} publicly
   * callable.
   * <p>
   * NOTE: Should not be used if this reconciler is installed on a viewer. This method is considered
   * EXPERIMENTAL and may not be available in subsequent versions.
   * </p>
   * 
   * @param damage the damage to be repaired
   * @param document the document whose presentation must be repaired
   * @return the presentation repair description as text presentation
   */
  public TextPresentation createRepairDescription(IRegion damage, IDocument document) {
    if (document != fLastDocument) {
      setDocumentToDamagers(document);
      setDocumentToRepairers(document);
      fLastDocument = document;
    }
    return createPresentation(damage, document);
  }

}
