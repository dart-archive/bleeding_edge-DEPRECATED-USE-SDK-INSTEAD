// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library route.client;

import 'dart:async';
import 'dart:collection';
import 'dart:html';

import 'package:logging/logging.dart';

import 'url_matcher.dart';
export 'url_matcher.dart';
import 'url_template.dart';


final _logger = new Logger('route');

typedef RoutePreEnterEventHandler(RoutePreEnterEvent path);
typedef RouteEnterEventHandler(RouteEnterEvent path);
typedef RouteLeaveEventHandler(RouteLeaveEvent path);

/**
 * A helper Router handle that scopes all route event subsriptions to it's
 * instance and provides an convinience [discard] method.
 */
class RouteHandle implements Route {
  Route _route;
  final StreamController<RoutePreEnterEvent> _onPreEnterController;
  final StreamController<RouteEnterEvent> _onEnterController;
  final StreamController<RouteLeaveEvent> _onLeaveController;

  @deprecated
  Stream<RouteEnterEvent> get onRoute => onEnter;
  Stream<RoutePreEnterEvent> get onPreEnter => _onPreEnterController.stream;
  Stream<RouteEnterEvent> get onEnter => _onEnterController.stream;
  Stream<RouteLeaveEvent> get onLeave => _onLeaveController.stream;

  StreamSubscription _onPreEnterSubscription;
  StreamSubscription _onEnterSubscription;
  StreamSubscription _onLeaveSubscription;
  List<RouteHandle> _childHandles = <RouteHandle>[];

  RouteHandle._new(Route this._route)
      : _onEnterController =
            new StreamController<RouteEnterEvent>.broadcast(sync: true),
        _onPreEnterController =
            new StreamController<RoutePreEnterEvent>.broadcast(sync: true),
        _onLeaveController =
            new StreamController<RouteLeaveEvent>.broadcast(sync: true) {
    _onEnterSubscription = _route.onEnter.listen(_onEnterController.add);
    _onPreEnterSubscription =
        _route.onPreEnter.listen(_onPreEnterController.add);
    _onLeaveSubscription = _route.onLeave.listen(_onLeaveController.add);
  }

  /// discards this handle.
  void discard() {
    _logger.finest('discarding handle for $_route');
    _onPreEnterSubscription.cancel();
    _onEnterSubscription.cancel();
    _onLeaveSubscription.cancel();
    _onEnterController.close();
    _onLeaveController.close();
    _childHandles.forEach((RouteHandle c) => c.discard());
    _childHandles.clear();
    _route = null;
  }

  /// Not supported. Overridden to throw an error.
  void addRoute({String name, Pattern path, bool defaultRoute: false,
    RouteEnterEventHandler enter, RoutePreEnterEventHandler preEnter,
    RouteLeaveEventHandler leave, mount}) =>
          throw new UnsupportedError('addRoute is not supported in handle');

  /// See [Route.getRoute]
  Route getRoute(String routePath) {
    Route r = _assertState(() => _getHost(_route).getRoute(routePath));
    if (r == null) return null;
    var handle = r.newHandle();
    if (handle != null) {
      _childHandles.add(handle);
    }
    return handle;
  }

  /**
   * Create an return a new [RouteHandle] for this route.
   */
  RouteHandle newHandle() {
    _logger.finest('newHandle for $this');
    return new RouteHandle._new(_getHost(_route));
  }

  Route _getHost(Route r) {
    _assertState();
    if (r == null) {
      throw new StateError('Oops?!');
    }
    if ((r is Route) && !(r is RouteHandle)) {
      return r;
    }
    RouteHandle rh = r;
    return rh._getHost(rh._route);
  }

  /// See [Route.reverse]
  String reverse(String tail) =>
      _assertState(() => _getHost(_route).reverse(tail));

  _assertState([f()]) {
    if (_route == null) {
      throw new StateError('This route handle is already discated.');
    }
    if (f != null)  return f();
  }

  /// See [Route.isActive]
  bool get isActive => _route.isActive;

  /// See [Route.parameters]
  Map get parameters => _route.parameters;

  /// See [Route.path]
  UrlMatcher get path => _route.path;

