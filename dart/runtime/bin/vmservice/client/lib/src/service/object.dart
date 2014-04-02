// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of service;

/// A [ServiceObject] is an object known to the VM service and is tied
/// to an owning [Isolate].
abstract class ServiceObject extends Observable {
  /// The owner of this [ServiceObject].  This can be an [Isolate], a
  /// [VM], or null.
  @reflectable ServiceObjectOwner get owner => _owner;
  ServiceObjectOwner _owner;

  /// The [VM] which owns this [ServiceObject].
  @reflectable VM get vm => _owner.vm;

  /// The [Isolate] which owns this [ServiceObject].  May be null.
  @reflectable Isolate get isolate => _owner.isolate;

  /// The id of this object.
  @reflectable String get id => _id;
  String _id;

  /// The service type of this object.
  @reflectable String get serviceType => _serviceType;
  String _serviceType;

  /// The complete service url of this object.
  @reflectable String get link => isolate.relativeLink(_id);

  /// The complete service url of this object with a '#/' prefix.
  @reflectable String get hashLink => '#/${link}';
  set hashLink(var o) { /* silence polymer */ }

  /// Has this object been fully loaded?
  bool get loaded => _loaded;
  bool _loaded = false;

  /// Is this object cacheable?  That is, is it impossible for the [id]
  /// of this object to change?
  bool get canCache => false;

  /// Is this object immutable after it is [loaded]?
  bool get immutable => false;

  @observable String name;
  @observable String vmName;

  /// Creates an empty [ServiceObject].
  ServiceObject._empty(this._owner);

  /// Creates a [ServiceObject] initialized from [map].
  factory ServiceObject._fromMap(ServiceObjectOwner owner,
                                 ObservableMap map) {
    if (map == null) {
      return null;
    }
    if (!_isServiceMap(map)) {
      Logger.root.severe('Malformed service object: $map');
    }
    assert(_isServiceMap(map));
    var type = _stripRef(map['type']);
    var obj = null;
    assert(type != 'VM');
    switch (type) {
      case 'Code':
        obj = new Code._empty(owner);
        break;
      case 'Error':
        obj = new DartError._empty(owner);
        break;
      case 'Isolate':
        obj = new Isolate._empty(owner);
        break;
      case 'ServiceError':
        obj = new ServiceError._empty(owner);
        break;
      case 'ServiceException':
        obj = new ServiceException._empty(owner);
        break;
      case 'Script':
        obj = new Script._empty(owner);
        break;
      default:
        obj = new ServiceMap._empty(owner);
    }
    obj.update(map);
    return obj;
  }

  /// If [this] was created from a reference, load the full object
  /// from the service by calling [reload]. Else, return [this].
  Future<ServiceObject> load() {
    if (loaded) {
      return new Future.value(this);
    }
    // Call reload which will fill in the entire object.
    return reload();
  }

  Future<ServiceObject> _inProgressReload;

  /// Reload [this]. Returns a future which completes to [this] or
  /// a [ServiceError].
  Future<ServiceObject> reload() {
    if (id == '') {
      // Errors don't have ids.
      assert(serviceType == 'Error');
      return new Future.value(this);
    }
    if (loaded && immutable) {
      return new Future.value(this);
    }
    if (_inProgressReload == null) {
      _inProgressReload = vm.getAsMap(link).then((ObservableMap map) {
          var mapType = _stripRef(map['type']);
          if (mapType != _serviceType) {
            // If the type changes, return a new object instead of
            // updating the existing one.
            assert(mapType == 'Error' || mapType == 'Null');
            return new ServiceObject._fromMap(owner, map);
          }
          update(map);
          return this;
      }).whenComplete(() {
          // This reload is complete.
          _inProgressReload = null;
      });
    }
    return _inProgressReload;
  }

  /// Update [this] using [map] as a source. [map] can be a reference.
  void update(ObservableMap map) {
    assert(_isServiceMap(map));

    // Don't allow the type to change on an object update.
    // TODO(turnidge): Make this a ServiceError?
    var mapIsRef = _hasRef(map['type']);
    var mapType = _stripRef(map['type']);
    assert(_serviceType == null || _serviceType == mapType);

    if (_id != null && _id != map['id']) {
      // It is only safe to change an id when the object isn't cacheable.
      assert(!canCache);
    }
    _id = map['id'];

    _serviceType = mapType;
    _update(map, mapIsRef);
  }

  // Updates internal state from [map]. [map] can be a reference.
  void _update(ObservableMap map, bool mapIsRef);
}

abstract class ServiceObjectOwner extends ServiceObject {
  /// Creates an empty [ServiceObjectOwner].
  ServiceObjectOwner._empty(ServiceObjectOwner owner) : super._empty(owner);

  /// Builds a [ServiceObject] corresponding to the [id] from [map].
  /// The result may come from the cache.  The result will not necessarily
  /// be [loaded].
  ServiceObject getFromMap(ObservableMap map);
}

