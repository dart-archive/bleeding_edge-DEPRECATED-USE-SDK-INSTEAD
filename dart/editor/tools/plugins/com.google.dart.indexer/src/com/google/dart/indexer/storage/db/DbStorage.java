/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.storage.db;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;
import com.google.dart.indexer.index.entries.DependentEntity;
import com.google.dart.indexer.index.entries.DependentFileInfo;
import com.google.dart.indexer.index.entries.DependentLocation;
import com.google.dart.indexer.index.entries.FileInfo;
import com.google.dart.indexer.index.entries.LocationInfo;
import com.google.dart.indexer.index.entries.PathAndModStamp;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.layers.LayerId;
import com.google.dart.indexer.index.layers.bidirectional_edges.BidirectionalEdgesLayer;
import com.google.dart.indexer.index.layers.bidirectional_edges.BidirectionalEdgesLocationInfo;
import com.google.dart.indexer.index.layers.reverse_edges.ReverseEdgesLayer;
import com.google.dart.indexer.index.layers.reverse_edges.ReverseEdgesLocationInfo;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.locations.LocationPersitence;
import com.google.dart.indexer.storage.AbstractIntegratedStorage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DbStorage extends AbstractIntegratedStorage {
  class LocationData {
    final int id;
    final String handle;

    public LocationData(int id, String handle) {
      this.id = id;
      this.handle = handle;
    }
  }

  private static IFile fromPortableString(String string) {
    return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(string));
  }

  private static String getPortableString(IFile file) {
    return file.getFullPath().toString();
  }

  private Connection connection;
  private PreparedStatement selectLocationStatement;
  private PreparedStatement insertLocationStatement;
  private PreparedStatement selectSourceHandlesStatement;
  private PreparedStatement insertConnectionStatement;
  private PreparedStatement deleteConnectionsStatement;
  private PreparedStatement selectLayerStatement;
  private PreparedStatement insertLayerStatement;
  private PreparedStatement selectAllLocationsStatement;
  private PreparedStatement deleteConnectionsByLocationStatement;
  private PreparedStatement deleteLocationByIdStatement;
  private PreparedStatement selectDestinationHandlesStatement;
  private PreparedStatement findFileByNameStatement;
  private PreparedStatement insertFileStatement;
  private PreparedStatement deleteFileSourceLocationsStatement;
  private PreparedStatement deleteFileDependentLocationsStatement;
  private PreparedStatement deleteFileDependentFilesStatement;
  private PreparedStatement insertFileSourceLocationStatement;
  private PreparedStatement insertFileDependentLocationStatement;
  private PreparedStatement insertFileDependentFileStatement;
  private PreparedStatement deleteFileByIdStatement;
  private PreparedStatement selectFileSourceLocations;

  private PreparedStatement selectFileDependentLocations;

  private PreparedStatement selectFileDependentFiles;

  private PreparedStatement selectAllFilesStatement;

  public DbStorage(IndexConfigurationInstance configuration) {
    super(configuration);
    try {
      Class.forName("org.h2.Driver");
      // connection = DriverManager.getConnection("jdbc:h2:mem:", "sa", "");
      connection = DriverManager.getConnection("jdbc:h2:~/indexerdba", "sa", "");
      Statement stat = connection.createStatement();

      // stat.executeUpdate("DROP TABLE IF EXISTS locations");
      // stat.executeUpdate("DROP TABLE IF EXISTS layers");
      // stat.executeUpdate("DROP TABLE IF EXISTS files");
      // stat.executeUpdate("DROP TABLE IF EXISTS connections");
      // stat.executeUpdate("DROP TABLE IF EXISTS file_source_locations");
      // stat.executeUpdate("DROP TABLE IF EXISTS file_dependent_locations");
      // stat.executeUpdate("DROP TABLE IF EXISTS file_dependent_files");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS locations(id int auto_increment primary key, handle varchar not null)");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS layers(id int auto_increment primary key, name varchar not null)");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS files(id int auto_increment primary key, name varchar not null, mod_stamp bigint not null)");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS connections(src_id int not null, dst_id int not null, layer_id int not null, primary key(src_id, dst_id, layer_id))");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS file_source_locations(file_id int not null, location_id int not null, primary key(file_id, location_id))");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS file_dependent_locations(file_id int not null, location_id int not null, layer_id int not null, internal bool, primary key(file_id, location_id, layer_id, internal))");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS file_dependent_files(file_id int not null, dependent_file_id int not null, internal bool, primary key(file_id, dependent_file_id, internal))");
      selectAllLocationsStatement = connection.prepareStatement("SELECT id, handle FROM locations");
      selectLocationStatement = connection.prepareStatement("SELECT id FROM locations WHERE handle = ?");
      insertLocationStatement = connection.prepareStatement(
          "INSERT INTO locations(handle) VALUES(?)", Statement.RETURN_GENERATED_KEYS);
      selectLayerStatement = connection.prepareStatement("SELECT id FROM layers WHERE name = ?");
      insertLayerStatement = connection.prepareStatement("INSERT INTO layers(name) VALUES(?)",
          Statement.RETURN_GENERATED_KEYS);
      selectSourceHandlesStatement = connection.prepareStatement("SELECT handle FROM connections INNER JOIN locations ON connections.src_id = locations.id WHERE dst_id = ? and layer_id = ?");
      selectDestinationHandlesStatement = connection.prepareStatement("SELECT handle FROM connections INNER JOIN locations ON connections.dst_id = locations.id WHERE src_id = ? and layer_id = ?");
      insertConnectionStatement = connection.prepareStatement("INSERT INTO connections(src_id, dst_id, layer_id) VALUES(?, ?, ?)");
      deleteConnectionsStatement = connection.prepareStatement("DELETE FROM connections WHERE dst_id = ? AND layer_id = ?");
      deleteConnectionsByLocationStatement = connection.prepareStatement("DELETE FROM connections WHERE src_id = ? OR dst_id = ?");
      deleteLocationByIdStatement = connection.prepareStatement("DELETE FROM locations WHERE id = ?");
      findFileByNameStatement = connection.prepareStatement("SELECT id FROM files WHERE name = ?");
      insertFileStatement = connection.prepareStatement("INSERT INTO files(name, mod_stamp) VALUES (?, ?)");
      deleteFileSourceLocationsStatement = connection.prepareStatement("DELETE FROM file_source_locations WHERE file_id = ?");
      deleteFileDependentLocationsStatement = connection.prepareStatement("DELETE FROM file_dependent_locations WHERE file_id = ?");
      deleteFileDependentFilesStatement = connection.prepareStatement("DELETE FROM file_dependent_files WHERE file_id = ?");
      insertFileSourceLocationStatement = connection.prepareStatement("INSERT INTO file_source_locations(file_id, location_id) VALUES (?, ?)");
      insertFileDependentLocationStatement = connection.prepareStatement("INSERT INTO file_dependent_locations(file_id, location_id, layer_id, internal) VALUES (?, ?, ?, ?)");
      insertFileDependentFileStatement = connection.prepareStatement("INSERT INTO file_dependent_files(file_id, dependent_file_id, internal) VALUES (?, ?, ?)");
      deleteFileByIdStatement = connection.prepareStatement("DELETE FROM files WHERE id = ?");
      selectFileSourceLocations = connection.prepareStatement("SELECT handle FROM locations INNER JOIN file_source_locations ON locations.id=file_source_locations.location_id WHERE file_id = ?");
      selectFileDependentLocations = connection.prepareStatement("SELECT handle, layers.name, internal FROM locations INNER JOIN file_dependent_locations ON locations.id=file_dependent_locations.location_id INNER JOIN layers ON layers.id=file_dependent_locations.layer_id WHERE file_id = ?");
      selectFileDependentFiles = connection.prepareStatement("SELECT name, internal FROM files INNER JOIN file_dependent_files ON files.id=file_dependent_files.dependent_file_id WHERE file_id = ?");
      selectAllFilesStatement = connection.prepareStatement("SELECT id, name, mod_stamp FROM files");
    } catch (ClassNotFoundException exception) {
      IndexerPlugin.getLogger().logError(exception);
    } catch (SQLException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public void close() {
    try {
      connection.close();
    } catch (SQLException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public void deleteFileInfo(IFile file) {
    String name = getPortableString(file);
    try {
      int fileId = findFile(name);
      if (fileId == -1) {
        return;
      }

      deleteFileSourceLocationsStatement.setInt(1, fileId);
      deleteFileSourceLocationsStatement.executeUpdate();

      deleteFileDependentLocationsStatement.setInt(1, fileId);
      deleteFileDependentLocationsStatement.executeUpdate();

      deleteFileDependentFilesStatement.setInt(1, fileId);
      deleteFileDependentFilesStatement.executeUpdate();

      deleteFileByIdStatement.setInt(1, fileId);
      deleteFileByIdStatement.executeUpdate();
    } catch (SQLException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public void deleteLocationInfo(Location location) {
    String handleIdentifier = LocationPersitence.getInstance().getUniqueIdentifier(location);
    try {
      int id = findLocation(handleIdentifier);
      if (id == -1) {
        return;
      }

      deleteConnectionsByLocationStatement.setInt(1, id);
      deleteConnectionsByLocationStatement.setInt(2, id);
      deleteConnectionsByLocationStatement.executeUpdate();

      deleteLocationByIdStatement.setInt(1, id);
      deleteLocationByIdStatement.executeUpdate();
    } catch (SQLException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }

  }

  @Override
  public Map<IFile, FileInfo> readAllFileInfos(IndexConfigurationInstance instance) {
    HashMap<IFile, FileInfo> result = new HashMap<IFile, FileInfo>();
    try {
      ResultSet resultSet = selectAllFilesStatement.executeQuery();
      while (resultSet.next()) {
        int id = resultSet.getInt(1);
        String name = resultSet.getString(2);
        IFile file = fromPortableString(name);
        FileInfo info = doReadFileInfo(instance, id);
        if (file != null && info != null) {
          result.put(file, info);
        }
      }
      return result;
    } catch (SQLException exception) {
      IndexerPlugin.getLogger().logError(exception);
      return result;
    }
  }

  @Override
  public void readAllLayerLocationsInto(Map<Location, LocationInfo> locationInfos, Layer layer) {
    try {
      int layerId = lookupLayer(layer.getId().stringValue());

      Collection<LocationData> ids = new ArrayList<LocationData>();
      ResultSet resultSet = selectAllLocationsStatement.executeQuery();
      while (resultSet.next()) {
        ids.add(new LocationData(resultSet.getInt(1), resultSet.getString(2)));
      }
      resultSet.close();

      for (Iterator<LocationData> iterator = ids.iterator(); iterator.hasNext();) {
        LocationData data = iterator.next();
        LocationInfo info = doReadLocation(layerId, data.id, layer);
        Location location = LocationPersitence.getInstance().byUniqueIdentifier(data.handle);
        if (location != null && !info.isEmpty()) {
          locationInfos.put(location, info);
        }
      }
    } catch (SQLException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public FileInfo readFileInfo(IFile file) {
    String name = getPortableString(file);
    try {
      int fileId = findFile(name);
      if (fileId == -1) {
        return null;
      }
      return doReadFileInfo(configuration, fileId);
    } catch (SQLException exception) {
      IndexerPlugin.getLogger().logError(exception);
      return null;
    }
  }

  @Override
  public PathAndModStamp[] readFileNamesAndStamps(HashSet<IFile> unprocessedExistingFiles) {
    Collection<PathAndModStamp> result = new ArrayList<PathAndModStamp>();
    try {
      ResultSet resultSet = selectAllFilesStatement.executeQuery();
      while (resultSet.next()) {
        String name = resultSet.getString(2);
        long modStamp = resultSet.getLong(3);
        result.add(new PathAndModStamp(name, modStamp));
      }
      return result.toArray(new PathAndModStamp[result.size()]);
    } catch (SQLException exception) {
      IndexerPlugin.getLogger().logError(exception);
      return new PathAndModStamp[0];
    }
  }

  @Override
  public LocationInfo readLocationInfo(Location location, Layer layer) {
    if (location == null) {
      throw new NullPointerException("location is null");
    }
    if (layer == null) {
      throw new NullPointerException("layerId is null");
    }
    String identifier = LocationPersitence.getInstance().getUniqueIdentifier(location);
    try {
      int layerId = lookupLayer(layer.getId().stringValue());
      int id = findLocation(identifier);
      if (id == -1) {
        return null;
      }
      return doReadLocation(layerId, id, layer);
    } catch (SQLException exception) {
      IndexerPlugin.getLogger().logError(exception);
      return null;
    }
  }

  @Override
  public void writeFileInfo(IFile file, FileInfo info) {
    String name = getPortableString(file);
    try {
      int fileId = lookupFile(name, file.getModificationStamp());

      deleteFileSourceLocationsStatement.setInt(1, fileId);
      deleteFileSourceLocationsStatement.executeUpdate();

      deleteFileDependentLocationsStatement.setInt(1, fileId);
      deleteFileDependentLocationsStatement.executeUpdate();

      deleteFileDependentFilesStatement.setInt(1, fileId);
      deleteFileDependentFilesStatement.executeUpdate();

      Collection<Location> sourceLocations = info.getSourceLocations();
      for (Iterator<Location> iterator = sourceLocations.iterator(); iterator.hasNext();) {
        Location location = iterator.next();
        int locationId = lookupLocation(LocationPersitence.getInstance().getUniqueIdentifier(
            location));
        insertFileSourceLocationStatement.setInt(1, fileId);
        insertFileSourceLocationStatement.setInt(2, locationId);
        insertFileSourceLocationStatement.executeUpdate();
      }

      insertDependencies(fileId, info.getInternalDependencies(), true);
      insertDependencies(fileId, info.getExternalDependencies(), false);
    } catch (SQLException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public void writeLocationInfo(Location location, LocationInfo info, Layer layerObj) {
    if (location == null) {
      throw new NullPointerException("location is null");
    }
    if (info == null) {
      throw new NullPointerException("info is null");
    }
    if (layerObj == null) {
      throw new NullPointerException("layerId is null");
    }
    String destinationHandleIdentifier = LocationPersitence.getInstance().getUniqueIdentifier(
        location);
    try {
      int destinationId = lookupLocation(destinationHandleIdentifier);
      int layer = lookupLayer(layerObj.getId().stringValue());

      deleteConnectionsStatement.setInt(1, destinationId);
      deleteConnectionsStatement.setInt(2, layer);
      deleteConnectionsStatement.executeUpdate();

      Location[] sourceLocations;
      if (info instanceof BidirectionalEdgesLocationInfo) {
        BidirectionalEdgesLocationInfo bidiInfo = (BidirectionalEdgesLocationInfo) info;
        sourceLocations = bidiInfo.getSourceLocations();
      } else if (info instanceof ReverseEdgesLocationInfo) {
        ReverseEdgesLocationInfo revInfo = (ReverseEdgesLocationInfo) info;
        sourceLocations = revInfo.getSourceLocations();
      } else {
        throw new AssertionError("Unknown kind of location: " + location.getClass().getName());
      }

      Set<Integer> usedSourceIds = new HashSet<Integer>();
      for (int i = 0; i < sourceLocations.length; i++) {
        Location sourceLocation = sourceLocations[i];
        String sourceHandleIdentifier = LocationPersitence.getInstance().getUniqueIdentifier(
            sourceLocation);
        int sourceId = lookupLocation(sourceHandleIdentifier);
        if (usedSourceIds.add(new Integer(sourceId))) {
          insertConnectionStatement.setInt(1, sourceId);
          insertConnectionStatement.setInt(2, destinationId);
          insertConnectionStatement.setInt(3, layer);
          insertConnectionStatement.executeUpdate();
        }
      }
    } catch (SQLException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }

  }

  int findFile(String name) throws SQLException {
    int result = -1;
    findFileByNameStatement.setString(1, name);
    ResultSet resultSet = findFileByNameStatement.executeQuery();
    if (resultSet.next()) {
      result = resultSet.getInt(1);
    }
    resultSet.close();
    return result;
  }

  int findLayer(String layerId) throws SQLException {
    int result = -1;
    selectLayerStatement.setString(1, layerId);
    ResultSet resultSet = selectLayerStatement.executeQuery();
    if (resultSet.next()) {
      result = resultSet.getInt(1);
    }
    resultSet.close();
    return result;
  }

  int findLocation(String handle) throws SQLException {
    int result = -1;
    selectLocationStatement.setString(1, handle);
    ResultSet resultSet = selectLocationStatement.executeQuery();
    if (resultSet.next()) {
      result = resultSet.getInt(1);
    }
    resultSet.close();
    return result;
  }

  int lookupFile(String name, long modStamp) throws SQLException {
    int result = findLayer(name);
    if (result == -1) {
      insertFileStatement.setString(1, name);
      insertFileStatement.setLong(2, modStamp);
      if (insertFileStatement.executeUpdate() != 1) {
        throw new AssertionError("INSERT failed for layer " + name);
      }
      ResultSet resultSet = insertFileStatement.getGeneratedKeys();
      if (!resultSet.next()) {
        throw new AssertionError("Failed to retrieve generated keys for file");
      }
      result = resultSet.getInt(1);
      resultSet.close();
    }
    return result;
  }

  int lookupLayer(String layerId) throws SQLException {
    int result = findLayer(layerId);
    if (result == -1) {
      insertLayerStatement.setString(1, layerId);
      if (insertLayerStatement.executeUpdate() != 1) {
        throw new AssertionError("INSERT failed for layer " + layerId);
      }
      ResultSet resultSet = insertLayerStatement.getGeneratedKeys();
      if (!resultSet.next()) {
        throw new AssertionError("Failed to retrieve generated keys for layer");
      }
      result = resultSet.getInt(1);
      resultSet.close();
    }
    return result;
  }

  int lookupLocation(String handle) throws SQLException {
    int result = findLocation(handle);
    if (result == -1) {
      insertLocationStatement.setString(1, handle);
      if (insertLocationStatement.executeUpdate() != 1) {
        throw new AssertionError("INSERT failed for location " + handle);
      }
      ResultSet resultSet = insertLocationStatement.getGeneratedKeys();
      if (!resultSet.next()) {
        throw new AssertionError("Failed to retrieve generated keys for location");
      }
      result = resultSet.getInt(1);
      resultSet.close();
    }
    return result;
  }

  private FileInfo doReadFileInfo(IndexConfigurationInstance instance, int fileId)
      throws SQLException {
    Collection<Location> sourceLocations = new ArrayList<Location>();
    Collection<DependentEntity> internalDependencies = new ArrayList<DependentEntity>();
    Collection<DependentEntity> externalDependencies = new ArrayList<DependentEntity>();

    loadSourceLocations(fileId, sourceLocations);
    loadDependentLocations(instance, fileId, internalDependencies, externalDependencies);
    loadDependentFiles(fileId, internalDependencies, externalDependencies);

    return new FileInfo(sourceLocations, internalDependencies, externalDependencies);
  }

  private LocationInfo doReadLocation(int layerId, int id, Layer layer) throws SQLException {
    Collection<Location> sourceLocations = new ArrayList<Location>();
    {
      selectSourceHandlesStatement.setInt(1, id);
      selectSourceHandlesStatement.setInt(2, layerId);
      ResultSet resultSet = selectSourceHandlesStatement.executeQuery();
      while (resultSet.next()) {
        String handleIdentifier = resultSet.getString(1);
        Location sourceLocation = LocationPersitence.getInstance().byUniqueIdentifier(
            handleIdentifier);
        if (sourceLocation != null) {
          sourceLocations.add(sourceLocation);
        }
      }
    }
    if (layer instanceof ReverseEdgesLayer) {
      return new ReverseEdgesLocationInfo(sourceLocations);
    } else if (layer instanceof BidirectionalEdgesLayer) {
      selectDestinationHandlesStatement.setInt(1, id);
      selectDestinationHandlesStatement.setInt(2, layerId);
      ResultSet resultSet = selectDestinationHandlesStatement.executeQuery();
      Collection<Location> destinationLocations = new ArrayList<Location>();
      while (resultSet.next()) {
        String handleIdentifier = resultSet.getString(1);
        Location destinationLocation = LocationPersitence.getInstance().byUniqueIdentifier(
            handleIdentifier);
        if (destinationLocation != null) {
          destinationLocations.add(destinationLocation);
        }
      }
      return new BidirectionalEdgesLocationInfo(sourceLocations, destinationLocations);
    } else {
      throw new AssertionError("Unsupported kind of layer");
    }
  }

  private void insertDependencies(int fileId, Collection<DependentEntity> dependencies,
      boolean internal) throws SQLException, AssertionError {
    for (Iterator<DependentEntity> iterator = dependencies.iterator(); iterator.hasNext();) {
      DependentEntity dependentEntity = iterator.next();
      if (dependentEntity instanceof DependentLocation) {
        DependentLocation dependentLocation = (DependentLocation) dependentEntity;
        int locationId = lookupLocation(LocationPersitence.getInstance().getUniqueIdentifier(
            dependentLocation.getDependentLocation()));
        int layerId = lookupLayer(dependentLocation.getDependentLayer().getId().stringValue());
        insertFileDependentLocationStatement.setInt(1, fileId);
        insertFileDependentLocationStatement.setInt(2, locationId);
        insertFileDependentLocationStatement.setInt(3, layerId);
        insertFileDependentLocationStatement.setBoolean(4, internal);
        insertFileDependentLocationStatement.executeUpdate();
      } else if (dependentEntity instanceof DependentFileInfo) {
        DependentFileInfo dependentFileInfo = (DependentFileInfo) dependentEntity;
        IFile dependentFile = dependentFileInfo.getFile();
        int dependentFileId = lookupFile(getPortableString(dependentFile),
            dependentFile.getModificationStamp());
        insertFileDependentFileStatement.setInt(1, fileId);
        insertFileDependentFileStatement.setInt(2, dependentFileId);
        insertFileDependentFileStatement.setBoolean(3, internal);
        insertFileDependentFileStatement.executeUpdate();
      } else {
        throw new AssertionError("Unexpected kind of dependent entity");
      }
    }
  }

  private void loadDependentFiles(int fileId, Collection<DependentEntity> internalDependencies,
      Collection<DependentEntity> externalDependencies) throws SQLException {
    selectFileDependentFiles.setInt(1, fileId);
    ResultSet resultSet = selectFileDependentFiles.executeQuery();
    while (resultSet.next()) {
      String depName = resultSet.getString(1);
      IFile depFile = fromPortableString(depName);
      boolean internal = resultSet.getBoolean(2);
      (internal ? internalDependencies : externalDependencies).add(new DependentFileInfo(depFile));
    }
  }

  private void loadDependentLocations(IndexConfigurationInstance instance, int fileId,
      Collection<DependentEntity> internalDependencies,
      Collection<DependentEntity> externalDependencies) throws SQLException {
    selectFileDependentLocations.setInt(1, fileId);
    ResultSet resultSet = selectFileDependentLocations.executeQuery();
    while (resultSet.next()) {
      String uniqueIdentifier = resultSet.getString(1);
      Location location = LocationPersitence.getInstance().byUniqueIdentifier(uniqueIdentifier);
      String layerId = resultSet.getString(2);
      boolean internal = resultSet.getBoolean(3);
      Layer layer = instance.getLayer(new LayerId(layerId));
      (internal ? internalDependencies : externalDependencies).add(new DependentLocation(location,
          layer));
    }
  }

  private void loadSourceLocations(int fileId, Collection<Location> sourceLocations)
      throws SQLException {
    selectFileSourceLocations.setInt(1, fileId);
    ResultSet resultSet = selectFileSourceLocations.executeQuery();
    while (resultSet.next()) {
      String uniqueIdentifier = resultSet.getString(1);
      Location location = LocationPersitence.getInstance().byUniqueIdentifier(uniqueIdentifier);
      sourceLocations.add(location);
    }
  }
}
