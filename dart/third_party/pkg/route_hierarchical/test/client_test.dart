import 'dart:async';
import 'dart:html';

import 'package:unittest/unittest.dart';
import 'package:unittest/mock.dart';
import 'package:unittest/html_enhanced_config.dart';
import 'package:route_hierarchical/client.dart';
import 'package:route_hierarchical/url_pattern.dart';

import 'mocks.dart';

main() {
  useHtmlEnhancedConfiguration();

  test('paths are routed to routes added with addRoute', () {
    Router router = new Router();
    router.root.addRoute(
        name: 'foo',
        path: '/foo',
        enter: expectAsync1((RouteEvent e) {
          expect(e.path, '/foo');
        }));
    router.route('/foo');
  });

  group('hierarchical routing', () {

    _testParentChild(
        Pattern parentPath,
        Pattern childPath,
        String expectedParentPath,
        String expectedChildPath,
        String testPath) {
      Router router = new Router();
      router.root.addRoute(
          name: 'parent',
          path: parentPath,
          enter: expectAsync1((RouteEvent e) {
            expect(e.path, expectedParentPath);
          }),
          mount: (Route child) {
            child.addRoute(
                name: 'child',
                path: childPath,
                enter: expectAsync1((RouteEvent e) {
                  expect(e.path, expectedChildPath);
                }));
          });
      router.route(testPath);
    }

    test('child router with UrlPattern', () {
      _testParentChild(
          new UrlPattern(r'/foo/(\w+)'),
          new UrlPattern(r'/bar'),
          '/foo/abc',
          '/bar',
          '/foo/abc/bar');
    });

    test('child router with Strings', () {
      _testParentChild(
          '/foo',
          '/bar',
          '/foo',
          '/bar',
          '/foo/bar');
    });

  });

  group('leave', () {

    test('should leave previous route and enter new', () {
      Map<String, int> counters = <String, int>{
        'fooEnter': 0,
        'fooLeave': 0,
        'barEnter': 0,
        'barLeave': 0,
        'bazEnter': 0,
        'bazLeave': 0
      };
      Router router = new Router();
      router.root
        ..addRoute(path: '/foo',
            name: 'foo',
            enter: (RouteEvent e) => counters['fooEnter']++,
            leave: (RouteEvent e) => counters['fooLeave']++,
            mount: (Route route) => route
              ..addRoute(path: '/bar',
                  name: 'bar',
                  enter: (RouteEvent e) => counters['barEnter']++,
                  leave: (RouteEvent e) => counters['barLeave']++)
              ..addRoute(path: '/baz',
                  name: 'baz',
                  enter: (RouteEvent e) => counters['bazEnter']++,
                  leave: (RouteEvent e) => counters['bazLeave']++));

      expect(counters, {
        'fooEnter': 0,
        'fooLeave': 0,
        'barEnter': 0,
        'barLeave': 0,
        'bazEnter': 0,
        'bazLeave': 0
      });

      router.route('/foo/bar').then(expectAsync1((_) {
        expect(counters, {
          'fooEnter': 1,
          'fooLeave': 0,
          'barEnter': 1,
          'barLeave': 0,
          'bazEnter': 0,
          'bazLeave': 0
        });

        router.route('/foo/baz').then(expectAsync1((_) {
          expect(counters, {
            'fooEnter': 1,
            'fooLeave': 0,
            'barEnter': 1,
            'barLeave': 1,
            'bazEnter': 1,
            'bazLeave': 0
          });
        }));
      }));
    });

    _testAllowLeave(bool allowLeave) {
      Completer<bool> completer = new Completer<bool>();
      bool barEntered = false;
      bool bazEntered = false;

      Router router = new Router();
      router.root
        ..addRoute(name: 'foo', path: '/foo',
            mount: (Route child) => child
              ..addRoute(name: 'bar', path: '/bar',
                  enter: (RouteEvent e) => barEntered = true,
                  leave: (RouteEvent e) => e.allowLeave(completer.future))
              ..addRoute(name: 'baz', path: '/baz',
                  enter: (RouteEvent e) => bazEntered = true));

      router.route('/foo/bar').then(expectAsync1((_) {
        expect(barEntered, true);
        expect(bazEntered, false);
        router.route('/foo/baz').then(expectAsync1((_) {
          expect(bazEntered, allowLeave);
        }));
        completer.complete(allowLeave);
      }));
    }

    test('should allow navigation', () {
      _testAllowLeave(true);
    });

    test('should veto navigation', () {
      _testAllowLeave(false);
    });
  });

  group('Default route', () {

    _testHeadTail(String path, String expectFoo, String expectBar) {
      Router router = new Router();
      router.root
        ..addRoute(
            name: 'foo',
            path: '/foo',
            defaultRoute: true,
            enter: expectAsync1((RouteEvent e) {
              expect(e.path, expectFoo);
            }),
            mount: (child) => child
              ..addRoute(
                  name: 'bar',
                  path: '/bar',
                  defaultRoute: true,
                  enter: expectAsync1((RouteEvent e) =>
                      expect(e.path, expectBar))));

      router.route(path);
    }

    test('should calculate head/tail of empty route', () {
      _testHeadTail('', '', '');
    });

    test('should calculate head/tail of partial route', () {
      _testHeadTail('/foo', '/foo', '');
    });

    test('should calculate head/tail of a route', () {
      _testHeadTail('/foo/bar', '/foo', '/bar');
    });

    test('should calculate head/tail of an invalid parent route', () {
      _testHeadTail('/garbage/bar', '', '');
    });

    test('should calculate head/tail of an invalid child route', () {
      _testHeadTail('/foo/garbage', '/foo', '');
    });

    test('should follow default routes', () {
      Map<String, int> counters = <String, int>{
        'list_entered': 0,
        'article_123_entered': 0,
        'article_123_view_entered': 0,
        'article_123_edit_entered': 0
      };

      Router router = new Router();
      router.root
        ..addRoute(
            name: 'articles',
            path: '/articles',
            defaultRoute: true,
            enter: (_) => counters['list_entered']++)
        ..addRoute(
            name: 'article',
            path: '/article/123',
            enter: (_) => counters['article_123_entered']++,
            mount: (Route child) => child
              ..addRoute(
                  name: 'viewArticles',
                  path: '/view',
                  defaultRoute: true,
                  enter: (_) => counters['article_123_view_entered']++)
              ..addRoute(
                  name: 'editArticles',
                  path: '/edit',
                  enter: (_) => counters['article_123_edit_entered']++));

      router.route('').then((_) {
        expect(counters, {
          'list_entered': 1, // default to list
          'article_123_entered': 0,
          'article_123_view_entered': 0,
          'article_123_edit_entered': 0
        });
        router.route('/articles').then((_) {
          expect(counters, {
            'list_entered': 2,
            'article_123_entered': 0,
            'article_123_view_entered': 0,
            'article_123_edit_entered': 0
          });
          router.route('/article/123').then((_) {
            expect(counters, {
              'list_entered': 2,
              'article_123_entered': 1,
              'article_123_view_entered': 1, // default to view
              'article_123_edit_entered': 0
            });
            router.route('/article/123/view').then((_) {
              expect(counters, {
                'list_entered': 2,
                'article_123_entered': 1,
                'article_123_view_entered': 2,
                'article_123_edit_entered': 0
              });
              router.route('/article/123/edit').then((_) {
                expect(counters, {
                  'list_entered': 2,
                  'article_123_entered': 1,
                  'article_123_view_entered': 2,
                  'article_123_edit_entered': 1
                });
              });
            });
          });
        });
      });
    });

  });

  group('go', () {

    test('shoud location.assign/replace when useFragment=true', () {
      MockWindow mockWindow = new MockWindow();
      Router router = new Router(useFragment: true, windowImpl: mockWindow);
      router.root
        ..addRoute(
            name: 'articles',
            path: '/articles');

      router.go('articles', {}).then(expectAsync1((_) {
        var mockLocation = mockWindow.location;

        mockLocation.getLogs(callsTo('assign', anything))
            .verify(happenedExactly(1));
        expect(mockLocation.getLogs(callsTo('assign', anything)).last.args,
            ['#/articles']);
        mockLocation.getLogs(callsTo('replace', anything))
            .verify(happenedExactly(0));

        router.go('articles', {}, replace: true).then(expectAsync1((_) {
          mockLocation.getLogs(callsTo('replace', anything))
              .verify(happenedExactly(1));
          expect(mockLocation.getLogs(callsTo('replace', anything)).last.args,
              ['#/articles']);
          mockLocation.getLogs(callsTo('assign', anything))
              .verify(happenedExactly(1));
        }));
      }));
    });

    test('shoud history.push/replaceState when useFragment=false', () {
      MockWindow mockWindow = new MockWindow();
      Router router = new Router(useFragment: false, windowImpl: mockWindow);
      router.root
        ..addRoute(
            name: 'articles',
            path: '/articles');

      router.go('articles', {}).then(expectAsync1((_) {
        var mockHistory = mockWindow.history;

        mockHistory.getLogs(callsTo('pushState', anything))
            .verify(happenedExactly(1));
        expect(mockHistory.getLogs(callsTo('pushState', anything)).last.args,
            [null, '', '/articles']);
        mockHistory.getLogs(callsTo('replaceState', anything))
            .verify(happenedExactly(0));

        router.go('articles', {}, replace: true).then(expectAsync1((_) {
          mockHistory.getLogs(callsTo('replaceState', anything))
              .verify(happenedExactly(1));
          expect(mockHistory.getLogs(callsTo('replaceState', anything)).last.args,
              [null, '', '/articles']);
          mockHistory.getLogs(callsTo('pushState', anything))
              .verify(happenedExactly(1));
        }));
      }));
    });

    test('should work with hierarchical go', () {
      MockWindow mockWindow = new MockWindow();
      Router router = new Router(windowImpl: mockWindow);
      router.root
        ..addRoute(
            name: 'a',
            path: '/:foo',
            mount: (child) => child
              ..addRoute(
                  name: 'b',
                  path: '/:bar'));

      var routeA = router.root.getRoute('a');

      router.go('a.b', {}).then(expectAsync1((_) {
        var mockHistory = mockWindow.history;

        mockHistory.getLogs(callsTo('pushState', anything))
            .verify(happenedExactly(1));
        expect(mockHistory.getLogs(callsTo('pushState', anything)).last.args,
            [null, '', '/null/null']);

        router.go('a.b', {'foo': 'aaaa', 'bar': 'bbbb'}).then(expectAsync1((_) {
          mockHistory.getLogs(callsTo('pushState', anything))
              .verify(happenedExactly(2));
          expect(mockHistory.getLogs(callsTo('pushState', anything)).last.args,
              [null, '', '/aaaa/bbbb']);

          router.go('b', {'bar': 'bbbb'}, startingFrom: routeA)
              .then(expectAsync1((_) {
                mockHistory.getLogs(callsTo('pushState', anything))
                   .verify(happenedExactly(3));
                expect(
                    mockHistory.getLogs(callsTo('pushState')).last.args,
                    [null, '', '/aaaa/bbbb']);
              }));

        }));
      }));

    });

    test('should attempt to reverse default routes', () {
      Map<String, int> counters = <String, int>{
        'aEnter': 0,
        'bEnter': 0
      };

      MockWindow mockWindow = new MockWindow();
      Router router = new Router(windowImpl: mockWindow);
      router.root
        ..addRoute(
            name: 'a',
            defaultRoute: true,
            path: '/:foo',
            enter: (_) => counters['aEnter']++,
            mount: (child) => child
              ..addRoute(
                  name: 'b',
                  defaultRoute: true,
                  path: '/:bar',
                  enter: (_) => counters['bEnter']++));

      expect(counters, {
        'aEnter': 0,
        'bEnter': 0
      });

      router.route('').then((_) {
        expect(counters, {
          'aEnter': 1,
          'bEnter': 1
        });

        var routeA = router.root.getRoute('a');
        router.go('b', {'bar': 'bbb'}, startingFrom: routeA).then((_) {
          var mockHistory = mockWindow.history;

          mockHistory.getLogs(callsTo('pushState', anything))
             .verify(happenedExactly(1));
          expect(mockHistory.getLogs(callsTo('pushState', anything)).last.args,
              [null, '', '/null/bbb']);
        });
      });
    });

  });

  group('url', () {

    test('should reconstruct url', () {
      MockWindow mockWindow = new MockWindow();
      Router router = new Router(windowImpl: mockWindow);
      router.root
        ..addRoute(
            name: 'a',
            defaultRoute: true,
            path: '/:foo',
            mount: (child) => child
              ..addRoute(
                  name: 'b',
                  defaultRoute: true,
                  path: '/:bar'));

      var routeA = router.root.getRoute('a');

      router.route('').then((_) {
        expect(router.url('a.b'), '/null/null');
        expect(router.url('a.b', parameters: {'foo': 'aaa'}), '/aaa/null');
        expect(router.url('b', parameters: {'bar': 'bbb'},
            startingFrom: routeA), '/null/bbb');

        router.route('/foo/bar').then((_) {
          expect(router.url('a.b'), '/foo/bar');
          expect(router.url('a.b', parameters: {'foo': 'aaa'}), '/aaa/bar');
          expect(router.url('b', parameters: {'bar': 'bbb'},
              startingFrom: routeA), '/foo/bbb');
          expect(router.url('b', parameters: {'foo': 'aaa', 'bar': 'bbb'},
              startingFrom: routeA), '/foo/bbb');

          expect(router.url('b', parameters: {'bar': 'bbb', 'b.param1': 'val1'},
              startingFrom: routeA), '/foo/bbb?b.param1=val1');

        });
      });
    });

  });

  group('getRoute', () {

    test('should return correct routes', () {
      Route routeFoo, routeBar, routeBaz, routeQux, routeAux;

      Router router = new Router();
      router.root
        ..addRoute(
            name: 'foo',
            path: '/:foo',
            mount: (child) => routeFoo = child
              ..addRoute(
                  name: 'bar',
                  path: '/:bar',
                  mount: (child) => routeBar = child
                    ..addRoute(
                        name: 'baz',
                        path: '/:baz',
                        mount: (child) => routeBaz = child))
              ..addRoute(
                  name: 'qux',
                  path: '/:qux',
                  mount: (child) => routeQux = child
                    ..addRoute(
                        name: 'aux',
                        path: '/:aux',
                        mount: (child) => routeAux = child)));

      expect(router.root.getRoute('foo'), same(routeFoo));
      expect(router.root.getRoute('foo.bar'), same(routeBar));
      expect(routeFoo.getRoute('bar'), same(routeBar));
      expect(router.root.getRoute('foo.bar.baz'), same(routeBaz));
      expect(router.root.getRoute('foo.qux'), same(routeQux));
      expect(router.root.getRoute('foo.qux.aux'), same(routeAux));
      expect(routeQux.getRoute('aux'), same(routeAux));
      expect(routeFoo.getRoute('qux.aux'), same(routeAux));

      expect(router.root.getRoute('baz'), isNull);
      expect(router.root.getRoute('foo.baz'), isNull);
    });

  });

  group('route', () {

    test('should parse query', () {
      Router router = new Router();
      router.root
        ..addRoute(
            name: 'foo',
            path: '/:foo',
            enter: expectAsync1((RouteEvent e) {
              expect(e.parameters, {
                'foo': '123',
                'a': 'b',
                'b': '',
                'c': 'foo bar'
              });
            }));

      router.route('/123?foo.a=b&foo.b=&foo.c=foo%20bar&foo.=ignore');
    });

    group('isActive', () {

      test('should currectly identify active/inactive routes', () {
        Router router = new Router();
        router.root
          ..addRoute(
              name: 'foo',
              path: '/foo',
              mount: (child) => child
                ..addRoute(
                    name: 'bar',
                    path: '/bar',
                    mount: (child) => child
                      ..addRoute(
                          name: 'baz',
                          path: '/baz',
                          mount: (child) => child))
                ..addRoute(
                    name: 'qux',
                    path: '/qux',
                    mount: (child) => child
                      ..addRoute(
                          name: 'aux',
                          path: '/aux',
                          mount: (child) => child)));

        expect(r(router, 'foo').isActive, false);
        expect(r(router, 'foo.bar').isActive, false);
        expect(r(router, 'foo.bar.baz').isActive, false);
        expect(r(router, 'foo.qux').isActive, false);

        return router.route('/foo').then((_) {
          expect(r(router, 'foo').isActive, true);
          expect(r(router, 'foo.bar').isActive, false);
          expect(r(router, 'foo.bar.baz').isActive, false);
          expect(r(router, 'foo.qux').isActive, false);

          return router.route('/foo/qux').then((_) {
            expect(r(router, 'foo').isActive, true);
            expect(r(router, 'foo.bar').isActive, false);
            expect(r(router, 'foo.bar.baz').isActive, false);
            expect(r(router, 'foo.qux').isActive, true);

            return router.route('/foo/bar/baz').then((_) {
              expect(r(router, 'foo').isActive, true);
              expect(r(router, 'foo.bar').isActive, true);
              expect(r(router, 'foo.bar.baz').isActive, true);
              expect(r(router, 'foo.qux').isActive, false);
            });
          });
        });
      });

    });

    group('parameters', () {

      test('should return path parameters for routes', () {
        Router router = new Router();
        router.root
          ..addRoute(
              name: 'foo',
              path: '/:foo',
              mount: (child) => child
                ..addRoute(
                    name: 'bar',
                    path: '/:bar',
                    mount: (child) => child
                      ..addRoute(
                          name: 'baz',
                          path: '/:baz',
                          mount: (child) => child)));

        expect(r(router, 'foo').parameters, isNull);
        expect(r(router, 'foo.bar').parameters, isNull);
        expect(r(router, 'foo.bar.baz').parameters, isNull);

        return router.route('/aaa').then((_) {
          expect(r(router, 'foo').parameters, {'foo': 'aaa'});
          expect(r(router, 'foo.bar').parameters, isNull);
          expect(r(router, 'foo.bar.baz').parameters, isNull);

          return router.route('/aaa/bbb').then((_) {
            expect(r(router, 'foo').parameters, {'foo': 'aaa'});
            expect(r(router, 'foo.bar').parameters, {'bar': 'bbb'});
            expect(r(router, 'foo.bar.baz').parameters, isNull);

            return router.route('/aaa/bbb/ccc').then((_) {
              expect(r(router, 'foo').parameters, {'foo': 'aaa'});
              expect(r(router, 'foo.bar').parameters, {'bar': 'bbb'});
              expect(r(router, 'foo.bar.baz').parameters, {'baz': 'ccc'});
            });
          });
        });
      });

    });

  });

  group('activePath', () {

    test('should currectly identify active path', () {
      Router router = new Router();
      router.root
        ..addRoute(
            name: 'foo',
            path: '/foo',
            mount: (child) => child
            ..addRoute(
                name: 'bar',
                path: '/bar',
                mount: (child) => child
                ..addRoute(
                    name: 'baz',
                    path: '/baz',
                    mount: (child) => child))
            ..addRoute(
                name: 'qux',
                path: '/qux',
                mount: (child) => child
                ..addRoute(
                    name: 'aux',
                    path: '/aux',
                    mount: (child) => child)));

      var strPath = (List<Route> path) =>
          path.map((Route r) => r.name).join('.');

      expect(strPath(router.activePath), '');

      return router.route('/foo').then((_) {
        expect(strPath(router.activePath), 'foo');

        return router.route('/foo/qux').then((_) {
          expect(strPath(router.activePath), 'foo.qux');

          return router.route('/foo/bar/baz').then((_) {
            expect(strPath(router.activePath), 'foo.bar.baz');
          });
        });
      });
    });

  });

}

/// An alias for Router.root.getRoute(path)
r(Router router, String path) => router.root.getRoute(path);