/// State for a VM being inspected.
abstract class VM extends ServiceObjectOwner {
  @reflectable VM get vm => this;
  @reflectable Isolate get isolate => null;

  @reflectable Iterable<Isolate> get isolates => _isolateCache.values;

  @reflectable String get link => '$id';

  @observable String version = 'unknown';
  @observable String architecture = 'unknown';
  @observable double uptime = 0.0;

  VM() : super._empty(null) {
    name = 'vm';
    vmName = 'vm';
    _cache['vm'] = this;
    update(toObservable({'id':'vm', 'type':'@VM'}));
  }

  final StreamController<ServiceException> exceptions =
      new StreamController.broadcast();
  final StreamController<ServiceError> errors =
      new StreamController.broadcast();

  static final RegExp _currentIsolateMatcher = new RegExp(r'isolates/\d+');
  static final RegExp _currentObjectMatcher = new RegExp(r'isolates/\d+/');
  static final String _isolatesPrefix = 'isolates/';

  String _parseObjectId(String id) {
    Match m = _currentObjectMatcher.matchAsPrefix(id);
    if (m == null) {
      return null;
    }
    return m.input.substring(m.end);
  }

  String _parseIsolateId(String id) {
    Match m = _currentIsolateMatcher.matchAsPrefix(id);
    if (m == null) {
      return '';
    }
    return id.substring(0, m.end);
  }

  Map<String,ServiceObject> _cache = new Map<String,ServiceObject>();
  Map<String,Isolate> _isolateCache = new Map<String,Isolate>();

  ServiceObject getFromMap(ObservableMap map) {
    throw new UnimplementedError();
  }

  Future<ServiceObject> _getIsolate(String isolateId) {
    if (isolateId == '') {
      return new Future.value(null);
    }
    Isolate isolate = _isolateCache[isolateId];
    if (isolate != null) {
      return new Future.value(isolate);
    }
    // The isolate is not in the cache.  Reload the vm and see if the
    // requested isolate is found.
    return reload().then((result) {
        if (result is! VM) {
          return null;
        }
        assert(result == this);
        return _isolateCache[isolateId];
      });
  }

  Future<ServiceObject> get(String id) {
    var parts = id.split('#');
    assert(parts.length >= 1);
    // We should never see more than two hashes.
    assert(parts.length <= 2);
    // The ID does not include anything after the # (or the # itself).
    id = parts[0];
    // I'm serious.
    assert(!id.contains('#'));
    // Isolates are handled specially, since they can cache sub-objects.
    if (id.startsWith(_isolatesPrefix)) {
      String isolateId = _parseIsolateId(id);
      String objectId = _parseObjectId(id);
      return _getIsolate(isolateId).then((isolate) {
          if (isolate == null) {
            // The isolate does not exist.  Return the VM object instead.
            //
            // TODO(turnidge): Generate a service error?
            return this;
          }
          if (objectId == null) {
            return isolate.reload();
          } else {
            return isolate.get(objectId);
          }
        });
    }

    var obj = _cache[id];
    if (obj != null) {
      return obj.reload();
    }
    // Cache miss.  Get the object from the vm directly.
    return getAsMap(id).then((ObservableMap map) {
        var obj = new ServiceObject._fromMap(this, map);
        if (obj.canCache) {
          _cache.putIfAbsent(id, () => obj);
        }
        return obj;
      });
  }

  /// Gets [id] as an [ObservableMap] from the service directly. If
  /// an error occurs, the future is completed as an error with a
  /// ServiceError or ServiceException. Therefore any chained then() calls
  /// will only receive a map encoding a valid ServiceObject.
  Future<ObservableMap> getAsMap(String id) {
    return getString(id).then((response) {
      try {
        var map = toObservable(JSON.decode(response));
        // Verify that the top level response is a service map.
        if (!_isServiceMap(map)) {
          return new Future.error(
                new ServiceObject._fromMap(this, toObservable({
            'type': 'ServiceException',
            'id': '',
            'kind': 'FormatException',
            'response': map,
            'message': 'Top level service responses must be service maps.',
          })));
        }
        // Preemptively capture ServiceError and ServiceExceptions.
        if (map['type'] == 'ServiceError') {
          return new Future.error(new ServiceObject._fromMap(this, map));
        } else if (map['type'] == 'ServiceException') {
          return new Future.error(new ServiceObject._fromMap(this, map));
        }
        // map is now guaranteed to be a non-error/exception ServiceObject.
        return map;
      } catch (e, st) {
        print(e);
        print(st);
        return new Future.error(
              new ServiceObject._fromMap(this, toObservable({
          'type': 'ServiceException',
          'id': '',
          'kind': 'DecodeException',
          'response': response,
          'message': 'Could not decode JSON: $e',
        })));
      }
    }).catchError((error) {
      // ServiceError, forward to VM's ServiceError stream.
      errors.add(error);
      return new Future.error(error);
    }, test: (e) => e is ServiceError).catchError((exception) {
      // ServiceException, forward to VM's ServiceException stream.
      exceptions.add(exception);
      return new Future.error(exception);
    }, test: (e) => e is ServiceException);
  }