  /// See [Route.name]
  String get name => _route.name;

  /// See [Route.parent]
  Route get parent => _route.parent;
}

childRoute({String name, Pattern path, bool defaultRoute: false,
    RouteEnterEventHandler enter, RoutePreEnterEventHandler preEnter,
    RouteLeaveEventHandler leave, mount}) => (Route route) =>
        route.addRoute(name: name, path: path, defaultRoute: defaultRoute,
            enter: enter, preEnter: preEnter, leave: leave, mount: leave);

/**
 * Route is a node in the tree of routes. The edge leading to the route is
 * defined by path.
 */
class Route {
  final String name;
  final Map<String, Route> _routes = new LinkedHashMap<String, Route>();
  final UrlMatcher path;
  final StreamController<RouteEnterEvent> _onEnterController;
  final StreamController<RoutePreEnterEvent> _onPreEnterController;
  final StreamController<RouteLeaveEvent> _onLeaveController;
  final Route parent;
  Route _defaultRoute;
  Route _currentRoute;
  RouteEvent _lastEvent;

  @deprecated
  Stream<RouteEvent> get onRoute => onEnter;

  Stream<RouteEvent> get onPreEnter => _onPreEnterController.stream;
  Stream<RouteEvent> get onLeave => _onLeaveController.stream;
  Stream<RouteEvent> get onEnter => _onEnterController.stream;

  Route._new({this.name, this.path, this.parent})
      : _onEnterController =
            new StreamController<RouteEnterEvent>.broadcast(sync: true),
        _onPreEnterController =
            new StreamController<RoutePreEnterEvent>.broadcast(sync: true),
        _onLeaveController =
            new StreamController<RouteLeaveEvent>.broadcast(sync: true);

  void addRoute({String name, Pattern path, bool defaultRoute: false,
      RouteEnterEventHandler enter, RoutePreEnterEventHandler preEnter,
      RouteLeaveEventHandler leave, mount}) {
    if (name == null) {
      throw new ArgumentError('name is required for all routes');
    }
    if (_routes.containsKey(name)) {
      throw new ArgumentError('Route $name already exists');
    }

    var matcher;
    if (!(path is UrlMatcher)) {
      matcher = new UrlTemplate(path.toString());
    } else {
      matcher = path;
    }
    var route = new Route._new(name: name, path: matcher, parent: this);

    if (preEnter != null) {
      route.onPreEnter.listen(preEnter);
    }
    if (enter != null) {
      route.onEnter.listen(enter);
    }
    if (leave != null) {
      route.onLeave.listen(leave);
    }

    if (mount != null) {
      if (mount is Function) {
        mount(route);
      } else if (mount is Routable) {
        mount.configureRoute(route);
      }
    }

    if (defaultRoute) {
      if (_defaultRoute != null) {
        throw new StateError('Only one default route can be added.');
      }
      _defaultRoute = route;
    }
    _routes[name] = route;
  }

  /**
   * Returns a route node at the end of the given route path. Route path
   * dot delimited string of route names.
   */
  Route getRoute(String routePath) {
    var routeName = routePath.split('.').first;
    if (!_routes.containsKey(routeName)) {
      _logger.warning('Invalid route name: $routeName $_routes');
      return null;
    }
    var routeToGo = _routes[routeName];
    var childPath = routePath.substring(routeName.length);
    if (!childPath.isEmpty) {
      return routeToGo.getRoute(childPath.substring(1));
    }
    return routeToGo;
  }

  String _getHead(String tail, Map queryParams) {
    if (parent == null) {
      return tail;
    }
    if (parent._currentRoute == null) {
      throw new StateError('Router $parent has no current router.');
    }
    _populateQueryParams(parent._currentRoute._lastEvent.parameters,
        parent._currentRoute, queryParams);
    return parent._getHead(parent._currentRoute.reverse(tail), queryParams);
  }

