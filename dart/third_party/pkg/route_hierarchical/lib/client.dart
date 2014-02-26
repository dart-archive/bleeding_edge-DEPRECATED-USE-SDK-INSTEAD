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

typedef RouteEventHandler(RouteEvent path);

/**
 * A helper Router handle that scopes all route event subsriptions to it's
 * instance and provides an convinience [discard] method.
 */
class RouteHandle implements Route {
  Route _route;
  final StreamController<RouteEvent> _onRouteController;
  final StreamController<RouteEvent> _onLeaveController;
  Stream<RouteEvent> get onRoute => _onRouteController.stream;
  Stream<RouteEvent> get onLeave => _onLeaveController.stream;
  StreamSubscription _onRouteSubscription;
  StreamSubscription _onLeaveSubscription;
  List<RouteHandle> _childHandles = <RouteHandle>[];

  RouteHandle._new(Route this._route)
      : _onRouteController =
            new StreamController<RouteEvent>.broadcast(sync: true),
        _onLeaveController =
            new StreamController<RouteEvent>.broadcast(sync: true) {
    _onRouteSubscription = _route.onRoute.listen(_onRouteController.add);
    _onLeaveSubscription = _route.onLeave.listen(_onLeaveController.add);
  }

  /// discards this handle.
  void discard() {
    _logger.finest('discarding handle for $_route');
    _onRouteSubscription.cancel();
    _onLeaveSubscription.cancel();
    _onRouteController.close();
    _onLeaveController.close();
    _childHandles.forEach((RouteHandle c) => c.discard());
    _childHandles.clear();
    _route = null;
  }

  /// Not supported. Overridden to throw an error.
  void addRoute({String name, Pattern path, bool defaultRoute: false,
      RouteEventHandler enter, RouteEventHandler leave, mount}) =>
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

/**
 * Route is a node in the tree of routes. The edge leading to the route is
 * defined by path.
 */
class Route {
  final String name;
  final Map<String, Route> _routes = new LinkedHashMap<String, Route>();
  final UrlMatcher path;
  final StreamController<RouteEvent> _onRouteController;
  final StreamController<RouteEvent> _onLeaveController;
  final Route parent;
  Route _defaultRoute;
  Route _currentRoute;
  RouteEvent _lastEvent;

  Stream<RouteEvent> get onRoute => _onRouteController.stream;
  Stream<RouteEvent> get onLeave => _onLeaveController.stream;

  Route._new({this.name, this.path, this.parent})
      : _onRouteController =
            new StreamController<RouteEvent>.broadcast(sync: true),
        _onLeaveController =
            new StreamController<RouteEvent>.broadcast(sync: true);

  void addRoute({String name, Pattern path, bool defaultRoute: false,
      RouteEventHandler enter, RouteEventHandler leave, mount}) {
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

    if (enter != null) {
      route.onRoute.listen(enter);
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
class RouteEvent {
  final String path;
  final Map parameters;
  final Route route;
  var _allowLeaveFutures = <Future<bool>>[];

  RouteEvent(this.path, this.parameters, this.route);

  /**
   * Can be called on leave with the future which will complete with a boolean
   * value allowing (true) or disallowing (false) the current navigation.
   */
  void allowLeave(Future<bool> allow) {
    _allowLeaveFutures.add(allow);
  }

  RouteEvent _clone() => new RouteEvent(path, parameters, route);
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
    var future = _route(path, startingFrom: startingFrom);
    _onRouteStart.add(new RouteStartEvent._new(path, future));
    return future;
  }

  Future<bool> _route(String path, {Route startingFrom}) {
    var baseRoute = startingFrom == null ? this.root : _dehandle(startingFrom);
    _logger.finest('route $path $baseRoute');
    Route matchedRoute;
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
      if (matchedRoute != baseRoute._currentRoute ||
          _paramsChanged(baseRoute, match)) {
        return _processNewRoute(baseRoute, path, match, matchedRoute);
      } else {
        baseRoute._currentRoute._lastEvent =
            new RouteEvent(match.match, match.parameters,
                baseRoute._currentRoute);
        return _route(match.tail, startingFrom: matchedRoute);
      }
    } else if (baseRoute._currentRoute != null) {
      var event = new RouteEvent('', {}, baseRoute);
      return _leaveCurrentRoute(baseRoute, event).then((success) {
        if (success) {
          baseRoute._currentRoute = null;
        }
        return success;
      });
    }
    return new Future.value(true);
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

  Route _dehandle(Route r) {
    if (r is RouteHandle) {
      return (r as RouteHandle)._getHost(r);
    }
    return r;
  }

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

  Future<bool> _processNewRoute(Route base, String path, UrlMatch match,
      Route newRoute) {
    _logger.finest('_processNewRoute $path');
    var event = new RouteEvent(match.match, match.parameters, newRoute);
    // before we make this a new current route, leave the old
    return _leaveCurrentRoute(base, event).then((bool allowNavigation) {
      if (allowNavigation) {
        _unsetAllCurrentRoutes(base);
        base._currentRoute = newRoute;
        base._currentRoute._lastEvent = event;
        newRoute._onRouteController.add(event);
        return _route(match.tail, startingFrom: newRoute);
      }
      return false;
    });
  }

  void _unsetAllCurrentRoutes(Route r) {
    if (r._currentRoute != null) {
      _unsetAllCurrentRoutes(r._currentRoute);
      r._currentRoute = null;
    }
  }

  Future<bool> _leaveCurrentRoute(Route base, RouteEvent e) =>
      Future.wait(_leaveCurrentRouteHelper(base, e))
          .then((values) => values.fold(true, (c, v) => c && v));

  List<Future<bool>> _leaveCurrentRouteHelper(Route base, RouteEvent e) {
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
  void listen({bool ignoreClick: false}) {
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
      _logger.finest('listen on win');
      _window.onClick.listen((Event e) {
        if (e.target is AnchorElement) {
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