  /// Get [id] as a [String] from the service directly. See [getAsMap].
  Future<String> getString(String id);

  void _update(ObservableMap map, bool mapIsRef) {
    if (mapIsRef) {
      return;
    }
    _loaded = true;
    version = map['version'];
    architecture = map['architecture'];
    uptime = map['uptime'];
    _updateIsolates(map['isolates']);
  }

  void _updateIsolates(List newIsolates) {
    var oldIsolateCache = _isolateCache;
    var newIsolateCache = new Map<String,Isolate>();
    for (var isolateMap in newIsolates) {
      var isolateId = isolateMap['id'];
      var isolate = oldIsolateCache[isolateId];
      if (isolate != null) {
        newIsolateCache[isolateId] = isolate;
      } else {
        isolate = new ServiceObject._fromMap(this, isolateMap);
        newIsolateCache[isolateId] = isolate;
        Logger.root.info('New isolate \'${isolate.id}\'');
      }
    }
    // Update the individual isolates asynchronously.
    newIsolateCache.forEach((isolateId, isolate) {
      isolate.reload();
    });

    _isolateCache = newIsolateCache;
  }
}

/// Snapshot in time of tag counters.
class TagProfileSnapshot {
  final double seconds;
  final List<int> counters;
  int get sum => _sum;
  int _sum = 0;
  TagProfileSnapshot(this.seconds, int countersLength)
      : counters = new List<int>(countersLength);

  /// Set [counters] and update [sum].
  void set(List<int> counters) {
    this.counters.setAll(0, counters);
    for (var i = 0; i < this.counters.length; i++) {
      _sum += this.counters[i];
    }
  }

  /// Set [counters] with the delta from [counters] to [old_counters]
  /// and update [sum].
  void delta(List<int> counters, List<int> old_counters) {
    for (var i = 0; i < this.counters.length; i++) {
      this.counters[i] = counters[i] - old_counters[i];
      _sum += this.counters[i];
    }
  }

  /// Update [counters] with new maximum values seen in [counters].
  void max(List<int> counters) {
    for (var i = 0; i < counters.length; i++) {
      var c = counters[i];
      this.counters[i] = this.counters[i] > c ? this.counters[i] : c;
    }
  }

  /// Zero [counters].
  void zero() {
    for (var i = 0; i < counters.length; i++) {
      counters[i] = 0;
    }
  }
}

class TagProfile {
  final List<String> names = new List<String>();
  final List<TagProfileSnapshot> snapshots = new List<TagProfileSnapshot>();
  double get updatedAtSeconds => _seconds;
  double _seconds;
  TagProfileSnapshot _maxSnapshot;
  int _historySize;
  int _countersLength = 0;

  TagProfile(this._historySize);

  void _processTagProfile(double seconds, ObservableMap tagProfile) {
    _seconds = seconds;
    var counters = tagProfile['counters'];
    if (names.length == 0) {
      // Initialization.
      names.addAll(tagProfile['names']);
      _countersLength = tagProfile['counters'].length;
      for (var i = 0; i < _historySize; i++) {
        var snapshot = new TagProfileSnapshot(0.0, _countersLength);
        snapshot.zero();
        snapshots.add(snapshot);
      }
      // The counters monotonically grow, keep track of the maximum value.
      _maxSnapshot = new TagProfileSnapshot(0.0, _countersLength);
      _maxSnapshot.set(counters);
      return;
    }
    var snapshot = new TagProfileSnapshot(seconds, _countersLength);
    // We snapshot the delta from the current counters to the maximum counter
    // values.
    snapshot.delta(counters, _maxSnapshot.counters);
    _maxSnapshot.max(counters);
    snapshots.add(snapshot);
    // Only keep _historySize snapshots.
    if (snapshots.length > _historySize) {
      snapshots.removeAt(0);
    }
  }
}

/// State for a running isolate.
class Isolate extends ServiceObjectOwner {
  @reflectable VM get vm => owner;
  @reflectable Isolate get isolate => this;
  @observable ObservableMap counters = toObservable(new ObservableMap());

  String get link => _id;
  String get hashLink => '#/$_id';

  @observable bool pausedOnStart = false;
  @observable bool pausedOnExit = false;
  @observable bool running = false;
  @observable bool idle = false;

  Map<String,ServiceObject> _cache = new Map<String,ServiceObject>();
  final TagProfile tagProfile = new TagProfile(20);

  Isolate._empty(ServiceObjectOwner owner) : super._empty(owner) {
    assert(owner is VM);
  }

  /// Creates a link to [id] relative to [this].
  @reflectable String relativeLink(String id) => '${this.id}/$id';
  /// Creates a relative link to [id] with a '#/' prefix.
  @reflectable String relativeHashLink(String id) => '#/${relativeLink(id)}';

  static const TAG_ROOT_ID = 'code/tag-0';

  /// Returns the Code object for the root tag.
  Code tagRoot() {
    // TODO(turnidge): Use get() here instead?
    return _cache[TAG_ROOT_ID];
  }