  String _getTailUrl(String routePath, Map parameters, Map queryParams) {
    var routeName = routePath.split('.').first;
    if (!_routes.containsKey(routeName)) {
      throw new StateError('Invalid route name: $routeName');
    }
    var routeToGo = _routes[routeName];
    var tail = '';
    var childPath = routePath.substring(routeName.length);
    if (childPath.length > 0) {
      tail = routeToGo._getTailUrl(
          childPath.substring(1), parameters, queryParams);
    }
    _populateQueryParams(parameters, routeToGo, queryParams);
    return routeToGo.path.reverse(
        parameters: _joinParams(parameters, routeToGo._lastEvent), tail: tail);
  }

  void _populateQueryParams(Map parameters, Route route, Map queryParams) {
    parameters.keys.forEach((String prefixedKey) {
      if (prefixedKey.startsWith('${route.name}.')) {
        var key = prefixedKey.substring('${route.name}.'.length);
        if (!route.path.urlParameterNames().contains(key)) {
          queryParams[prefixedKey] = parameters[prefixedKey];
        }
      }
    });
  }

  Map _joinParams(Map parameters, RouteEvent lastEvent) {
    if (lastEvent == null) {
      return parameters;
    }
    var joined = new Map.from(lastEvent.parameters);
    parameters.forEach((k, v) { joined[k] = v; });
    return joined;
  }

  String toString() {
    return '[Route: $name]';
  }

  /**
   * Returns a URL for this route. The tail (url generated by the child path)
   * will be passes to the UrlMatcher to be properly appended in the
   * right place.
   */
  String reverse(String tail) {
    return path.reverse(parameters: _lastEvent.parameters, tail: tail);
  }

  /**
   * Create an return a new [RouteHandle] for this route.
   */
  RouteHandle newHandle() {
    _logger.finest('newHandle for $this');
    return new RouteHandle._new(this);
  }

  /**
   * Indicates whether this route is currently active. Root route is always
   * active.
   */
  bool get isActive =>
      parent == null ? true : identical(parent._currentRoute, this);

  /**
   * Returns parameters for the currently active route. If the route is not
   * active the getter returns null.
   */
  Map get parameters {
    if (isActive) {
      if (_lastEvent == null) return {};
      return new Map.from(_lastEvent.parameters);
    }
    return null;
  }
}

/**
 * Route enter or leave event.
 */
abstract class RouteEvent {
  final String path;
  final Map parameters;
  final Route route;

  RouteEvent(this.path, this.parameters, this.route);
}

class RoutePreEnterEvent extends RouteEvent {

  var _allowEnterFutures = <Future<bool>>[];

  RoutePreEnterEvent(path, parameters, route)  : super(path, parameters, route);

  /**
   * Can be called on enter with the future which will complete with a boolean
   * value allowing (true) or disallowing (false) the current navigation.
   */
  void allowEnter(Future<bool> allow) {
    _allowEnterFutures.add(allow);
  }
}

class RouteEnterEvent extends RouteEvent {

  RouteEnterEvent(path, parameters, route)  : super(path, parameters, route);
}

class RouteLeaveEvent extends RouteEvent {

  var _allowLeaveFutures = <Future<bool>>[];

  RouteLeaveEvent(path, parameters, route)  : super(path, parameters, route);

  /**
   * Can be called on enter with the future which will complete with a boolean
   * value allowing (true) or disallowing (false) the current navigation.
   */
  void allowLeave(Future<bool> allow) {
    _allowLeaveFutures.add(allow);
  }

  RouteLeaveEvent _clone() => new RouteLeaveEvent(path, parameters, route);
}

/**
 * Event emitted when routing starts.
 */
class RouteStartEvent {

  /**
   * URI that was passed to [Router.route].
   */
  final String uri;

  /**
   * Future that completes to a boolean value of whether the routing was
   * successful.
   */
  final Future<bool> completed;

  RouteStartEvent._new(this.uri, this.completed);
}

abstract class Routable {
  void configureRoute(Route router);
}

/**
 * Stores a set of [UrlPattern] to [Handler] associations and provides methods
 * for calling a handler for a URL path, listening to [Window] history events,
 * and creating HTML event handlers that navigate to a URL.
 */
class Router {
  final bool _useFragment;
  final Window _window;
  final Route root;
  final StreamController<RouteStartEvent> _onRouteStart =
      new StreamController<RouteStartEvent>.broadcast(sync: true);
  bool _listen = false;

