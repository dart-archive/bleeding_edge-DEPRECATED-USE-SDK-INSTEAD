// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of service;

/// A [ServiceObject] represents a persistent object within the vm.
abstract class ServiceObject extends Observable {
  static int LexicalSortName(ServiceObject o1, ServiceObject o2) {
    return o1.name.compareTo(o2.name);
  }

  List removeDuplicatesAndSortLexical(List<ServiceObject> list) {
    return list.toSet().toList()..sort(LexicalSortName);
  }

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

  /// The user-level type of this object.
  @reflectable String get type => _type;
  String _type;

  /// The vm type of this object.
  @reflectable String get vmType => _vmType;
  String _vmType;

  static bool _isInstanceType(String type) {
    switch (type) {
      case 'BoundedType':
      case 'Instance':
      case 'List':
      case 'String':
      case 'Type':
      case 'TypeParameter':
      case 'TypeRef':
      case 'bool':
      case 'double':
      case 'int':
      case 'null':
        return true;
      default:
        return false;
    }
  }

  static bool _isTypeType(String type) {
    switch (type) {
      case 'BoundedType':
      case 'Type':
      case 'TypeParameter':
      case 'TypeRef':
        return true;
      default:
        return false;
    }
  }

  bool get isAbstractType => _isTypeType(type);
  bool get isBool => type == 'bool';
  bool get isContext => type == 'Context';
  bool get isDouble => type == 'double';
  bool get isError => type == 'Error';
  bool get isInstance => _isInstanceType(type);
  bool get isInt => type == 'int';
  bool get isList => type == 'List';
  bool get isNull => type == 'null';
  bool get isSentinel => type == 'Sentinel';
  bool get isString => type == 'String';

  // Kinds of Instance.
  bool get isMirrorReference => vmType == 'MirrorReference';
  bool get isWeakProperty => vmType == 'WeakProperty';
  bool get isClosure => false;
  bool get isPlainInstance {
    return (type == 'Instance' &&
            !isMirrorReference && !isWeakProperty && !isClosure);
  }