  void processProfile(ServiceMap profile) {
    assert(profile.serviceType == 'Profile');
    var codeTable = new List<Code>();
    var codeRegions = profile['codes'];
    for (var codeRegion in codeRegions) {
      Code code = codeRegion['code'];
      assert(code != null);
      codeTable.add(code);
    }
    _resetProfileData();
    _updateProfileData(profile, codeTable);
    var exclusiveTrie = profile['exclusive_trie'];
    if (exclusiveTrie != null) {
      profileTrieRoot = _processProfileTrie(exclusiveTrie, codeTable);
    }
  }

  void _resetProfileData() {
    _cache.values.forEach((value) {
        if (value is Code) {
          Code code = value;
          code.resetProfileData();
        }
      });
  }

  void _updateProfileData(ServiceMap profile, List<Code> codeTable) {
    var codeRegions = profile['codes'];
    var sampleCount = profile['samples'];
    for (var codeRegion in codeRegions) {
      Code code = codeRegion['code'];
      code.updateProfileData(codeRegion, codeTable, sampleCount);
    }
  }

  Future refreshCoverage() {
    return get('coverage').then(_processCoverage);
  }

  void _processCoverage(ServiceMap coverage) {
    assert(coverage.serviceType == 'CodeCoverage');
    var coverageList = coverage['coverage'];
    assert(coverageList != null);
    coverageList.forEach((scriptCoverage) {
      _processScriptCoverage(scriptCoverage);
    });
  }

  void _processScriptCoverage(ObservableMap scriptCoverage) {
    // Because the coverage data was upgraded into a ServiceObject,
    // the script can be directly accessed.
    Script script = scriptCoverage['script'];
    assert(_cache.containsValue(script));
    script._processHits(scriptCoverage['hits']);
  }

  ServiceObject getFromMap(ObservableMap map) {
    if (map == null) {
      return null;
    }
    String id = map['id'];
    var obj = _cache[id];
    if (obj != null) {
      return obj;
    }
    // Build the object from the map directly.
    obj = new ServiceObject._fromMap(this, map);
    if (obj.canCache) {
      _cache[id] = obj;
    }
    return obj;
  }

  Future<ServiceObject> get(String id) {
    // Do not allow null ids or empty ids.
    assert(id != null && id != '');
    var obj = _cache[id];
    if (obj != null) {
      return obj.reload();
    }
    // Cache miss.  Get the object from the vm directly.
    return vm.getAsMap(relativeLink(id)).then((ObservableMap map) {
        var obj = new ServiceObject._fromMap(this, map);
        if (obj.canCache) {
          _cache.putIfAbsent(id, () => obj);
        }
        return obj;
      });
  }

  @observable ServiceMap rootLib;
  @observable ObservableMap topFrame;

  @observable String name;
  @observable String vmName;
  @observable String mainPort;
  @observable Map entry;

  @observable final Map<String, double> timers =
      toObservable(new Map<String, double>());

  @observable int newHeapUsed = 0;
  @observable int oldHeapUsed = 0;
  @observable int newHeapCapacity = 0;
  @observable int oldHeapCapacity = 0;

  @observable String fileAndLine;

  @observable DartError error;

  void _update(ObservableMap map, bool mapIsRef) {
    mainPort = map['mainPort'];
    name = map['name'];
    vmName = map['name'];
    if (mapIsRef) {
      return;
    }
    _loaded = true;
    _upgradeCollection(map, isolate);
    if (map['rootLib'] == null ||
        map['timers'] == null ||
        map['heap'] == null) {
      Logger.root.severe("Malformed 'Isolate' response: $map");
      return;
    }
    rootLib = map['rootLib'];
    if (map['entry'] != null) {
      entry = map['entry'];
    }
    if (map['topFrame'] != null) {
      topFrame = map['topFrame'];
    } else {
      topFrame = null ;
    }

    var countersMap = map['tagCounters'];
    if (countersMap != null) {
      var names = countersMap['names'];
      var counts = countersMap['counters'];
      assert(names.length == counts.length);
      var sum = 0;
      for (var i = 0; i < counts.length; i++) {
        sum += counts[i];
      }
      // TODO: Why does this not work without this?
      counters = toObservable({});
      if (sum == 0) {
        for (var i = 0; i < names.length; i++) {
          counters[names[i]] = '0.0%';
        }
      } else {
        for (var i = 0; i < names.length; i++) {
          counters[names[i]] =
              (counts[i] / sum * 100.0).toStringAsFixed(2) + '%';
        }
      }
    }
    var timerMap = {};
    map['timers'].forEach((timer) {
        timerMap[timer['name']] = timer['time'];
      });
    timers['total'] = timerMap['time_total_runtime'];
    timers['compile'] = timerMap['time_compilation'];
    timers['gc'] = 0.0;  // TODO(turnidge): Export this from VM.
    timers['init'] = (timerMap['time_script_loading'] +
                      timerMap['time_creating_snapshot'] +
                      timerMap['time_isolate_initialization'] +
                      timerMap['time_bootstrap']);
    timers['dart'] = timerMap['time_dart_execution'];

    newHeapUsed = map['heap']['usedNew'];
    oldHeapUsed = map['heap']['usedOld'];
    newHeapCapacity = map['heap']['capacityNew'];
    oldHeapCapacity = map['heap']['capacityOld'];

    // Isolate status
    pausedOnStart = map['pausedOnStart'];
    pausedOnExit = map['pausedOnExit'];
    running = map['topFrame'] != null;
    idle = !pausedOnStart && !pausedOnExit && !running;
    error = map['error'];
  }