  /**
   * [useFragment] determines whether this Router uses pure paths with
   * [History.pushState] or paths + fragments and [Location.assign]. The default
   * value is null which then determines the behavior based on
   * [History.supportsState].
   */
  Router({bool useFragment, Window windowImpl})
      : this._init(null, useFragment: useFragment, windowImpl: windowImpl);


  Router._init(Router parent, {bool useFragment, Window windowImpl})
      : _useFragment = (useFragment == null)
            ? !History.supportsState
            : useFragment,
        _window = (windowImpl == null) ? window : windowImpl,
        root = new Route._new();

  /**
   * A stream of route calls.
   */
  Stream<RouteStartEvent> get onRouteStart => _onRouteStart.stream;

  /**
   * Finds a matching [Route] added with [addRoute], parses the path
   * and invokes the associated callback.
   *
   * This method does not perform any navigation, [go] should be used for that.
   * This method is used to invoke a handler after some other code navigates the
   * window, such as [listen].
   */
  Future<bool> route(String path, {Route startingFrom}) {
    var future = _route(path, startingFrom);
    _onRouteStart.add(new RouteStartEvent._new(path, future));
    return future;
  }

  Future<bool> _route(String path, Route startingFrom) {
    var baseRoute = startingFrom == null ? root : _dehandle(startingFrom);
    _logger.finest('route $path $baseRoute');
    var treePath = _matchingTreePath(path, baseRoute);
    Route cmpBase = baseRoute;
    var tail = path;
    // Skip all routes that are unaffected by this path.
    treePath = treePath.skipWhile((_Match matchedRoute) {
      var skip = cmpBase._currentRoute == matchedRoute.route &&
          !_paramsChanged(cmpBase, matchedRoute.urlMatch);
      if (skip) {
        cmpBase = matchedRoute.route;
        tail = matchedRoute.urlMatch.tail;
      }
      return skip;
    });
    // TODO(pavelgj): weird things happen without this line...
    treePath = treePath.toList();
    if (treePath.isEmpty) {
      return new Future.value(true);
    }
    var preEnterFutures = _preEnter(tail, treePath);
    return Future.wait(preEnterFutures).then((List<bool> results) {
      if (results.fold(true, (a, b) => a && b)) {
        return _processNewRoute(cmpBase, treePath, tail);
      }
      return false;
    });
  }

  List<Future<bool>> _preEnter(String tail, Iterable<_Match> treePath) {
    List<Future<bool>> preEnterFutures = <Future<bool>>[];
    treePath.forEach((_Match matchedRoute) {
      tail = matchedRoute.urlMatch.tail;
      var preEnterEvent = new RoutePreEnterEvent(tail, matchedRoute.urlMatch.parameters, matchedRoute.route);
      matchedRoute.route._onPreEnterController.add(preEnterEvent);
      preEnterFutures.addAll(preEnterEvent._allowEnterFutures);
    });
    return preEnterFutures;
  }

  Future<bool> _processNewRoute(Route startingFrom, Iterable<_Match> treePath, String path) {
    return _leaveOldRoutes(startingFrom, treePath).then((bool allowed) {
      if (allowed) {
        var base = startingFrom;
        var tail = path;
        treePath.forEach((_Match matchedRoute) {
          tail = matchedRoute.urlMatch.tail;
          var event = new RouteEnterEvent(matchedRoute.urlMatch.match,
              matchedRoute.urlMatch.parameters, matchedRoute.route);
          _unsetAllCurrentRoutes(base);
          base._currentRoute = matchedRoute.route;
          base._currentRoute._lastEvent = event;
          matchedRoute.route._onEnterController.add(event);
          base = matchedRoute.route;
        });
        return true;
      }
      return false;
    });
  }

  Future<bool> _leaveOldRoutes(Route startingFrom, Iterable<_Match> treePath) {
    if (treePath.isEmpty) {
      return new Future.value(true);
    }
    var event = new RouteLeaveEvent('', {}, startingFrom);
    return _leaveCurrentRoute(startingFrom, event);
  }

