package com.google.dart.tools.wst.ui.style;

import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.internal.text.dart.DartCodeScanner;
import com.google.dart.tools.ui.internal.text.dartdoc.DartDocScanner;
import com.google.dart.tools.ui.internal.text.functions.DartCommentScanner;
import com.google.dart.tools.ui.internal.text.functions.DartMultilineStringScanner;
import com.google.dart.tools.ui.internal.text.functions.SingleTokenDartScanner;
import com.google.dart.tools.ui.text.DartIndiscriminateDamager;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.IColorManager;
import com.google.dart.tools.ui.text.IDartColorConstants;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.swt.custom.StyleRange;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Analyze a document to produce syntactic highlighting ranges. Adapted from PresentationReconciler.
 */
public class SyntacticHighlightingEngine {

  private static class PositionCollector {
    private Collection<StyleRange> positions;
    private String partition;
    private Map<String, IPresentationDamager> damagers;
    private Map<String, IPresentationRepairer> repairers;

    public PositionCollector(Collection<StyleRange> positions) {
      this.positions = positions;
    }

    public IPresentationRepairer getRepairer(String contentType) {
      if (repairers == null) {
        return null;
      }
      return repairers.get(contentType);
    }

    public void setDamager(IPresentationDamager damager, String contentType) {
      if (damagers == null) {
        damagers = new HashMap<String, IPresentationDamager>();
      }
      if (damager == null) {
        damagers.remove(contentType);
      } else {
        damagers.put(contentType, damager);
      }
    }

    public void setDocumentPartitioning(String partitioning) {
      partition = partitioning;
    }

    public void setRepairer(IPresentationRepairer repairer, String contentType) {
      if (repairers == null) {
        repairers = new HashMap<String, IPresentationRepairer>();
      }
      if (repairer == null) {
        repairers.remove(contentType);
      } else {
        repairers.put(contentType, repairer);
      }
    }

    private TextPresentation createPresentation(IRegion damage, IDocument document) {
      try {
        if (repairers == null || repairers.isEmpty()) {
          TextPresentation presentation = new TextPresentation(damage, 100);
          presentation.setDefaultStyleRange(new StyleRange(
              damage.getOffset(),
              damage.getLength(),
              null,
              null));
          return presentation;
        }
        TextPresentation presentation = new TextPresentation(damage, 1000);
        ITypedRegion[] partitioning = TextUtilities.computePartitioning(
            document,
            partition,
            damage.getOffset(),
            damage.getLength(),
            false);
        for (int i = 0; i < partitioning.length; i++) {
          ITypedRegion r = partitioning[i];
          IPresentationRepairer repairer = getRepairer(r.getType());
          if (repairer != null) {
            repairer.createPresentation(presentation, r);
          }
        }
        return presentation;
      } catch (BadLocationException x) {
        return null;
      }
    }

    private void processRegion(IRegion damage, IDocument document) {
      setDocumentToDamagers(document);
      setDocumentToRepairers(document);
      if (damage != null && damage.getLength() > 0) {
        TextPresentation p = createPresentation(damage, document);
        @SuppressWarnings("unchecked")
        List<StyleRange> styles = (List<StyleRange>) ReflectionUtils.getFieldObject(p, "fRanges");
        for (StyleRange range : styles) {
          positions.add(range);
        }
      }
    }

    private void setDocumentToDamagers(IDocument document) {
      if (damagers != null) {
        Iterator<IPresentationDamager> e = damagers.values().iterator();
        while (e.hasNext()) {
          IPresentationDamager damager = e.next();
          damager.setDocument(document);
        }
      }
    }

    private void setDocumentToRepairers(IDocument document) {
      if (repairers != null) {
        Iterator<IPresentationRepairer> e = repairers.values().iterator();
        while (e.hasNext()) {
          IPresentationRepairer repairer = e.next();
          repairer.setDocument(document);
        }
      }
    }

  }

  private IPreferenceStore preferenceStore;
  private IColorManager colorManager;
  private DartCodeScanner codeScanner;
  private DartCommentScanner multilineCommentScanner;
  private DartCommentScanner singlelineCommentScanner;
  private SingleTokenDartScanner stringScanner;
  private DartMultilineStringScanner multilineStringScanner;
  private DartDocScanner docCommentScanner;

  public SyntacticHighlightingEngine(IColorManager colorManager, IPreferenceStore preferenceStore) {
    this.colorManager = colorManager;
    this.preferenceStore = preferenceStore;
    initializeScanners();
  }

  public void analyze(IDocument document, IRegion region, Collection<StyleRange> positions) {
    PositionCollector collector = getPositionCollector(positions);
    collector.processRegion(region, document);
  }

  private PositionCollector getPositionCollector(Collection<StyleRange> positions) {

    PositionCollector reconciler = new PositionCollector(positions);
    reconciler.setDocumentPartitioning(DartPartitions.DART_PARTITIONING);

    DefaultDamagerRepairer dr = new DefaultDamagerRepairer(codeScanner);
    reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
    reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

    dr = new DefaultDamagerRepairer(docCommentScanner);
    reconciler.setDamager(new DartIndiscriminateDamager(), DartPartitions.DART_DOC);
    reconciler.setRepairer(dr, DartPartitions.DART_DOC);
    reconciler.setDamager(new DartIndiscriminateDamager(), DartPartitions.DART_SINGLE_LINE_DOC);
    reconciler.setRepairer(dr, DartPartitions.DART_SINGLE_LINE_DOC);

    dr = new DefaultDamagerRepairer(multilineCommentScanner);
    reconciler.setDamager(dr, DartPartitions.DART_MULTI_LINE_COMMENT);
    reconciler.setRepairer(dr, DartPartitions.DART_MULTI_LINE_COMMENT);

    dr = new DefaultDamagerRepairer(singlelineCommentScanner);
    reconciler.setDamager(dr, DartPartitions.DART_SINGLE_LINE_COMMENT);
    reconciler.setRepairer(dr, DartPartitions.DART_SINGLE_LINE_COMMENT);

    dr = new DefaultDamagerRepairer(stringScanner);
    reconciler.setDamager(dr, DartPartitions.DART_STRING);
    reconciler.setRepairer(dr, DartPartitions.DART_STRING);

    dr = new DefaultDamagerRepairer(multilineStringScanner);
    reconciler.setDamager(dr, DartPartitions.DART_MULTI_LINE_STRING);
    reconciler.setRepairer(dr, DartPartitions.DART_MULTI_LINE_STRING);

    return reconciler;
  }

  private void initializeScanners() {
    codeScanner = new DartCodeScanner(colorManager, preferenceStore);
    multilineCommentScanner = new DartCommentScanner(
        colorManager,
        preferenceStore,
        IDartColorConstants.JAVA_MULTI_LINE_COMMENT);
    singlelineCommentScanner = new DartCommentScanner(
        colorManager,
        preferenceStore,
        IDartColorConstants.JAVA_SINGLE_LINE_COMMENT);
    stringScanner = new SingleTokenDartScanner(
        colorManager,
        preferenceStore,
        IDartColorConstants.JAVA_STRING);
    multilineStringScanner = new DartMultilineStringScanner(
        colorManager,
        preferenceStore,
        IDartColorConstants.DART_MULTI_LINE_STRING);
    docCommentScanner = new DartDocScanner(colorManager, preferenceStore);
  }

}