  Future<TagProfile> updateTagProfile() {
    return vm.getAsMap(relativeLink('profile/tag')).then((ObservableMap m) {
      var seconds = new DateTime.now().millisecondsSinceEpoch / 1000.0;
      tagProfile._processTagProfile(seconds, m);
      return tagProfile;
    });
  }

  @reflectable CodeTrieNode profileTrieRoot;
  // The profile trie is serialized as a list of integers. Each node
  // is recreated by consuming some portion of the list. The format is as
  // follows:
  // [0] index into codeTable of code object.
  // [1] tick count (number of times this stack frame occured).
  // [2] child node count
  // Reading the trie is done by recursively reading the tree depth-first
  // pre-order.
  CodeTrieNode _processProfileTrie(List<int> data, List<Code> codeTable) {
    // Setup state shared across calls to _readTrieNode.
    _trieDataCursor = 0;
    _trieData = data;
    if (_trieData == null) {
      return null;
    }
    if (_trieData.length < 3) {
      // Not enough integers for 1 node.
      return null;
    }
    // Read the tree, returns the root node.
    return _readTrieNode(codeTable);
  }
  int _trieDataCursor;
  List<int> _trieData;
  CodeTrieNode _readTrieNode(List<Code> codeTable) {
    // Read index into code table.
    var index = _trieData[_trieDataCursor++];
    // Lookup code object.
    var code = codeTable[index];
    // Frame counter.
    var count = _trieData[_trieDataCursor++];
    // Create node.
    var node = new CodeTrieNode(code, count);
    // Number of children.
    var children = _trieData[_trieDataCursor++];
    // Recursively read child nodes.
    for (var i = 0; i < children; i++) {
      var child = _readTrieNode(codeTable);
      node.children.add(child);
      node.summedChildCount += child.count;
    }
    return node;
  }
}

/// A [ServiceObject] which implements [ObservableMap].
class ServiceMap extends ServiceObject implements ObservableMap {
  final ObservableMap _map = new ObservableMap();
  static String objectIdRingPrefix = 'objects/';

  bool get canCache {
    return (_serviceType == 'Class' ||
            _serviceType == 'Function' ||
            _serviceType == 'Library') &&
           !_id.startsWith(objectIdRingPrefix);
  }
  bool get immutable => canCache;

  ServiceMap._empty(ServiceObjectOwner owner) : super._empty(owner);

  String toString() => _map.toString();

  void _upgradeValues() {
    assert(owner != null);
    _upgradeCollection(_map, owner);
  }

  void _update(ObservableMap map, bool mapIsRef) {
    _loaded = !mapIsRef;

    // TODO(turnidge): Currently _map.clear() prevents us from
    // upgrading an already upgraded submap.  Is clearing really the
    // right thing to do here?
    _map.clear();
    _map.addAll(map);

    name = _map['user_name'];
    vmName = _map['name'];
    _upgradeValues();
  }

  // Forward Map interface calls.
  void addAll(Map other) => _map.addAll(other);
  void clear() => _map.clear();
  bool containsValue(v) => _map.containsValue(v);
  bool containsKey(k) => _map.containsKey(k);
  void forEach(Function f) => _map.forEach(f);
  putIfAbsent(key, Function ifAbsent) => _map.putIfAbsent(key, ifAbsent);
  void remove(key) => _map.remove(key);
  operator [](k) => _map[k];
  operator []=(k, v) => _map[k] = v;
  bool get isEmpty => _map.isEmpty;
  bool get isNotEmpty => _map.isNotEmpty;
  Iterable get keys => _map.keys;
  Iterable get values => _map.values;
  int get length => _map.length;

  // Forward ChangeNotifier interface calls.
  bool deliverChanges() => _map.deliverChanges();
  void notifyChange(ChangeRecord record) => _map.notifyChange(record);
  notifyPropertyChange(Symbol field, Object oldValue, Object newValue) =>
      _map.notifyPropertyChange(field, oldValue, newValue);
  void observed() => _map.observed();
  void unobserved() => _map.unobserved();
  Stream<List<ChangeRecord>> get changes => _map.changes;
  bool get hasObservers => _map.hasObservers;
}

/// A [DartError] is peered to a Dart Error object.
class DartError extends ServiceObject {
  DartError._empty(ServiceObject owner) : super._empty(owner);