  Iterable<_Match> _matchingTreePath(String path, Route baseRoute) {
    List<_Match> treePath = <_Match>[];
    Route matchedRoute;
    do {
      matchedRoute = null;
      List matchingRoutes = baseRoute._routes.values.where(
          (r) => r.path.match(path) != null).toList();
      if (!matchingRoutes.isEmpty) {
        if (matchingRoutes.length > 1) {
          _logger.warning("More than one route matches $path $matchingRoutes");
        }
        matchedRoute = matchingRoutes.first;
      } else {
        if (baseRoute._defaultRoute != null) {
          matchedRoute = baseRoute._defaultRoute;
        }
      }
      if (matchedRoute != null) {
        var match = _getMatch(matchedRoute, path);
        treePath.add(new _Match(matchedRoute, match));
        baseRoute = matchedRoute;
        path = match.tail;
      }
    } while (matchedRoute != null);
    return treePath;
  }

  bool _paramsChanged(Route baseRoute, UrlMatch match) {
    return baseRoute._currentRoute._lastEvent.path != match.match ||
        !_mapsEqual(baseRoute._currentRoute._lastEvent.parameters,
            match.parameters);
  }

  bool _mapsEqual(Map a, Map b) {
    if (a.keys.length != b.keys.length) {
      return false;
    }
    for (var keyInA in a.keys) {
      if (!b.containsKey(keyInA) || a[keyInA] != b[keyInA]) {
        return false;
      }
    }
    return true;
  }

  /// Navigates to a given relative route path, and parameters.
  Future go(String routePath, Map parameters,
            {Route startingFrom, bool replace: false}) {
    Map queryParams = {};
    var baseRoute = startingFrom == null ? this.root : _dehandle(startingFrom);
    var newTail = baseRoute._getTailUrl(routePath, parameters, queryParams) +
        _buildQuery(queryParams);
    String newUrl = baseRoute._getHead(newTail, queryParams);
    _logger.finest('go $newUrl');
    return route(newTail, startingFrom: baseRoute).then((success) {
      if (success) {
        _go(newUrl, null, replace);
      }
      return success;
    });
  }

  /// Returns an absolute URL for a given relative route path and parameters.
  String url(String routePath, {Route startingFrom, Map parameters}) {
    var baseRoute = startingFrom == null ? this.root : _dehandle(startingFrom);
    parameters = parameters == null ? {} : parameters;
    Map queryParams = {};
    var tail = baseRoute._getTailUrl(routePath, parameters, queryParams);
    return (_useFragment ? '#' : '') + baseRoute._getHead(tail, queryParams) +
        _buildQuery(queryParams);
  }

  String _buildQuery(Map queryParams) {
    var query = queryParams.keys.map((key) =>
        '$key=${Uri.encodeComponent(queryParams[key])}').join('&');
    if (query.isEmpty) {
      return '';
    }
    return '?$query';
  }

  Route _dehandle(Route r) => r is RouteHandle ? r._getHost(r): r;

  UrlMatch _getMatch(Route route, String path) {
    var match = route.path.match(path);
    if (match == null) { // default route
      return new UrlMatch('', '', {});
    }
    _parseQuery(route, path).forEach((k, v) { match.parameters[k] = v; });
    return match;
  }

  Map _parseQuery(Route route, String path) {
    var params = {};
    if (path.indexOf('?') == -1) {
      return params;
    }
    String queryStr = path.substring(path.indexOf('?') + 1);
    queryStr.split('&').forEach((String keyValPair) {
      List<String> keyVal = _parseKeyVal(keyValPair);
      if (keyVal[0].startsWith('${route.name}.')) {
        var key = keyVal[0].substring('${route.name}.'.length);
        if (!key.isEmpty) {
          params[key] = Uri.decodeComponent(keyVal[1]);
        }
      }
    });
    return params;
  }

  List<String> _parseKeyVal(keyValPair) {
    if (keyValPair.isEmpty) {
      return ['', ''];
    }
    var splitPoint = keyValPair.indexOf('=') == -1 ?
        keyValPair.length : keyValPair.indexOf('=') + 1;
    var key = keyValPair.substring(0, splitPoint +
        (keyValPair.indexOf('=') == -1 ? 0 : -1));
    var value = keyValPair.substring(splitPoint);
    return [key, value];
  }