  /// Has this object been fully loaded?
  bool get loaded => _loaded;
  bool _loaded = false;
  // TODO(turnidge): Make loaded observable and get rid of loading
  // from Isolate.

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
    var vmType = map['_vmType'] != null ? _stripRef(map['_vmType']) : type;
    var obj = null;
    assert(type != 'VM');
    switch (type) {
      case 'Breakpoint':
        obj = new Breakpoint._empty(owner);
        break;
      case 'Class':
        obj = new Class._empty(owner);
        break;
      case 'Code':
        obj = new Code._empty(owner);
        break;
      case 'Context':
        obj = new Context._empty(owner);
        break;
      case 'Counter':
        obj = new ServiceMetric._empty(owner);
        break;
      case 'Error':
        obj = new DartError._empty(owner);
        break;
      case 'Field':
        obj = new Field._empty(owner);
        break;
      case 'Function':
        obj = new ServiceFunction._empty(owner);
        break;
      case 'Gauge':
        obj = new ServiceMetric._empty(owner);
        break;
      case 'Isolate':
        obj = new Isolate._empty(owner.vm);
        break;
      case 'Library':
        obj = new Library._empty(owner);
        break;
      case 'Object':
        switch (vmType) {
          case 'PcDescriptors':
            obj = new PcDescriptors._empty(owner);
            break;
          case 'LocalVarDescriptors':
            obj = new LocalVarDescriptors._empty(owner);
            break;
          case 'TokenStream':
            obj = new TokenStream._empty(owner);
            break;
        }
        break;
      case 'ServiceError':
        obj = new ServiceError._empty(owner);
        break;
      case 'ServiceEvent':
        obj = new ServiceEvent._empty(owner);
        break;
      case 'ServiceException':
        obj = new ServiceException._empty(owner);
        break;
      case 'Script':
        obj = new Script._empty(owner);
        break;
      case 'Socket':
        obj = new Socket._empty(owner);
        break;
      default:
        if (_isInstanceType(type) ||
            type == 'Sentinel') {  // TODO(rmacnak): Separate this out.
          obj = new Instance._empty(owner);
        }
        break;
    }
    if (obj == null) {
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

  Future<ObservableMap> _fetchDirect() {
    Map params = {
      'objectId': id,
    };
    return isolate.invokeRpcNoUpgrade('getObject', params);
  }

  /// Reload [this]. Returns a future which completes to [this] or
  /// a [ServiceError].
  Future<ServiceObject> reload() {
    if (id == '') {
      // Errors don't have ids.
      assert(type == 'Error');
      return new Future.value(this);
    }
    if (loaded && immutable) {
      return new Future.value(this);
    }
    if (_inProgressReload == null) {
      _inProgressReload = _fetchDirect().then((ObservableMap map) {
          var mapType = _stripRef(map['type']);
          if (mapType != _type) {
            // If the type changes, return a new object instead of
            // updating the existing one.
            //
            // TODO(turnidge): Check for vmType changing as well?
            assert(mapType == 'Error' || mapType == 'Sentinel');
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
    assert(_type == null || _type == mapType);

    if (_id != null && _id != map['id']) {
      // It is only safe to change an id when the object isn't cacheable.
      assert(!canCache);
    }
    _id = map['id'];

    _type = mapType;

    // When the response specifies a specific vmType, use it.
    // Otherwise the vmType of the response is the same as the 'user'
    // type.
    if (map.containsKey('_vmType')) {
      _vmType = _stripRef(map['_vmType']);
    } else {
      _vmType = _type;
    }

    _update(map, mapIsRef);
  }

  // Updates internal state from [map]. [map] can be a reference.
  void _update(ObservableMap map, bool mapIsRef);

  // Helper that can be passed to .catchError that ignores the error.
  _ignoreError(error, stackTrace) {
    // do nothing.
  }
}

abstract class Coverage {
  // Following getters and functions will be provided by [ServiceObject].
  String get id;
  Isolate get isolate;

  /// Default handler for coverage data.
  void processCoverageData(List coverageData) {
    coverageData.forEach((scriptCoverage) {
      assert(scriptCoverage['script'] != null);
      scriptCoverage['script']._processHits(scriptCoverage['hits']);
    });
  }

  Future refreshCoverage() {
    Map params = {};
    if (this is! Isolate) {
      params['targetId'] = id;
    }
    return isolate.invokeRpcNoUpgrade('getCoverage', params).then(
        (ObservableMap map) {
          var coverage = new ServiceObject._fromMap(isolate, map);
          assert(coverage.type == 'CodeCoverage');
          var coverageList = coverage['coverage'];
          assert(coverageList != null);
          processCoverageData(coverageList);
          return this;
        });
  }
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

  // TODO(johnmccutchan): Ensure that isolates do not end up in _cache.
  Map<String,ServiceObject> _cache = new Map<String,ServiceObject>();
  final ObservableMap<String,Isolate> _isolateCache =
      new ObservableMap<String,Isolate>();

  @reflectable Iterable<Isolate> get isolates => _isolateCache.values;

  @observable String version = 'unknown';
  @observable String targetCPU;
  @observable int architectureBits;
  @observable double uptime = 0.0;
  @observable bool assertsEnabled = false;
  @observable bool typeChecksEnabled = false;
  @observable String pid = '';
  @observable DateTime lastUpdate;

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
  final StreamController<ServiceEvent> events =
      new StreamController.broadcast();

  bool _isIsolateLifecycleEvent(String eventType) {
    return _isIsolateShutdownEvent(eventType) ||
           _isIsolateCreatedEvent(eventType);
  }

  bool _isIsolateShutdownEvent(String eventType) {
    return (eventType == 'IsolateShutdown');
  }

  bool _isIsolateCreatedEvent(String eventType) {
    return (eventType == 'IsolateCreated');
  }

  void postServiceEvent(String response, ByteData data) {
    var map;
    try {
      map = _parseJSON(response);
      assert(!map.containsKey('_data'));
      if (data != null) {
        map['_data'] = data;
      }
    } catch (e, st) {
      Logger.root.severe('Ignoring malformed event response: ${response}');
      return;
    }
    if (map['type'] != 'ServiceEvent') {
      Logger.root.severe(
          "Expected 'ServiceEvent' but found '${map['type']}'");
      return;
    }

    var eventType = map['eventType'];

    if (_isIsolateLifecycleEvent(eventType)) {
      String isolateId = map['isolate']['id'];
      var event;
      if (_isIsolateCreatedEvent(eventType)) {
        _onIsolateCreated(map['isolate']);
        // By constructing the event *after* adding the isolate to the
        // isolate cache, the call to getFromMap will use the cached Isolate.
        event = new ServiceObject._fromMap(this, map);
      } else {
        assert(_isIsolateShutdownEvent(eventType));
        // By constructing the event *before* removing the isolate from the
        // isolate cache, the call to getFromMap will use the cached Isolate.
        event = new ServiceObject._fromMap(this, map);
        _onIsolateShutdown(isolateId);
      }
      assert(event != null);
      events.add(event);
      return;
    }

    // Extract the owning isolate from the event itself.
    String owningIsolateId = map['isolate']['id'];
    getIsolate(owningIsolateId).then((owningIsolate) {
        if (owningIsolate == null) {
          // TODO(koda): Do we care about GC events in VM isolate?
          Logger.root.severe('Ignoring event with unknown isolate id: '
                             '$owningIsolateId');
          return;
        }
        var event = new ServiceObject._fromMap(owningIsolate, map);
        events.add(event);
    });
  }

  Isolate _onIsolateCreated(Map isolateMap) {
    var isolateId = isolateMap['id'];
    assert(!_isolateCache.containsKey(isolateId));
    Isolate isolate = new ServiceObject._fromMap(this, isolateMap);
    _isolateCache[isolateId] = isolate;
    notifyPropertyChange(#isolates, true, false);
    // Eagerly load the isolate.
    isolate.load().catchError((e) {
      Logger.root.info('Eagerly loading an isolate failed: $e');
    });
    return isolate;
  }

  void _onIsolateShutdown(String isolateId) {
    assert(_isolateCache.containsKey(isolateId));
    _isolateCache.remove(isolateId);
    notifyPropertyChange(#isolates, true, false);
  }

  void _updateIsolatesFromList(List isolateList) {
    var shutdownIsolates = <String>[];
    var createdIsolates = <Map>[];
    var isolateStillExists = <String, bool>{};

    // Start with the assumption that all isolates are gone.
    for (var isolateId in _isolateCache.keys) {
      isolateStillExists[isolateId] = false;
    }

    // Find created isolates and mark existing isolates as living.
    for (var isolateMap in isolateList) {
      var isolateId = isolateMap['id'];
      if (!_isolateCache.containsKey(isolateId)) {
        createdIsolates.add(isolateMap);
      } else {
        isolateStillExists[isolateId] = true;
      }
    }

    // Find shutdown isolates.
    isolateStillExists.forEach((isolateId, exists) {
      if (!exists) {
        shutdownIsolates.add(isolateId);
      }
    });

    // Process shutdown.
    for (var isolateId in shutdownIsolates) {
      _onIsolateShutdown(isolateId);
    }

    // Process creation.
    for (var isolateMap in createdIsolates) {
      _onIsolateCreated(isolateMap);
    }
  }

  static final String _isolateIdPrefix = 'isolates/';

  ServiceObject getFromMap(ObservableMap map) {
    if (map == null) {
      return null;
    }
    String id = map['id'];
    if (!id.startsWith(_isolateIdPrefix)) {
      // Currently the VM only supports upgrading Isolate ServiceObjects.
      throw new UnimplementedError();
    }

    // Check cache.
    var isolate = _isolateCache[id];
    if (isolate == null) {
      // We should never see an unknown isolate here.
      throw new UnimplementedError();
    }
    return isolate;
  }

  // Note that this function does not reload the isolate if it found
  // in the cache.
  Future<ServiceObject> getIsolate(String isolateId) {
    if (!loaded) {
      // Trigger a VM load, then get the isolate.
      return load().then((_) => getIsolate(isolateId)).catchError(_ignoreError);
    }
    return new Future.value(_isolateCache[isolateId]);
  }

  dynamic _reviver(dynamic key, dynamic value) {
    return value;
  }

  ObservableMap _parseJSON(String response) {
    var map;
    try {
      var decoder = new JsonDecoder(_reviver);
      map = decoder.convert(response);
    } catch (e) {
      return toObservable({
        'type': 'ServiceException',
        'kind': 'JSONDecodeException',
        'response': map,
        'message': 'Could not decode JSON: $e',
      });
    }
    return toObservable(map);
  }

  Future<ObservableMap> _processMap(ObservableMap map) {
    // Verify that the top level response is a service map.
    if (!_isServiceMap(map)) {
      return new Future.error(
            new ServiceObject._fromMap(this, toObservable({
        'type': 'ServiceException',
        'kind': 'ResponseFormatException',
        'response': map,
        'message': 'Top level service responses must be service maps: ${map}.',
      })));
    }
    // Preemptively capture ServiceError and ServiceExceptions.
    if (map['type'] == 'ServiceError') {
      return new Future.error(new ServiceObject._fromMap(this, map));
    } else if (map['type'] == 'ServiceException') {
      return new Future.error(new ServiceObject._fromMap(this, map));
    }
    // map is now guaranteed to be a non-error/exception ServiceObject.
    return new Future.value(map);
  }

  // Implemented in subclass.
  Future<String> invokeRpcRaw(String method, Map params);

  Future<ObservableMap> invokeRpcNoUpgrade(String method, Map params) {
    return invokeRpcRaw(method, params).then((String response) {
      var map = _parseJSON(response);
      if (Tracer.current != null) {
        Tracer.current.trace("Received response for ${method}/${params}}",
                             map:map);
      }

      // Check for ill-formed responses.
      return _processMap(map);
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

  Future<ServiceObject> invokeRpc(String method, Map params) {
    return invokeRpcNoUpgrade(method, params).then((ObservableMap response) {
      var obj = new ServiceObject._fromMap(this, response);
      if ((obj != null) && obj.canCache) {
        String objId = obj.id;
        _cache.putIfAbsent(objId, () => obj);
      }
      return obj;
    });
  }

  Future<ObservableMap> _fetchDirect() {
    return invokeRpcNoUpgrade('getVM', {});
  }

  Future<ServiceObject> getFlagList() {
    return invokeRpc('getFlagList', {});
  }

  /// Force the VM to disconnect.
  void disconnect();
  /// Completes when the VM first connects.
  Future get onConnect;
  /// Completes when the VM disconnects or there was an error connecting.
  Future get onDisconnect;

  void _update(ObservableMap map, bool mapIsRef) {
    if (mapIsRef) {
      return;
    }
    _loaded = true;
    version = map['version'];
    targetCPU = map['targetCPU'];
    architectureBits = map['architectureBits'];
    uptime = map['uptime'];
    var dateInMillis = int.parse(map['date']);
    lastUpdate = new DateTime.fromMillisecondsSinceEpoch(dateInMillis);
    assertsEnabled = map['assertsEnabled'];
    pid = map['pid'];
    typeChecksEnabled = map['typeChecksEnabled'];
    _updateIsolatesFromList(map['isolates']);
  }

  // Reload all isolates.
  Future reloadIsolates() {
    var reloads = [];
    for (var isolate in isolates) {
      var reload = isolate.reload().catchError((e) {
        Logger.root.info('Bulk reloading of isolates failed: $e');
      });
      reloads.add(reload);
    }
    return Future.wait(reloads);
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

class HeapSpace extends Observable {
  @observable int used = 0;
  @observable int capacity = 0;
  @observable int external = 0;
  @observable int collections = 0;
  @observable double totalCollectionTimeInSeconds = 0.0;
  @observable double averageCollectionPeriodInMillis = 0.0;

  void update(Map heapMap) {
    used = heapMap['used'];
    capacity = heapMap['capacity'];
    external = heapMap['external'];
    collections = heapMap['collections'];
    totalCollectionTimeInSeconds = heapMap['time'];
    averageCollectionPeriodInMillis = heapMap['avgCollectionPeriodMillis'];
  }
}

class HeapSnapshot {
  final ObjectGraph graph;
  final DateTime timeStamp;
  final Isolate isolate;

  HeapSnapshot(this.isolate, ByteData data) :
      graph = new ObjectGraph(new ReadStream(data)),
      timeStamp = new DateTime.now() {
  }

  List<Future<ServiceObject>> getMostRetained({int classId, int limit}) {
    var result = [];
    for (var v in graph.getMostRetained(classId: classId, limit: limit)) {
      var address = v.addressForWordSize(isolate.vm.architectureBits ~/ 8);
      result.add(isolate.getObjectByAddress(address.toRadixString(16)).then((obj) {
        obj.retainedSize = v.retainedSize;
        return new Future(() => obj);
      }));
    }
    return result;
  }


}

/// State for a running isolate.
class Isolate extends ServiceObjectOwner with Coverage {
  @reflectable VM get vm => owner;
  @reflectable Isolate get isolate => this;
  @observable ObservableMap counters = new ObservableMap();

  @observable ServiceEvent pauseEvent = null;
  bool get _isPaused => pauseEvent != null;

  @observable bool running = false;
  @observable bool idle = false;
  @observable bool loading = true;
  @observable bool ioEnabled = false;

  Map<String,ServiceObject> _cache = new Map<String,ServiceObject>();
  final TagProfile tagProfile = new TagProfile(20);

  Isolate._empty(ServiceObjectOwner owner) : super._empty(owner) {
    assert(owner is VM);
  }

  void resetCachedProfileData() {
    _cache.values.forEach((value) {
      if (value is Code) {
        Code code = value;
        code.profile = null;
      } else if (value is ServiceFunction) {
        ServiceFunction function = value;
        function.profile = null;
      }
    });
  }

  /// Fetches and builds the class hierarchy for this isolate. Returns the
  /// Object class object.
  Future<Class> getClassHierarchy() {
    return invokeRpc('getClassList', {})
        .then(_loadClasses)
        .then(_buildClassHierarchy);
  }

  /// Given the class list, loads each class.
  Future<List<Class>> _loadClasses(ServiceMap classList) {
    assert(classList.type == 'ClassList');
    var futureClasses = [];
    for (var cls in classList['classes']) {
      // Skip over non-class classes.
      if (cls is Class) {
        futureClasses.add(cls.load());
      }
    }
    return Future.wait(futureClasses);
  }

  /// Builds the class hierarchy and returns the Object class.
  Future<Class> _buildClassHierarchy(List<Class> classes) {
    rootClasses.clear();
    objectClass = null;
    for (var cls in classes) {
      if (cls.superclass == null) {
        rootClasses.add(cls);
      }
      if ((cls.vmName == 'Object') && (cls.isPatch == false)) {
        objectClass = cls;
      }
    }
    assert(objectClass != null);
    return new Future.value(objectClass);
  }

  ServiceObject getFromMap(ObservableMap map) {
    if (map == null) {
      return null;
    }
    String mapId = map['id'];
    var obj = (mapId != null) ? _cache[mapId] : null;
    if (obj != null) {
      // Consider calling update when map is not a reference.
      return obj;
    }
    // Build the object from the map directly.
    obj = new ServiceObject._fromMap(this, map);
    if ((obj != null) && obj.canCache) {
      _cache[mapId] = obj;
    }
    return obj;
  }

  Future<ObservableMap> invokeRpcNoUpgrade(String method, Map params) {
    params['isolateId'] = id;
    return vm.invokeRpcNoUpgrade(method, params);
  }

  Future<ServiceObject> invokeRpc(String method, Map params) {
    return invokeRpcNoUpgrade(method, params).then((ObservableMap response) {
      var obj = new ServiceObject._fromMap(this, response);
      if ((obj != null) && obj.canCache) {
        String objId = obj.id;
        _cache.putIfAbsent(objId, () => obj);
      }
      return obj;
    });
  }

  Future<ServiceObject> getObject(String objectId) {
    assert(objectId != null && objectId != '');
    var obj = _cache[objectId];
    if (obj != null) {
      return obj.reload();
    }
    Map params = {
      'objectId': objectId,
    };
    return isolate.invokeRpc('getObject', params);
  }

  Future<ObservableMap> _fetchDirect() {
    return invokeRpcNoUpgrade('getIsolate', {});
  }

  @observable Class objectClass;
  @observable final rootClasses = new ObservableList<Class>();

  @observable Library rootLib;
  @observable ObservableList<Library> libraries =
      new ObservableList<Library>();
  @observable ObservableMap topFrame;

  @observable String name;
  @observable String vmName;
  @observable String mainPort;
  @observable ServiceFunction entry;

  @observable final Map<String, double> timers =
      toObservable(new Map<String, double>());

  final HeapSpace newSpace = new HeapSpace();
  final HeapSpace oldSpace = new HeapSpace();

  @observable String fileAndLine;

  @observable DartError error;
  @observable HeapSnapshot latestSnapshot;
  Completer<HeapSnapshot> _snapshotFetch;

  void loadHeapSnapshot(ServiceEvent event) {
    latestSnapshot = new HeapSnapshot(this, event.data);
    _snapshotFetch.complete(latestSnapshot);
  }

  Future<HeapSnapshot> fetchHeapSnapshot() {
    if (_snapshotFetch == null || _snapshotFetch.isCompleted) {
      _snapshotFetch = new Completer<HeapSnapshot>();
      isolate.invokeRpcNoUpgrade('requestHeapSnapshot', {});
    }
    return _snapshotFetch.future;
  }

  void updateHeapsFromMap(ObservableMap map) {
    newSpace.update(map['new']);
    oldSpace.update(map['old']);
  }

  void _update(ObservableMap map, bool mapIsRef) {
    mainPort = map['mainPort'];
    name = map['name'];
    vmName = map['name'];
    if (mapIsRef) {
      return;
    }
    _loaded = true;
    loading = false;

    reloadBreakpoints();
    _upgradeCollection(map, isolate);
    if (map['rootLib'] == null ||
        map['timers'] == null ||
        map['heaps'] == null) {
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

    updateHeapsFromMap(map['heaps']);

    List features = map['features'];
    if (features != null) {
      for (var feature in features) {
        if (feature == 'io') {
          ioEnabled = true;
        }
      }
    }
    // Isolate status
    pauseEvent = map['pauseEvent'];
    running = (!_isPaused && map['topFrame'] != null);
    idle = (!_isPaused && map['topFrame'] == null);
    error = map['error'];

    libraries.clear();
    libraries.addAll(map['libraries']);
    libraries.sort(ServiceObject.LexicalSortName);
  }

  Future<TagProfile> updateTagProfile() {
    return isolate.invokeRpcNoUpgrade('getTagProfile', {}).then(
      (ObservableMap map) {
        var seconds = new DateTime.now().millisecondsSinceEpoch / 1000.0;
        tagProfile._processTagProfile(seconds, map);
        return tagProfile;
      });
  }

  ObservableList<Breakpoint> breakpoints = new ObservableList();

  void _removeBreakpoint(Breakpoint bpt) {
    var script = bpt.script;
    var tokenPos = bpt.tokenPos;
    assert(tokenPos != null);
    if (script.loaded) {
      var line = script.tokenToLine(tokenPos);
      assert(line != null);
      if (script.lines[line - 1] != null) {
        assert(script.lines[line - 1].bpt == bpt);
        script.lines[line - 1].bpt = null;
      }
    }
  }

  void _addBreakpoint(Breakpoint bpt) {
    var script = bpt.script;
    var tokenPos = bpt.tokenPos;
    assert(tokenPos != null);
    if (script.loaded) {
      var line = script.tokenToLine(tokenPos);
      assert(line != null);
      assert(script.lines[line - 1].bpt == null);
      script.lines[line - 1].bpt = bpt;
    } else {
      // Load the script and then plop in the breakpoint.
      script.load().then((_) {
          _addBreakpoint(bpt);
      });
    }
  }

  void _updateBreakpoints(ServiceMap newBreakpoints) {
    // Remove all of the old breakpoints from the Script lines.
    if (breakpoints != null) {
      for (var bpt in breakpoints) {
        _removeBreakpoint(bpt);
      }
    }
    // Add all of the new breakpoints to the Script lines.
    for (var bpt in newBreakpoints['breakpoints']) {
      _addBreakpoint(bpt);
    }
    breakpoints.clear();
    breakpoints.addAll(newBreakpoints['breakpoints']);

    // Sort the breakpoints by breakpointNumber.
    breakpoints.sort((a, b) => (a.number - b.number));
  }

  Future<ServiceObject> _inProgressReloadBpts;

  Future reloadBreakpoints() {
    // TODO(turnidge): Can reusing the Future here ever cause us to
    // get stale breakpoints?
    if (_inProgressReloadBpts == null) {
      _inProgressReloadBpts =
          invokeRpc('getBreakpoints', {}).then((newBpts) {
              _updateBreakpoints(newBpts);
          }).whenComplete(() {
              _inProgressReloadBpts = null;
          });
    }
    return _inProgressReloadBpts;
  }

  Future<ServiceObject> addBreakpoint(Script script, int line) {
    // TODO(turnidge): Pass line as an int instead of a string.
    Map params = {
      'scriptId': script.id,
      'line': '$line',
    };
    return invokeRpc('addBreakpoint', params).then((result) {
      if (result is DartError) {
        return result;
      }
      Breakpoint bpt = result;
      if (bpt.resolved &&
          script.loaded &&
          script.tokenToLine(result.tokenPos) != line) {
        // Unable to set a breakpoint at desired line.
        script.lines[line - 1].possibleBpt = false;
      }
      // TODO(turnidge): Instead of reloading all of the breakpoints,
      // rely on events to update the breakpoint list.
      return reloadBreakpoints().then((_) {
        return result;
      });
    });
  }

  Future<ServiceObject> addBreakpointAtEntry(ServiceFunction function) {
    return invokeRpc('addBreakpointAtEntry',
                     { 'functionId': function.id }).then((result) {
        // TODO(turnidge): Instead of reloading all of the breakpoints,
        // rely on events to update the breakpoint list.
        return reloadBreakpoints().then((_) {
            return result;
        });
      });
  }

  Future removeBreakpoint(Breakpoint bpt) {
    return invokeRpc('removeBreakpoint',
                     { 'breakpointId': bpt.id }).then((result) {
        if (result is DartError) {
          // TODO(turnidge): Handle this more gracefully.
          Logger.root.severe(result.message);
          return result;
        }
        if (pauseEvent != null &&
            pauseEvent.breakpoint != null &&
            (pauseEvent.breakpoint.id == bpt.id)) {
          return isolate.reload();
        } else {
          return reloadBreakpoints();
        }
      });
  }

  // TODO(turnidge): If the user invokes pause (or other rpcs) twice,
  // they could get a race.  Consider returning an "in progress"
  // future to avoid this.
  Future pause() {
    return invokeRpc('pause', {}).then((result) {
        if (result is DartError) {
          // TODO(turnidge): Handle this more gracefully.
          Logger.root.severe(result.message);
        }
        return isolate.reload();
      });
  }

  Future resume() {
    return invokeRpc('resume', {}).then((result) {
        if (result is DartError) {
          // TODO(turnidge): Handle this more gracefully.
          Logger.root.severe(result.message);
        }
        return isolate.reload();
      });
  }

  Future stepInto() {
    return invokeRpc('resume', {'step': 'into'}).then((result) {
        if (result is DartError) {
          // TODO(turnidge): Handle this more gracefully.
          Logger.root.severe(result.message);
        }
        return isolate.reload();
      });
  }

  Future stepOver() {
    return invokeRpc('resume', {'step': 'over'}).then((result) {
        if (result is DartError) {
          // TODO(turnidge): Handle this more gracefully.
          Logger.root.severe(result.message);
        }
        return isolate.reload();
      });
  }

  Future stepOut() {
    return invokeRpc('resume', {'step': 'out'}).then((result) {
        if (result is DartError) {
          // TODO(turnidge): Handle this more gracefully.
          Logger.root.severe(result.message);
        }
        return isolate.reload();
      });
  }

  Future<ServiceMap> getStack() {
    return invokeRpc('getStack', {}).then((result) {
        if (result is DartError) {
          // TODO(turnidge): Handle this more gracefully.
          Logger.root.severe(result.message);
        }
        return result;
      });
  }

  Future<ServiceObject> eval(ServiceObject target,
                             String expression) {
    Map params = {
      'targetId': target.id,
      'expression': expression,
    };
    return invokeRpc('eval', params);
  }

  Future<ServiceObject> getRetainedSize(ServiceObject target) {
    Map params = {
      'targetId': target.id,
    };
    return invokeRpc('getRetainedSize', params);
  }

  Future<ServiceObject> getRetainingPath(ServiceObject target, var limit) {
    Map params = {
      'targetId': target.id,
      'limit': limit.toString(),
    };
    return invokeRpc('getRetainingPath', params);
  }

  Future<ServiceObject> getInboundReferences(ServiceObject target, var limit) {
    Map params = {
      'targetId': target.id,
      'limit': limit.toString(),
    };
    return invokeRpc('getInboundReferences', params);
  }

  Future<ServiceObject> getTypeArgumentsList(bool onlyWithInstantiations) {
    Map params = {
      'onlyWithInstantiations': onlyWithInstantiations,
    };
    return invokeRpc('getTypeArgumentsList', params);
  }

  Future<ServiceObject> getInstances(Class cls, var limit) {
    Map params = {
      'classId': cls.id,
      'limit': limit.toString(),
    };
    return invokeRpc('getInstances', params);
  }

  Future<ServiceObject> getObjectByAddress(String address, [bool ref=true]) {
    Map params = {
      'address': address,
      'ref': ref,
    };
    return invokeRpc('getObjectByAddress', params);
  }

  final ObservableMap<String, ServiceMetric> dartMetrics =
      new ObservableMap<String, ServiceMetric>();

  final ObservableMap<String, ServiceMetric> nativeMetrics =
      new ObservableMap<String, ServiceMetric>();

  Future<ObservableMap<String, ServiceMetric>> _refreshMetrics(
      String metricType,
      ObservableMap<String, ServiceMetric> metricsMap) {
    return invokeRpc('getIsolateMetricList',
                     { 'type': metricType }).then((result) {
      if (result is DartError) {
        // TODO(turnidge): Handle this more gracefully.
        Logger.root.severe(result.message);
        return null;
      }
      // Clear metrics map.
      metricsMap.clear();
      // Repopulate metrics map.
      var metrics = result['metrics'];
      for (var metric in metrics) {
        metricsMap[metric.id] = metric;
      }
      return metricsMap;
    });
  }

  Future<ObservableMap<String, ServiceMetric>> refreshDartMetrics() {
    return _refreshMetrics('Dart', dartMetrics);
  }

  Future<ObservableMap<String, ServiceMetric>> refreshNativeMetrics() {
    return _refreshMetrics('Native', nativeMetrics);
  }

  Future refreshMetrics() {
    return Future.wait([refreshDartMetrics(), refreshNativeMetrics()]);
  }

  String toString() => "Isolate($_id)";
}

/// A [ServiceObject] which implements [ObservableMap].
class ServiceMap extends ServiceObject implements ObservableMap {
  final ObservableMap _map = new ObservableMap();
  static String objectIdRingPrefix = 'objects/';

  bool get canCache {
    return (_type == 'Class' ||
            _type == 'Function' ||
            _type == 'Field') &&
           !_id.startsWith(objectIdRingPrefix);
  }
  bool get immutable => false;

  ServiceMap._empty(ServiceObjectOwner owner) : super._empty(owner);

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

    name = _map['name'];
    vmName = (_map.containsKey('vmName') ? _map['vmName'] : name);
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

  String toString() => "ServiceMap($_map)";
}

/// A [DartError] is peered to a Dart Error object.
class DartError extends ServiceObject {
  DartError._empty(ServiceObject owner) : super._empty(owner);

  @observable String kind;
  @observable String message;
  @observable Instance exception;
  @observable Instance stacktrace;

  void _update(ObservableMap map, bool mapIsRef) {
    kind = map['kind'];
    message = map['message'];
    exception = new ServiceObject._fromMap(owner, map['exception']);
    stacktrace = new ServiceObject._fromMap(owner, map['stacktrace']);
    name = 'DartError $kind';
    vmName = name;
  }

  String toString() => 'DartError($message)';
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

  String toString() => 'ServiceError($message)';
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

  String toString() => 'ServiceException($message)';
}

/// A [ServiceEvent] is an asynchronous event notification from the vm.
class ServiceEvent extends ServiceObject {
  ServiceEvent._empty(ServiceObjectOwner owner) : super._empty(owner);

  ServiceEvent.vmDisconencted() : super._empty(null) {
    eventType = 'VMDisconnected';
  }

  @observable String eventType;
  @observable Breakpoint breakpoint;
  @observable ServiceMap exception;
  @observable ByteData data;
  @observable int count;

  void _update(ObservableMap map, bool mapIsRef) {
    _loaded = true;
    _upgradeCollection(map, owner);
    eventType = map['eventType'];
    name = 'ServiceEvent $eventType';
    vmName = name;
    if (map['breakpoint'] != null) {
      breakpoint = map['breakpoint'];
    }
    if (map['exception'] != null) {
      exception = map['exception'];
    }
    if (map['_data'] != null) {
      data = map['_data'];
    }
    if (map['count'] != null) {
      count = map['count'];
    }
  }

  String toString() {
    return 'ServiceEvent of type $eventType with '
        '${data == null ? 0 : data.lengthInBytes} bytes of binary data';
  }
}

class Breakpoint extends ServiceObject {
  Breakpoint._empty(ServiceObjectOwner owner) : super._empty(owner);

  // TODO(turnidge): Add state to track if a breakpoint has been
  // removed from the program.  Remove from the cache when deleted.
  bool get canCache => true;
  bool get immutable => false;

  // A unique integer identifier for this breakpoint.
  @observable int number;

  // Source location information.
  @observable Script script;
  @observable int tokenPos;

  // The breakpoint has been assigned to a final source location.
  @observable bool resolved;

  // The breakpoint is active.
  @observable bool enabled;

  void _update(ObservableMap map, bool mapIsRef) {
    _loaded = true;
    _upgradeCollection(map, owner);

    number = map['breakpointNumber'];
    script = map['location']['script'];
    tokenPos = map['location']['tokenPos'];

    resolved = map['resolved'];
    enabled = map['enabled'];
  }

  String toString() {
    if (number != null) {
      return 'Breakpoint ${number} at ${script.name}(token:${tokenPos})';
    } else {
      return 'Uninitialized breakpoint';
    }
  }
}

class Library extends ServiceObject with Coverage {
  @observable String url;
  @reflectable final imports = new ObservableList<Library>();
  @reflectable final scripts = new ObservableList<Script>();
  @reflectable final classes = new ObservableList<Class>();
  @reflectable final variables = new ObservableList<Field>();
  @reflectable final functions = new ObservableList<ServiceFunction>();

  bool get canCache => true;
  bool get immutable => false;

  Library._empty(ServiceObjectOwner owner) : super._empty(owner);

  void _update(ObservableMap map, bool mapIsRef) {
    url = map['url'];
    var shortUrl = url;
    if (url.startsWith('file://') ||
        url.startsWith('http://')) {
      shortUrl = url.substring(url.lastIndexOf('/') + 1);
    }
    name = map['name'];
    if (name.isEmpty) {
      // When there is no name for a library, use the shortUrl.
      name = shortUrl;
    }
    vmName = (map.containsKey('vmName') ? map['vmName'] : name);
    if (mapIsRef) {
      return;
    }
    _loaded = true;
    _upgradeCollection(map, isolate);
    imports.clear();
    imports.addAll(removeDuplicatesAndSortLexical(map['imports']));
    scripts.clear();
    scripts.addAll(removeDuplicatesAndSortLexical(map['scripts']));
    classes.clear();
    classes.addAll(map['classes']);
    classes.sort(ServiceObject.LexicalSortName);
    variables.clear();
    variables.addAll(map['variables']);
    variables.sort(ServiceObject.LexicalSortName);
    functions.clear();
    functions.addAll(map['functions']);
    functions.sort(ServiceObject.LexicalSortName);
  }

  String toString() => "Library($url)";
}

class AllocationCount extends Observable {
  @observable int instances = 0;
  @observable int bytes = 0;

  void reset() {
    instances = 0;
    bytes = 0;
  }

  bool get empty => (instances == 0) && (bytes == 0);
}

class Allocations {
  // Indexes into VM provided array. (see vm/class_table.h).
  static const ALLOCATED_BEFORE_GC = 0;
  static const ALLOCATED_BEFORE_GC_SIZE = 1;
  static const LIVE_AFTER_GC = 2;
  static const LIVE_AFTER_GC_SIZE = 3;
  static const ALLOCATED_SINCE_GC = 4;
  static const ALLOCATED_SINCE_GC_SIZE = 5;
  static const ACCUMULATED = 6;
  static const ACCUMULATED_SIZE = 7;

  final AllocationCount accumulated = new AllocationCount();
  final AllocationCount current = new AllocationCount();

  void update(List stats) {
    accumulated.instances = stats[ACCUMULATED];
    accumulated.bytes = stats[ACCUMULATED_SIZE];
    current.instances = stats[LIVE_AFTER_GC] + stats[ALLOCATED_SINCE_GC];
    current.bytes = stats[LIVE_AFTER_GC_SIZE] + stats[ALLOCATED_SINCE_GC_SIZE];
  }

  bool get empty => accumulated.empty && current.empty;
}

class Class extends ServiceObject with Coverage {
  @observable Library library;
  @observable Script script;

  @observable bool isAbstract;
  @observable bool isConst;
  @observable bool isFinalized;
  @observable bool isPatch;
  @observable bool isImplemented;

  @observable int tokenPos;
  @observable int endTokenPos;

  @observable ServiceMap error;
  @observable int vmCid;

  final Allocations newSpace = new Allocations();
  final Allocations oldSpace = new Allocations();
  final AllocationCount promotedByLastNewGC = new AllocationCount();

  bool get hasNoAllocations => newSpace.empty && oldSpace.empty;

  @reflectable final fields = new ObservableList<Field>();
  @reflectable final functions = new ObservableList<ServiceFunction>();

  @observable Class superclass;
  @reflectable final interfaces = new ObservableList<Class>();
  @reflectable final subclasses = new ObservableList<Class>();

  bool get canCache => true;
  bool get immutable => false;

  Class._empty(ServiceObjectOwner owner) : super._empty(owner);

  void _update(ObservableMap map, bool mapIsRef) {
    name = map['name'];
    vmName = (map.containsKey('vmName') ? map['vmName'] : name);
    var idPrefix = "classes/";
    assert(id.startsWith(idPrefix));
    vmCid = int.parse(id.substring(idPrefix.length));

    if (mapIsRef) {
      return;
    }

    // We are fully loaded.
    _loaded = true;

    // Extract full properties.
    _upgradeCollection(map, isolate);

    // Some builtin classes aren't associated with a library.
    if (map['library'] is Library) {
      library = map['library'];
    } else {
      library = null;
    }

    script = map['script'];

    isAbstract = map['abstract'];
    isConst = map['const'];
    isFinalized = map['finalized'];
    isPatch = map['patch'];
    isImplemented = map['implemented'];

    tokenPos = map['tokenPos'];
    endTokenPos = map['endTokenPos'];

    subclasses.clear();
    subclasses.addAll(map['subclasses']);
    subclasses.sort(ServiceObject.LexicalSortName);

    fields.clear();
    fields.addAll(map['fields']);
    fields.sort(ServiceObject.LexicalSortName);

    functions.clear();
    functions.addAll(map['functions']);
    functions.sort(ServiceObject.LexicalSortName);

    superclass = map['super'];
    // Work-around Object not tracking its subclasses in the VM.
    if (superclass != null && superclass.name == "Object") {
      superclass._addSubclass(this);
    }
    error = map['error'];

    var allocationStats = map['allocationStats'];
    if (allocationStats != null) {
      newSpace.update(allocationStats['new']);
      oldSpace.update(allocationStats['old']);
      promotedByLastNewGC.instances = allocationStats['promotedInstances'];
      promotedByLastNewGC.bytes = allocationStats['promotedBytes'];
    }
  }

  void _addSubclass(Class subclass) {
    if (subclasses.contains(subclass)) {
      return;
    }
    subclasses.add(subclass);
    subclasses.sort(ServiceObject.LexicalSortName);
  }

  String toString() => 'Class($vmName)';
}

class Instance extends ServiceObject {
  @observable Class clazz;
  @observable int size;
  @observable int retainedSize;
  @observable String valueAsString;  // If primitive.
  @observable bool valueAsStringIsTruncated;
  @observable ServiceFunction closureFunc;  // If a closure.
  @observable Context closureCtxt;  // If a closure.
  @observable String name;  // If a Type.
  @observable int length; // If a List.

  @observable var typeClass;
  @observable var fields;
  @observable var nativeFields;
  @observable var elements;
  @observable var userName;
  @observable var referent;  // If a MirrorReference.
  @observable Instance key;  // If a WeakProperty.
  @observable Instance value;  // If a WeakProperty.

  bool get isClosure => closureFunc != null;

  Instance._empty(ServiceObjectOwner owner) : super._empty(owner);

  void _update(ObservableMap map, bool mapIsRef) {
    // Extract full properties.
    _upgradeCollection(map, isolate);

    clazz = map['class'];
    size = map['size'];
    valueAsString = map['valueAsString'];
    // Coerce absence to false.
    valueAsStringIsTruncated = map['valueAsStringIsTruncated'] == true;
    closureFunc = map['closureFunc'];
    closureCtxt = map['closureCtxt'];
    name = map['name'];
    length = map['length'];

    if (mapIsRef) {
      return;
    }

    nativeFields = map['nativeFields'];
    fields = map['fields'];
    elements = map['elements'];
    typeClass = map['type_class'];
    userName = map['user_name'];
    referent = map['referent'];
    key = map['key'];
    value = map['value'];

    // We are fully loaded.
    _loaded = true;
  }

  String get shortName => valueAsString != null ? valueAsString : 'a ${clazz.name}';

  String toString() => 'Instance($shortName)';
}


class Context extends ServiceObject {
  @observable Class clazz;
  @observable int size;

  @observable var parentContext;
  @observable int length;
  @observable var variables;

  Context._empty(ServiceObjectOwner owner) : super._empty(owner);

  void _update(ObservableMap map, bool mapIsRef) {
    // Extract full properties.
    _upgradeCollection(map, isolate);

    size = map['size'];
    length = map['length'];
    parentContext = map['parent'];

    if (mapIsRef) {
      return;
    }

    clazz = map['class'];
    variables = map['variables'];

    // We are fully loaded.
    _loaded = true;
  }

  String toString() => 'Context($length)';
}


// TODO(koda): Sync this with VM.
class FunctionKind {
  final String _strValue;
  FunctionKind._internal(this._strValue);
  toString() => _strValue;
  bool isSynthetic() => [kCollected, kNative, kTag, kReused].contains(this);

  static FunctionKind fromJSON(String value) {
    switch(value) {
      case 'kRegularFunction': return kRegularFunction;
      case 'kClosureFunction': return kClosureFunction;
      case 'kGetterFunction': return kGetterFunction;
      case 'kSetterFunction': return kSetterFunction;
      case 'kConstructor': return kConstructor;
      case 'kImplicitGetter': return kImplicitGetterFunction;
      case 'kImplicitSetter': return kImplicitSetterFunction;
      case 'kStaticInitializer': return kStaticInitializer;
      case 'kMethodExtractor': return kMethodExtractor;
      case 'kNoSuchMethodDispatcher': return kNoSuchMethodDispatcher;
      case 'kInvokeFieldDispatcher': return kInvokeFieldDispatcher;
      case 'Collected': return kCollected;
      case 'Native': return kNative;
      case 'Tag': return kTag;
      case 'Reused': return kReused;
    }
    return kUNKNOWN;
  }

  static FunctionKind kRegularFunction = new FunctionKind._internal('function');
  static FunctionKind kClosureFunction = new FunctionKind._internal('closure function');
  static FunctionKind kGetterFunction = new FunctionKind._internal('getter function');
  static FunctionKind kSetterFunction = new FunctionKind._internal('setter function');
  static FunctionKind kConstructor = new FunctionKind._internal('constructor');
  static FunctionKind kImplicitGetterFunction = new FunctionKind._internal('implicit getter function');
  static FunctionKind kImplicitSetterFunction = new FunctionKind._internal('implicit setter function');
  static FunctionKind kStaticInitializer = new FunctionKind._internal('static initializer');
  static FunctionKind kMethodExtractor = new FunctionKind._internal('method extractor');
  static FunctionKind kNoSuchMethodDispatcher = new FunctionKind._internal('noSuchMethod dispatcher');
  static FunctionKind kInvokeFieldDispatcher = new FunctionKind._internal('invoke field dispatcher');
  static FunctionKind kCollected = new FunctionKind._internal('Collected');
  static FunctionKind kNative = new FunctionKind._internal('Native');
  static FunctionKind kTag = new FunctionKind._internal('Tag');
  static FunctionKind kReused = new FunctionKind._internal('Reused');
  static FunctionKind kUNKNOWN = new FunctionKind._internal('UNKNOWN');
}

class ServiceFunction extends ServiceObject with Coverage {
  @observable Class owningClass;
  @observable Library owningLibrary;
  @observable bool isStatic;
  @observable bool isConst;
  @observable ServiceFunction parent;
  @observable Script script;
  @observable int tokenPos;
  @observable int endTokenPos;
  @observable Code code;
  @observable Code unoptimizedCode;
  @observable bool isOptimizable;
  @observable bool isInlinable;
  @observable FunctionKind kind;
  @observable int deoptimizations;
  @observable String qualifiedName;
  @observable int usageCounter;
  @observable bool isDart;
  @observable ProfileFunction profile;

  bool get canCache => true;
  bool get immutable => false;

  ServiceFunction._empty(ServiceObject owner) : super._empty(owner);

  void _update(ObservableMap map, bool mapIsRef) {
    name = map['name'];
    vmName = (map.containsKey('vmName') ? map['vmName'] : name);

    _upgradeCollection(map, isolate);

    owningClass = map.containsKey('owningClass') ? map['owningClass'] : null;
    owningLibrary = map.containsKey('owningLibrary') ? map['owningLibrary'] : null;
    kind = FunctionKind.fromJSON(map['kind']);
    isDart = !kind.isSynthetic();

    if (parent == null) {
      qualifiedName = (owningClass != null) ?
          "${owningClass.name}.${name}" :
          name;
    } else {
      qualifiedName = "${parent.qualifiedName}.${name}";
    }

    if (mapIsRef) {
      return;
    }

    isStatic = map['static'];
    isConst = map['const'];
    parent = map['parent'];
    script = map['script'];
    tokenPos = map['tokenPos'];
    endTokenPos = map['endTokenPos'];
    code = _convertNull(map['code']);
    unoptimizedCode = _convertNull(map['unoptimizedCode']);
    isOptimizable = map['optimizable'];
    isInlinable = map['inlinable'];
    deoptimizations = map['deoptimizations'];
    usageCounter = map['usageCounter'];
  }
}


class Field extends ServiceObject {
  @observable var /* Library or Class */ owner;
  @observable Instance declaredType;
  @observable bool isStatic;
  @observable bool isFinal;
  @observable bool isConst;
  @observable Instance value;
  @observable String name;
  @observable String vmName;

  @observable bool guardNullable;
  @observable String guardClass;
  @observable String guardLength;
  @observable Script script;
  @observable int tokenPos;

  Field._empty(ServiceObjectOwner owner) : super._empty(owner);

  void _update(ObservableMap map, bool mapIsRef) {
    // Extract full properties.
    _upgradeCollection(map, isolate);

    name = map['name'];
    vmName = (map.containsKey('vmName') ? map['vmName'] : name);
    owner = map['owner'];
    declaredType = map['declaredType'];
    isStatic = map['static'];
    isFinal = map['final'];
    isConst = map['const'];
    value = map['value'];

    if (mapIsRef) {
      return;
    }

    guardNullable = map['guardNullable'];
    guardClass = map['guardClass'];
    guardLength = map['guardLength'];
    script = map['script'];
    tokenPos = map['tokenPos'];

    _loaded = true;
  }

  String toString() => 'Field(${owner.name}.$name)';
}


class ScriptLine extends Observable {
  final Script script;
  final int line;
  final String text;
  @observable int hits;
  @observable Breakpoint bpt;
  @observable bool possibleBpt = true;

  bool get isBlank {
    // Compute isBlank on demand.
    if (_isBlank == null) {
      _isBlank = text.trim().isEmpty;
    }
    return _isBlank;
  }
  bool _isBlank;

  static bool _isTrivialToken(String token) {
    if (token == 'else') {
      return true;
    }
    for (var c in token.split('')) {
      switch (c) {
        case '{':
        case '}':
        case '(':
        case ')':
        case ';':
          break;
        default:
          return false;
      }
    }
    return true;
  }

  static bool _isTrivialLine(String text) {
    var wsTokens = text.split(new RegExp(r"(\s)+"));
    for (var wsToken in wsTokens) {
      var tokens = wsToken.split(new RegExp(r"(\b)"));
      for (var token in tokens) {
        if (!_isTrivialToken(token)) {
          return false;
        }
      }
    }
    return true;
  }

  ScriptLine(this.script, this.line, this.text) {
    possibleBpt = !_isTrivialLine(text);

    // TODO(turnidge): This is not so efficient.  Consider improving.
    for (var bpt in this.script.isolate.breakpoints) {
      if (bpt.script == this.script &&
          bpt.script.tokenToLine(bpt.tokenPos) == line) {
        this.bpt = bpt;
      }
    }
  }
}

class Script extends ServiceObject with Coverage {
  final lines = new ObservableList<ScriptLine>();
  final _hits = new Map<int, int>();
  @observable String kind;
  @observable int firstTokenPos;
  @observable int lastTokenPos;
  @observable Library owningLibrary;
  bool get canCache => true;
  bool get immutable => true;

  String _shortUrl;
  String _url;

  Script._empty(ServiceObjectOwner owner) : super._empty(owner);

  ScriptLine getLine(int line) {
    assert(line >= 1);
    return lines[line - 1];
  }

  /// This function maps a token position to a line number.
  int tokenToLine(int token) => _tokenToLine[token];
  Map _tokenToLine = {};

  /// This function maps a token position to a column number.
  int tokenToCol(int token) => _tokenToCol[token];
  Map _tokenToCol = {};

  void _update(ObservableMap map, bool mapIsRef) {
    _upgradeCollection(map, isolate);
    kind = map['kind'];
    _url = map['name'];
    _shortUrl = _url.substring(_url.lastIndexOf('/') + 1);
    name = _shortUrl;
    vmName = _url;
    if (mapIsRef) {
      return;
    }
    _processSource(map['source']);
    _parseTokenPosTable(map['tokenPosTable']);
    owningLibrary = map['owningLibrary'];
  }

  void _parseTokenPosTable(List<List<int>> table) {
    if (table == null) {
      return;
    }
    _tokenToLine.clear();
    _tokenToCol.clear();
    firstTokenPos = null;
    lastTokenPos = null;
    var lineSet = new Set();

    for (var line in table) {
      // Each entry begins with a line number...
      var lineNumber = line[0];
      lineSet.add(lineNumber);
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

    for (var line in lines) {
      // Remove possible breakpoints on lines with no tokens.
      if (!lineSet.contains(line.line)) {
        line.possibleBpt = false;
      }
    }
  }

  void _processHits(List scriptHits) {
    // Update hits table.
    for (var i = 0; i < scriptHits.length; i += 2) {
      var line = scriptHits[i];
      var hit = scriptHits[i + 1]; // hit status.
      assert(line >= 1); // Lines start at 1.
      var oldHits = _hits[line];
      if (oldHits != null) {
        hit += oldHits;
      }
      _hits[line] = hit;
    }
    _applyHitsToLines();
    // Notify any Observers that this Script's state has changed.
    notifyChange(null);
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
      lines.add(new ScriptLine(this, i + 1, sourceLines[i]));
    }
    _applyHitsToLines();
    // Notify any Observers that this Script's state has changed.
    notifyChange(null);
  }

  void _applyHitsToLines() {
    for (var line in lines) {
      var hits = _hits[line.line];
      line.hits = hits;
    }
  }
}

class PcDescriptor extends Observable {
  final int pcOffset;
  @reflectable final int deoptId;
  @reflectable final int tokenPos;
  @reflectable final int tryIndex;
  @reflectable final String kind;
  @observable Script script;
  @observable String formattedLine;
  PcDescriptor(this.pcOffset, this.deoptId, this.tokenPos, this.tryIndex,
               this.kind);

  @reflectable String formattedDeoptId() {
    if (deoptId == -1) {
      return 'N/A';
    }
    return deoptId.toString();
  }

  @reflectable String formattedTokenPos() {
    if (tokenPos == -1) {
      return '';
    }
    return tokenPos.toString();
  }

  void processScript(Script script) {
    this.script = null;
    if (tokenPos == -1) {
      return;
    }
    var line = script.tokenToLine(tokenPos);
    if (line == null) {
      return;
    }
    this.script = script;
    var scriptLine = script.getLine(line);
    formattedLine = scriptLine.text;
  }
}

class PcDescriptors extends ServiceObject {
  @observable Class clazz;
  @observable int size;
  bool get canCache => false;
  bool get immutable => true;
  @reflectable final List<PcDescriptor> descriptors =
      new ObservableList<PcDescriptor>();

  PcDescriptors._empty(ServiceObjectOwner owner) : super._empty(owner) {
  }

  void _update(ObservableMap m, bool mapIsRef) {
    if (mapIsRef) {
      return;
    }
    _upgradeCollection(m, isolate);
    clazz = m['class'];
    size = m['size'];
    descriptors.clear();
    for (var descriptor in m['members']) {
      var pcOffset = int.parse(descriptor['pcOffset'], radix:16);
      var deoptId = descriptor['deoptId'];
      var tokenPos = descriptor['tokenPos'];
      var tryIndex = descriptor['tryIndex'];
      var kind = descriptor['kind'].trim();
      descriptors.add(
          new PcDescriptor(pcOffset, deoptId, tokenPos, tryIndex, kind));
    }
  }
}

class LocalVarDescriptor extends Observable {
  @reflectable final String name;
  @reflectable final int index;
  @reflectable final int beginPos;
  @reflectable final int endPos;
  @reflectable final int scopeId;
  @reflectable final String kind;

  LocalVarDescriptor(this.name, this.index, this.beginPos, this.endPos,
                     this.scopeId, this.kind);
}

class LocalVarDescriptors extends ServiceObject {
  @observable Class clazz;
  @observable int size;
  bool get canCache => false;
  bool get immutable => true;
  @reflectable final List<LocalVarDescriptor> descriptors =
        new ObservableList<LocalVarDescriptor>();
  LocalVarDescriptors._empty(ServiceObjectOwner owner) : super._empty(owner);

  void _update(ObservableMap m, bool mapIsRef) {
    if (mapIsRef) {
      return;
    }
    _upgradeCollection(m, isolate);
    clazz = m['class'];
    size = m['size'];
    descriptors.clear();
    for (var descriptor in m['members']) {
      var name = descriptor['name'];
      var index = descriptor['index'];
      var beginPos = descriptor['beginPos'];
      var endPos = descriptor['endPos'];
      var scopeId = descriptor['scopeId'];
      var kind = descriptor['kind'].trim();
      descriptors.add(
          new LocalVarDescriptor(name, index, beginPos, endPos, scopeId, kind));
    }
  }
}

class TokenStream extends ServiceObject {
  @observable Class clazz;
  @observable int size;
  bool get canCache => false;
  bool get immutable => true;

  @observable String privateKey;

  TokenStream._empty(ServiceObjectOwner owner) : super._empty(owner);

  void _update(ObservableMap m, bool mapIsRef) {
    if (mapIsRef) {
      return;
    }
    _upgradeCollection(m, isolate);
    clazz = m['class'];
    size = m['size'];
    privateKey = m['privateKey'];
  }
}

class CodeInstruction extends Observable {
  @observable final int address;
  @observable final int pcOffset;
  @observable final String machine;
  @observable final String human;
  @observable CodeInstruction jumpTarget;
  @reflectable List<PcDescriptor> descriptors =
      new ObservableList<PcDescriptor>();

  CodeInstruction(this.address, this.pcOffset, this.machine, this.human);

  @reflectable bool get isComment => address == 0;
  @reflectable bool get hasDescriptors => descriptors.length > 0;

  bool _isJumpInstruction() {
    return human.startsWith('j');
  }

  int _getJumpAddress() {
    assert(_isJumpInstruction());
    var chunks = human.split(' ');
    if (chunks.length != 2) {
      // We expect jump instructions to be of the form 'j.. address'.
      return 0;
    }
    var address = chunks[1];
    if (address.startsWith('0x')) {
      // Chop off the 0x.
      address = address.substring(2);
    }
    try {
      return int.parse(address, radix:16);
    } catch (_) {
      return 0;
    }
  }

  void _resolveJumpTarget(List<CodeInstruction> instructions) {
    if (!_isJumpInstruction()) {
      return;
    }
    int address = _getJumpAddress();
    if (address == 0) {
      return;
    }
    for (var i = 0; i < instructions.length; i++) {
      var instruction = instructions[i];
      if (instruction.address == address) {
        jumpTarget = instruction;
        return;
      }
    }
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

class CodeInlineInterval {
  final int start;
  final int end;
  final List<ServiceFunction> functions = new List<ServiceFunction>();
  bool contains(int pc) => (pc >= start) && (pc < end);
  CodeInlineInterval(this.start, this.end);
}

class Code extends ServiceObject {
  @observable CodeKind kind;
  @observable Instance objectPool;
  @observable ServiceFunction function;
  @observable Script script;
  @observable bool isOptimized = false;
  @reflectable int startAddress = 0;
  @reflectable int endAddress = 0;
  @reflectable final instructions = new ObservableList<CodeInstruction>();
  @observable ProfileCode profile;
  final List<CodeInlineInterval> inlineIntervals =
      new List<CodeInlineInterval>();
  final ObservableList<ServiceFunction> inlinedFunctions =
      new ObservableList<ServiceFunction>();
  bool get canCache => true;
  bool get immutable => true;

  Code._empty(ServiceObjectOwner owner) : super._empty(owner);

  void _updateDescriptors(Script script) {
    this.script = script;
    for (var instruction in instructions) {
      for (var descriptor in instruction.descriptors) {
        descriptor.processScript(script);
      }
    }
  }

  void loadScript() {
    if (script != null) {
      // Already done.
      return;
    }
    if (kind != CodeKind.Dart){
      return;
    }
    if (function == null) {
      return;
    }
    if (function.script == null) {
      // Attempt to load the function.
      function.load().then((func) {
        var script = function.script;
        if (script == null) {
          // Function doesn't have an associated script.
          return;
        }
        // Load the script and then update descriptors.
        script.load().then(_updateDescriptors);
      });
      return;
    }
    // Load the script and then update descriptors.
    function.script.load().then(_updateDescriptors);
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

  void _update(ObservableMap m, bool mapIsRef) {
    name = m['name'];
    vmName = (m.containsKey('vmName') ? m['vmName'] : name);
    isOptimized = m['optimized'] != null ? m['optimized'] : false;
    kind = CodeKind.fromString(m['kind']);
    startAddress = int.parse(m['start'], radix:16);
    endAddress = int.parse(m['end'], radix:16);
    function = isolate.getFromMap(m['function']);
    if (mapIsRef) {
      return;
    }
    _loaded = true;
    objectPool = isolate.getFromMap(m['objectPool']);
    var disassembly = m['disassembly'];
    if (disassembly != null) {
      _processDisassembly(disassembly);
    }
    var descriptors = m['descriptors'];
    if (descriptors != null) {
      descriptors = descriptors['members'];
      _processDescriptors(descriptors);
    }
    hasDisassembly = (instructions.length != 0) && (kind == CodeKind.Dart);
    inlinedFunctions.clear();
    var inlinedFunctionsTable = m['inlinedFunctions'];
    var inlinedIntervals = m['inlinedIntervals'];
    if (inlinedFunctionsTable != null) {
      // Iterate and upgrade each ServiceFunction.
      for (var i = 0; i < inlinedFunctionsTable.length; i++) {
        // Upgrade each function and set it back in the list.
        var func = isolate.getFromMap(inlinedFunctionsTable[i]);
        inlinedFunctionsTable[i] = func;
        if (!inlinedFunctions.contains(func)) {
          inlinedFunctions.add(func);
        }
      }
    }
    if ((inlinedIntervals == null) || (inlinedFunctionsTable == null)) {
      // No inline information.
      inlineIntervals.clear();
      return;
    }
    _processInline(inlinedFunctionsTable, inlinedIntervals);
  }

  CodeInlineInterval findInterval(int pc) {
    for (var i = 0; i < inlineIntervals.length; i++) {
      var interval = inlineIntervals[i];
      if (interval.contains(pc)) {
        return interval;
      }
    }
    return null;
  }

  void _processInline(List<ServiceFunction> inlinedFunctionsTable,
                      List<List<int>> inlinedIntervals) {
    for (var i = 0; i < inlinedIntervals.length; i++) {
      var inlinedInterval = inlinedIntervals[i];
      var start = inlinedInterval[0] + startAddress;
      var end = inlinedInterval[1] + startAddress;
      var codeInlineInterval = new CodeInlineInterval(start, end);
      for (var i = 2; i < inlinedInterval.length - 1; i++) {
        var inline_id = inlinedInterval[i];
        if (inline_id < 0) {
          continue;
        }
        var function = inlinedFunctionsTable[inline_id];
        codeInlineInterval.functions.add(function);
      }
      inlineIntervals.add(codeInlineInterval);
    }
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
      var pcOffset = 0;
      if (disassembly[i] != '') {
        // Not a code comment, extract address.
        address = int.parse(disassembly[i]);
        pcOffset = address - startAddress;
      }
      var instruction = new CodeInstruction(address, pcOffset, machine, human);
      instructions.add(instruction);
    }
    for (var instruction in instructions) {
      instruction._resolveJumpTarget(instructions);
    }
  }

  void _processDescriptor(Map d) {
    var pcOffset = int.parse(d['pcOffset'], radix:16);
    var address = startAddress + pcOffset;
    var deoptId = d['deoptId'];
    var tokenPos = d['tokenPos'];
    var tryIndex = d['tryIndex'];
    var kind = d['kind'].trim();
    for (var instruction in instructions) {
      if (instruction.address == address) {
        instruction.descriptors.add(new PcDescriptor(pcOffset,
                                                     deoptId,
                                                     tokenPos,
                                                     tryIndex,
                                                     kind));
        return;
      }
    }
    Logger.root.warning(
        'Could not find instruction with pc descriptor address: $address');
  }

  void _processDescriptors(List<Map> descriptors) {
    for (Map descriptor in descriptors) {
      _processDescriptor(descriptor);
    }
  }

  /// Returns true if [address] is contained inside [this].
  bool contains(int address) {
    return (address >= startAddress) && (address < endAddress);
  }

  @reflectable bool get isDartCode => kind == CodeKind.Dart;
}


class SocketKind {
  final _value;
  const SocketKind._internal(this._value);
  String toString() => '$_value';

  static SocketKind fromString(String s) {
    if (s == 'Listening') {
      return Listening;
    } else if (s == 'Normal') {
      return Normal;
    } else if (s == 'Pipe') {
      return Pipe;
    } else if (s == 'Internal') {
      return Internal;
    }
    Logger.root.warning('Unknown socket kind $s');
    throw new FallThroughError();
  }
  static const Listening = const SocketKind._internal('Listening');
  static const Normal = const SocketKind._internal('Normal');
  static const Pipe = const SocketKind._internal('Pipe');
  static const Internal = const SocketKind._internal('Internal');
}

/// A snapshot of statistics associated with a [Socket].
class SocketStats {
  @reflectable final int bytesRead;
  @reflectable final int bytesWritten;
  @reflectable final int readCalls;
  @reflectable final int writeCalls;
  @reflectable final int available;

  SocketStats(this.bytesRead, this.bytesWritten,
              this.readCalls, this.writeCalls,
              this.available);
}

/// A peer to a Socket in dart:io. Sockets can represent network sockets or
/// OS pipes. Each socket is owned by another ServceObject, for example,
/// a process or an HTTP server.
class Socket extends ServiceObject {
  Socket._empty(ServiceObjectOwner owner) : super._empty(owner);

  bool get canCache => true;

  ServiceObject socketOwner;

  @reflectable bool get isPipe => (kind == SocketKind.Pipe);

  @observable SocketStats latest;
  @observable SocketStats previous;

  @observable SocketKind kind;

  @observable String protocol = '';

  @observable bool readClosed = false;
  @observable bool writeClosed = false;
  @observable bool closing = false;

  /// Listening for connections.
  @observable bool listening = false;

  @observable int fd;

  @observable String localAddress;
  @observable int localPort;
  @observable String remoteAddress;
  @observable int remotePort;

  // Updates internal state from [map]. [map] can be a reference.
  void _update(ObservableMap map, bool mapIsRef) {
    name = map['name'];
    vmName = map['name'];

    kind = SocketKind.fromString(map['kind']);

    if (mapIsRef) {
      return;
    }

    _loaded = true;

    _upgradeCollection(map, isolate);

    readClosed = map['readClosed'];
    writeClosed = map['writeClosed'];
    closing = map['closing'];
    listening = map['listening'];

    protocol = map['protocol'];

    localAddress = map['localAddress'];
    localPort = map['localPort'];
    remoteAddress = map['remoteAddress'];
    remotePort = map['remotePort'];

    fd = map['fd'];
    socketOwner = map['owner'];
  }
}

class MetricSample {
  final double value;
  final DateTime time;
  MetricSample(this.value) : time = new DateTime.now();
}

class ServiceMetric extends ServiceObject {
  ServiceMetric._empty(ServiceObjectOwner owner) : super._empty(owner) {
  }

  bool get canCache => true;
  bool get immutable => false;

  @observable bool recording = false;
  MetricPoller poller;

  final ObservableList<MetricSample> samples =
      new ObservableList<MetricSample>();
  int _sampleBufferSize = 100;
  int get sampleBufferSize => _sampleBufferSize;
  set sampleBufferSize(int size) {
    _sampleBufferSize = size;
    _removeOld();
  }

  Future<ObservableMap> _fetchDirect() {
    assert(owner is Isolate);
    return isolate.invokeRpcNoUpgrade('getIsolateMetric', { 'metricId': id });
  }


  void addSample(MetricSample sample) {
    samples.add(sample);
    _removeOld();
  }

  void _removeOld() {
    // TODO(johnmccutchan): If this becomes hot, consider using a circular
    // buffer.
    if (samples.length > _sampleBufferSize) {
      int count = samples.length - _sampleBufferSize;
      samples.removeRange(0, count);
    }
  }

  @observable String description;
  @observable double value = 0.0;
  // Only a guage has a non-null min and max.
  @observable double min;
  @observable double max;

  bool get isGauge => (min != null) && (max != null);

  void _update(ObservableMap map, bool mapIsRef) {
    name = map['name'];
    description = map['description'];
    vmName = map['name'];
    value = map['value'];
    min = map['min'];
    max = map['max'];
  }

  String toString() => "ServiceMetric($_id)";
}

class MetricPoller {
  // Metrics to be polled.
  final List<ServiceMetric> metrics = new List<ServiceMetric>();
  final Duration pollPeriod;
  Timer _pollTimer;

  MetricPoller(int milliseconds) :
      pollPeriod = new Duration(milliseconds: milliseconds) {
    start();
  }

  void start() {
    _pollTimer = new Timer.periodic(pollPeriod, _onPoll);
  }

  void cancel() {
    if (_pollTimer != null) {
      _pollTimer.cancel();
    }
    _pollTimer = null;
  }

  void _onPoll(_) {
    // Reload metrics and add a sample to each.
    for (var metric in metrics) {
      metric.reload().then((m) {
        m.addSample(new MetricSample(m.value));
      });
    }
  }
}

// Convert any ServiceMaps representing a null instance into an actual null.
_convertNull(obj) {
  if (obj.isNull) {
    return null;
  }
  return obj;
}

// Returns true if [map] is a service map. i.e. it has the following keys:
// 'id' and a 'type'.
bool _isServiceMap(ObservableMap m) {
  return (m != null) && (m['type'] != null);
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