  @observable String kind;
  @observable String message;
  @observable ServiceMap exception;
  @observable ServiceMap stacktrace;

  void _update(ObservableMap map, bool mapIsRef) {
    kind = map['kind'];
    message = map['message'];
    exception = new ServiceObject._fromMap(owner, map['exception']);
    stacktrace = new ServiceObject._fromMap(owner, map['stacktrace']);
    name = 'DartError $kind';
    vmName = name;
  }
}

/// A [ServiceError] is an error that was triggered in the service
/// server or client. Errors are prorammer mistakes that could have
/// been prevented, for example, requesting a non-existant path over the
/// service.
class ServiceError extends ServiceObject {
  ServiceError._empty(ServiceObjectOwner owner) : super._empty(owner);

  @observable String kind;
  @observable String message;

  void _update(ObservableMap map, bool mapIsRef) {
    _loaded = true;
    kind = map['kind'];
    message = map['message'];
    name = 'ServiceError $kind';
    vmName = name;
  }
}

/// A [ServiceException] is an exception that was triggered in the service
/// server or client. Exceptions are events that should be handled,
/// for example, an isolate went away or the connection to the VM was lost.
class ServiceException extends ServiceObject {
  ServiceException._empty(ServiceObject owner) : super._empty(owner);

  @observable String kind;
  @observable String message;
  @observable dynamic response;

  void _update(ObservableMap map, bool mapIsRef) {
    kind = map['kind'];
    message = map['message'];
    response = map['response'];
    name = 'ServiceException $kind';
    vmName = name;
  }
}

class ScriptLine {
  @reflectable final int line;
  @reflectable final String text;
  ScriptLine(this.line, this.text);
}

class Script extends ServiceObject {
  @reflectable final lines = new ObservableList<ScriptLine>();
  @reflectable final hits = new ObservableMap<int, int>();
  @observable ServiceObject library;
  @observable String kind;
  @observable int firstTokenPos;
  @observable int lastTokenPos;
  bool get canCache => true;
  bool get immutable => true;

  String _shortUrl;
  String _url;

  Script._empty(ServiceObjectOwner owner) : super._empty(owner);

  /// This function maps a token position to a line number.
  int tokenToLine(int token) => _tokenToLine[token];
  Map _tokenToLine;

  /// This function maps a token position to a column number.
  int tokenToCol(int token) => _tokenToCol[token];
  Map _tokenToCol;

  void _update(ObservableMap map, bool mapIsRef) {
    kind = map['kind'];
    _url = map['name'];
    _shortUrl = _url.substring(_url.lastIndexOf('/') + 1);
    name = _shortUrl;
    vmName = _url;
    _processSource(map['source']);
    _parseTokenPosTable(map['tokenPosTable']);
  }

  void _parseTokenPosTable(List<List<int>> table) {
    if (table == null) {
      return;
    }
    _tokenToLine = {};
    _tokenToCol = {};
    firstTokenPos = null;
    lastTokenPos = null;
    for (var line in table) {
      // Each entry begins with a line number...
      var lineNumber = line[0];
      for (var pos = 1; pos < line.length; pos += 2) {
        // ...and is followed by (token offset, col number) pairs.
        var tokenOffset = line[pos];
        var colNumber = line[pos+1];
        if (firstTokenPos == null) {
          // Mark first token position.
          firstTokenPos = tokenOffset;
          lastTokenPos = tokenOffset;
        } else {
          // Keep track of max and min token positions.
          firstTokenPos = (firstTokenPos <= tokenOffset) ?
              firstTokenPos : tokenOffset;
          lastTokenPos = (lastTokenPos >= tokenOffset) ?
              lastTokenPos : tokenOffset;
        }
        _tokenToLine[tokenOffset] = lineNumber;
        _tokenToCol[tokenOffset] = colNumber;
      }
    }
  }

  void _processHits(List scriptHits) {
    // Update hits table.
    for (var i = 0; i < scriptHits.length; i += 2) {
      var line = scriptHits[i];
      var hit = scriptHits[i + 1]; // hit status.
      assert(line >= 1); // Lines start at 1.
      hits[line] = hit;
    }
  }

  void _processSource(String source) {
    // Preemptyively mark that this is not loaded.
    _loaded = false;
    if (source == null) {
      return;
    }
    var sourceLines = source.split('\n');
    if (sourceLines.length == 0) {
      return;
    }
    // We have the source to the script. This is now loaded.
    _loaded = true;
    lines.clear();
    Logger.root.info('Adding ${sourceLines.length} source lines for ${_url}');
    for (var i = 0; i < sourceLines.length; i++) {
      lines.add(new ScriptLine(i + 1, sourceLines[i]));
    }
  }
}

class CodeTick {
  final int address;
  final int exclusiveTicks;
  final int inclusiveTicks;
  CodeTick(this.address, this.exclusiveTicks, this.inclusiveTicks);
}


class CodeInstruction extends Observable {
  @observable final int address;
  @observable final String machine;
  @observable final String human;