  void _unsetAllCurrentRoutes(Route r) {
    if (r._currentRoute != null) {
      _unsetAllCurrentRoutes(r._currentRoute);
      r._currentRoute = null;
    }
  }

  Future<bool> _leaveCurrentRoute(Route base, RouteLeaveEvent e) =>
      Future.wait(_leaveCurrentRouteHelper(base, e))
          .then((values) => values.fold(true, (c, v) => c && v));

  List<Future<bool>> _leaveCurrentRouteHelper(Route base, RouteLeaveEvent e) {
    var futures = [];
    if (base._currentRoute != null) {
      List<Future<bool>> pendingResponses = <Future<bool>>[];
      // We create a copy of the route event
      var event = e._clone();
      base._currentRoute._onLeaveController.add(event);
      futures.addAll(event._allowLeaveFutures);
      futures.addAll(_leaveCurrentRouteHelper(base._currentRoute, event));
    }
    return futures;
  }

  /**
   * Listens for window history events and invokes the router. On older
   * browsers the hashChange event is used instead.
   */
  void listen({bool ignoreClick: false, Element appRoot}) {
    _logger.finest('listen ignoreClick=$ignoreClick');
    if (_listen) {
      throw new StateError('listen can only be called once');
    }
    _listen = true;
    if (_useFragment) {
      _window.onHashChange.listen((_) {
        route(_normalizeHash(_window.location.hash)).then((allowed) {
          // if not allowed, we need to restore the browser location
          if (!allowed) {
            _window.history.back();
          }
        });
      });
      route(_normalizeHash(_window.location.hash));
    } else {
      _window.onPopState.listen((_) {
        var path = '${_window.location.pathname}${_window.location.hash}';
        route(path).then((allowed) {
          // if not allowed, we need to restore the browser location
          if (!allowed) {
            _window.history.back();
          }
        });
      });
    }
    if (!ignoreClick) {
      if (appRoot == null) {
        appRoot = _window.document.documentElement;
      }
      _logger.finest('listen on win');
      appRoot.onClick.listen((MouseEvent e) {
        if (!e.ctrlKey && !e.metaKey && !e.shiftKey && e.target is AnchorElement) {
          AnchorElement anchor = e.target;
          if (anchor.host == _window.location.host) {
            _logger.finest('clicked ${anchor.pathname}${anchor.hash}');
            e.preventDefault();
            var path;
            if (_useFragment) {
              path = _normalizeHash(anchor.hash);
            } else {
              path = '${anchor.pathname}';
            }
            route(path).then((allowed) {
              if (allowed) {
                _go(path, null, false);
              }
            });
          }
        }
      });
    }
  }

  String _normalizeHash(String hash) {
    if (hash.isEmpty) {
      return '';
    }
    return hash.substring(1);
  }

  /**
   * Navigates the browser to the path produced by [url] with [args] by calling
   * [History.pushState], then invokes the handler associated with [url].
   *
   * On older browsers [Location.assign] is used instead with the fragment
   * version of the UrlPattern.
   */
  Future<bool> gotoUrl(String url) {
    return route(url).then((success) {
      if (success) {
        _go(url, null, false);
      }
    });
  }

  void _go(String path, String title, bool replace) {
    title = (title == null) ? '' : title;
    if (_useFragment) {
      if (replace) {
        _window.location.replace('#$path');
      } else {
        _window.location.assign('#$path');
      }
      (_window.document as HtmlDocument).title = title;
    } else {
      if (replace) {
        _window.history.replaceState(null, title, path);
      } else {
        _window.history.pushState(null, title, path);
      }
    }
  }

  /**
   * Returns the current active route path in the route tree.
   * Excludes the root path.
   */
  List<Route> get activePath {
    var res = <Route>[];
    var current = root;
    while (current._currentRoute != null) {
      current = current._currentRoute;
      res.add(current);
    }
    return res;
  }
}

class _Match {
  final Route route;
  final UrlMatch urlMatch;

  _Match(this.route, this.urlMatch);
}
