package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.ContentCache;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.analysis.model.ResourceMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Instances of {@code SimpleResourceMapImpl} provide basic mapping between sources and resources,
 * but do NOT properly map canonical package sources.
 */
public class SimpleResourceMapImpl implements ResourceMap {

  /**
   * The root resource containing all resources in the map (not {@code null}).
   */
  protected final IContainer container;

  /**
   * The root resource location (not {@code null}).
   */
  private final IPath containerLocation;

  /**
   * THe analysis context containing all sources in the map (not {@code null}).
   */
  protected final AnalysisContext context;

  /**
   * The common cache used when constructing sources (not {@code null}).
   */
  protected final ContentCache contentCache;

  public SimpleResourceMapImpl(IContainer container, AnalysisContext context) {
    this.container = container;
    this.containerLocation = container.getLocation();
    this.context = context;
    this.contentCache = context.getSourceFactory().getContentCache();
  }

  @Override
  public AnalysisContext getContext() {
    return context;
  }

  @Override
  public IContainer getResource() {
    return container;
  }

  @Override
  public IFile getResource(Source source) {
    if (source == null) {
      return null;
    }
    IPath path = new Path(source.getFullName());
    if (containerLocation.isPrefixOf(path)) {
      IPath relPath = path.removeFirstSegments(containerLocation.segmentCount());
      return container.getFile(relPath);
    }
    // TODO (danrubel): Handle mapped subfolders
    return null;
  }

  @Override
  public Source getSource(IFile resource) {
    if (resource == null) {
      return null;
    }
    IPath location = resource.getLocation();
    if (location == null) {
      return null;
    }
    return new FileBasedSource(contentCache, location.toFile());
  }
}