  static String formatPercent(num a, num total) {
    var percent = 100.0 * (a / total);
    return '${percent.toStringAsFixed(2)}%';
  }

  CodeInstruction(this.address, this.machine, this.human);

  @reflectable String formattedAddress() {
    if (address == 0) {
      return '';
    }
    return '0x${address.toRadixString(16)}';
  }

  @reflectable String formattedInclusive(Code code) {
    if (code == null) {
      return '';
    }
    var tick = code.addressTicks[address];
    if (tick == null) {
      return '';
    }
    // Don't show inclusive ticks if they are the same as exclusive ticks.
    if (tick.inclusiveTicks == tick.exclusiveTicks) {
      return '';
    }
    var pcent = formatPercent(tick.inclusiveTicks, code.totalSamplesInProfile);
    return '$pcent (${tick.inclusiveTicks})';
  }

  @reflectable String formattedExclusive(Code code) {
    if (code == null) {
      return '';
    }
    var tick = code.addressTicks[address];
    if (tick == null) {
      return '';
    }
    var pcent = formatPercent(tick.exclusiveTicks, code.totalSamplesInProfile);
    return '$pcent (${tick.exclusiveTicks})';
  }
}

class CodeKind {
  final _value;
  const CodeKind._internal(this._value);
  String toString() => '$_value';

  static CodeKind fromString(String s) {
    if (s == 'Native') {
      return Native;
    } else if (s == 'Dart') {
      return Dart;
    } else if (s == 'Collected') {
      return Collected;
    } else if (s == 'Reused') {
      return Reused;
    } else if (s == 'Tag') {
      return Tag;
    }
    Logger.root.warning('Unknown code kind $s');
    throw new FallThroughError();
  }
  static const Native = const CodeKind._internal('Native');
  static const Dart = const CodeKind._internal('Dart');
  static const Collected = const CodeKind._internal('Collected');
  static const Reused = const CodeKind._internal('Reused');
  static const Tag = const CodeKind._internal('Tag');
}

class CodeCallCount {
  final Code code;
  final int count;
  CodeCallCount(this.code, this.count);
}

class CodeTrieNode {
  final Code code;
  final int count;
  final children = new List<CodeTrieNode>();
  int summedChildCount = 0;
  CodeTrieNode(this.code, this.count);
}

class Code extends ServiceObject {
  @observable CodeKind kind;
  @observable int totalSamplesInProfile = 0;
  @reflectable int exclusiveTicks = 0;
  @reflectable int inclusiveTicks = 0;
  @reflectable int startAddress = 0;
  @reflectable int endAddress = 0;
  @reflectable final callers = new List<CodeCallCount>();
  @reflectable final callees = new List<CodeCallCount>();
  @reflectable final instructions = new ObservableList<CodeInstruction>();
  @reflectable final addressTicks = new ObservableMap<int, CodeTick>();
  @observable String formattedInclusiveTicks = '';
  @observable String formattedExclusiveTicks = '';
  @observable ServiceMap objectPool;
  @observable ServiceMap function;
  String name;
  String vmName;

  bool get canCache => true;
  bool get immutable => true;

  Code._empty(ServiceObjectOwner owner) : super._empty(owner);

  // Reset all data associated with a profile.
  void resetProfileData() {
    totalSamplesInProfile = 0;
    exclusiveTicks = 0;
    inclusiveTicks = 0;
    formattedInclusiveTicks = '';
    formattedExclusiveTicks = '';
    callers.clear();
    callees.clear();
    addressTicks.clear();
  }

  /// Reload [this]. Returns a future which completes to [this] or
  /// a [ServiceError].
  Future<ServiceObject> reload() {
    assert(kind != null);
    if (kind == CodeKind.Dart) {
      // We only reload Dart code.
      return super.reload();
    }
    return new Future.value(this);
  }

  void _resolveCalls(List<CodeCallCount> calls, List data, List<Code> codes) {
    // Assert that this has been cleared.
    assert(calls.length == 0);
    // Resolve.
    for (var i = 0; i < data.length; i += 2) {
      var index = int.parse(data[i]);
      var count = int.parse(data[i + 1]);
      assert(index >= 0);
      assert(index < codes.length);
      calls.add(new CodeCallCount(codes[index], count));
    }
    // Sort to descending count order.
    calls.sort((a, b) => b.count - a.count);
  }


  static String formatPercent(num a, num total) {
    var percent = 100.0 * (a / total);
    return '${percent.toStringAsFixed(2)}%';
  }

  void updateProfileData(Map profileData,
                         List<Code> codeTable,
                         int sampleCount) {
    // Assert we have a CodeRegion entry.
    assert(profileData['type'] == 'CodeRegion');
    // Assert we are handed profile data for this code object.
    assert(profileData['code'] == this);
    totalSamplesInProfile = sampleCount;
    inclusiveTicks = int.parse(profileData['inclusive_ticks']);
    exclusiveTicks = int.parse(profileData['exclusive_ticks']);
    _resolveCalls(callers, profileData['callers'], codeTable);
    _resolveCalls(callees, profileData['callees'], codeTable);
    var ticks = profileData['ticks'];
    if (ticks != null) {
      _processTicks(ticks);
    }
    formattedInclusiveTicks =
        '${formatPercent(inclusiveTicks, totalSamplesInProfile)} '
        '($inclusiveTicks)';
    formattedExclusiveTicks =
        '${formatPercent(exclusiveTicks, totalSamplesInProfile)} '
        '($exclusiveTicks)';
  }

