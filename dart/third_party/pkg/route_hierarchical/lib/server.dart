// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * This library provides a simple API for routing HttpRequests based on thier
 * URL.
 */
library route.server;

import 'dart:async';
import 'dart:collection';
import 'dart:io';
import 'package:route_hierarchical/src/async_utils.dart';
export 'url_pattern.dart';
import 'pattern.dart';

typedef Future<bool> Filter(HttpRequest request);

/**
 * A request router that makes it easier to handle [HttpRequest]s from an
 * [HttpServer].
 *
 * [serve] creates a new [Stream] of requests whose paths match against the
 * given pattern. Matching requests are not sent to any other streams created by
 * a server() call.
 *
 * [filter] registers a [Filter] function to run against matching requests. On
 * each request the filters that match are run in order, waiting for each to
 * complete since filters return a Future. If any filter completes false, the
 * subsequent filters and request handlers are not run. This way a filter can
 * prevent further processing, like needed for authentication.
 *
 * Requests not matched by a call to [serve] are sent to the [defaultStream].
 * If there's no subscriber to the defaultStream then a 404 is sent to the
 * response.
 *
 * Example:
 *     import 'package:route/server.dart';
 *     import 'package:route/pattern.dart';
 *
 *     HttpServer.bind().then((server) {
 *       var router = new Router(server);
 *       router.filter(matchesAny(['/foo', '/bar']), authFilter);
 *       router.serve('/foo').listen(fooHandler);
 *       router.serve('/bar').listen(barHandler);
 *       router.defaultStream.listen(send404);
 *     });
 */
class Router {
  final Stream<HttpRequest> _incoming;

  final List<_Route> _routes = <_Route>[];

  final Map<Pattern, Filter> _filters = new LinkedHashMap<Pattern, Filter>();

  final StreamController<HttpRequest> _defaultController =
      new StreamController<HttpRequest>();

  /**
   * Create a new Router that listens to the [incoming] stream, usually an
   * instance of [HttpServer].
   */
  Router(Stream<HttpRequest> incoming) : _incoming = incoming {
    _incoming.listen(_handleRequest);
  }

  /**
   * Request whose URI matches [url] and [method] (if provided) are sent to the
   * stream created by this method, and not sent to any other router streams.
   */
  Stream<HttpRequest> serve(Pattern url, {String method}) {
    var controller = new StreamController<HttpRequest>();
    _routes.add(new _Route(controller, url, method:method));
    return controller.stream;
  }

  /**
   * A [Filter] returns a [Future<bool>] that tells the router whether to apply
   * the remaining filters and send requests to the streams created by [serve].
   *
   * If the filter returns true, the request is passed to the next filter, and
   * then to the first matching server stream. If the filter returns false, it's
   * assumed that the filter is handling the request and it's not forwarded.
   */
  void filter(Pattern url, Filter filter) {
    _filters[url] = filter;
  }

  Stream<HttpRequest> get defaultStream => _defaultController.stream;

  void _handleRequest(HttpRequest req) {
    bool cont = true;
    doWhile(_filters.keys, (Pattern pattern) {
      if (matchesFull(pattern, req.uri.path)) {
        return _filters[pattern](req).then((c) {
          cont = c;
          return c;
        });
      }
      return new Future.value(true);
    }).then((_) {
      if (cont) {
        bool handled = false;
        var matches = _routes.where((r) => r.matches(req));
        if (!matches.isEmpty) {
          matches.first.controller.add(req);
        } else {
          if (_defaultController.hasListener) {
            _defaultController.add(req);
          } else {
            send404(req);
          }
        }
      }
    });
  }
}

void send404(HttpRequest req) {
  req.response.statusCode = HttpStatus.NOT_FOUND;
  req.response.write("Not Found");
  req.response.close();
}

class _Route {
  final Pattern url;
  final String method;
  final StreamController controller;
  _Route(this.controller, this.url, {this.method});

  bool matches(HttpRequest request) => matchesFull(url, request.uri.path) &&
      (method == null || request.method.toUpperCase() == method);
}