  void _update(ObservableMap m, bool mapIsRef) {
    name = m['user_name'];
    vmName = m['name'];
    kind = CodeKind.fromString(m['kind']);
    startAddress = int.parse(m['start'], radix:16);
    endAddress = int.parse(m['end'], radix:16);
    function = isolate.getFromMap(m['function']);
    objectPool = isolate.getFromMap(m['object_pool']);
    var disassembly = m['disassembly'];
    if (disassembly != null) {
      _processDisassembly(disassembly);
    }
    // We are loaded if we have instructions or are not Dart code.
    _loaded = (instructions.length != 0) || (kind != CodeKind.Dart);
    hasDisassembly = (instructions.length != 0) && (kind == CodeKind.Dart);
  }

  @observable bool hasDisassembly = false;

  void _processDisassembly(List<String> disassembly){
    assert(disassembly != null);
    instructions.clear();
    assert((disassembly.length % 3) == 0);
    for (var i = 0; i < disassembly.length; i += 3) {
      var address = 0;  // Assume code comment.
      var machine = disassembly[i + 1];
      var human = disassembly[i + 2];
      if (disassembly[i] != '') {
        // Not a code comment, extract address.
        address = int.parse(disassembly[i]);
      }
      var instruction = new CodeInstruction(address, machine, human);
      instructions.add(instruction);
    }
  }

  void _processTicks(List<String> profileTicks) {
    assert(profileTicks != null);
    assert((profileTicks.length % 3) == 0);
    for (var i = 0; i < profileTicks.length; i += 3) {
      var address = int.parse(profileTicks[i], radix:16);
      var exclusive = int.parse(profileTicks[i + 1]);
      var inclusive = int.parse(profileTicks[i + 2]);
      var tick = new CodeTick(address, exclusive, inclusive);
      addressTicks[address] = tick;
    }
  }

  /// Returns true if [address] is contained inside [this].
  bool contains(int address) {
    return (address >= startAddress) && (address < endAddress);
  }

  /// Sum all caller counts.
  int sumCallersCount() => _sumCallCount(callers);
  /// Specific caller count.
  int callersCount(Code code) => _callCount(callers, code);
  /// Sum of callees count.
  int sumCalleesCount() => _sumCallCount(callees);
  /// Specific callee count.
  int calleesCount(Code code) => _callCount(callees, code);

  int _sumCallCount(List<CodeCallCount> calls) {
    var sum = 0;
    for (CodeCallCount caller in calls) {
      sum += caller.count;
    }
    return sum;
  }

  int _callCount(List<CodeCallCount> calls, Code code) {
    for (CodeCallCount caller in calls) {
      if (caller.code == code) {
        return caller.count;
      }
    }
    return 0;
  }

  @reflectable bool get isDartCode => kind == CodeKind.Dart;
}

// Returns true if [map] is a service map. i.e. it has the following keys:
// 'id' and a 'type'.
bool _isServiceMap(ObservableMap m) {
  return (m != null) && (m['id'] != null) && (m['type'] != null);
}

bool _hasRef(String type) => type.startsWith('@');
String _stripRef(String type) => (_hasRef(type) ? type.substring(1) : type);

/// Recursively upgrades all [ServiceObject]s inside [collection] which must
/// be an [ObservableMap] or an [ObservableList]. Upgraded elements will be
/// associated with [vm] and [isolate].
void _upgradeCollection(collection, ServiceObjectOwner owner) {
  if (collection is ServiceMap) {
    return;
  }
  if (collection is ObservableMap) {
    _upgradeObservableMap(collection, owner);
  } else if (collection is ObservableList) {
    _upgradeObservableList(collection, owner);
  }
}

void _upgradeObservableMap(ObservableMap map, ServiceObjectOwner owner) {
  map.forEach((k, v) {
    if ((v is ObservableMap) && _isServiceMap(v)) {
      map[k] = owner.getFromMap(v);
    } else if (v is ObservableList) {
      _upgradeObservableList(v, owner);
    } else if (v is ObservableMap) {
      _upgradeObservableMap(v, owner);
    }
  });
}

void _upgradeObservableList(ObservableList list, ServiceObjectOwner owner) {
  for (var i = 0; i < list.length; i++) {
    var v = list[i];
    if ((v is ObservableMap) && _isServiceMap(v)) {
      list[i] = owner.getFromMap(v);
    } else if (v is ObservableList) {
      _upgradeObservableList(v, owner);
    } else if (v is ObservableMap) {
      _upgradeObservableMap(v, owner);
    }
  }
}
