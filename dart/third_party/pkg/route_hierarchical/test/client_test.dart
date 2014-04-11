// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library route.client_test;

import 'dart:async';
import 'dart:html';

import 'package:unittest/unittest.dart';
import 'package:mock/mock.dart';
import 'package:route_hierarchical/client.dart';
import 'package:route_hierarchical/url_pattern.dart';

import 'util/mocks.dart';

main() {
  unittestConfiguration.timeout = new Duration(seconds: 1);

  test('paths are routed to routes added with addRoute', () {
    var router = new Router();
    router.root.addRoute(
        name: 'foo',
        path: '/foo',
        enter: expectAsync((RouteEvent e) {
          expect(e.path, '/foo');
          expect(router.root.findRoute('foo').isActive, isTrue);
        }));
    return router.route('/foo');
  });

  group('use a longer path first', () {

    test('add a longer path first', () {
      Router router = new Router();
      router.root
        ..addRoute(
          name: 'foobar',
          path: '/foo/bar',
          enter: expectAsync((RouteEvent e) {
            expect(e.path, '/foo/bar');
            expect(router.root.findRoute('foobar').isActive, isTrue);
          }))
        ..addRoute(
          name: 'foo',
          path: '/foo',
          enter: (e) => fail('should invoke /foo/bar'));
      return router.route('/foo/bar');
    });

    test('add a longer path last', () {
      Router router = new Router();
      router.root
        ..addRoute(
          name: 'foo',
          path: '/foo',
          enter: (e) => fail('should invoke /foo/bar'))
        ..addRoute(
          name: 'foobar',
          path: '/foo/bar',
          enter: expectAsync((RouteEvent e) {
            expect(e.path, '/foo/bar');
            expect(router.root.findRoute('foobar').isActive, isTrue);
          }));
      return router.route('/foo/bar');
    });

    test('add paths with a param', () {
      Router router = new Router();
      router.root
        ..addRoute(
          name: 'foo',
          path: '/foo',
          enter: (e) => fail('should invoke /foo/bar'))
        ..addRoute(
          name: 'fooparam',
          path: '/foo/:param',
          enter: expectAsync((RouteEvent e) {
            expect(e.path, '/foo/bar');
            expect(router.root.findRoute('fooparam').isActive, isTrue);
          }));
      return router.route('/foo/bar');
    });

    test('add paths with a parametalized parent', () {
      Router router = new Router();
      router.root
        ..addRoute(
          name: 'paramaddress',
          path: '/:zzzzzz/address',
          enter: expectAsync((RouteEvent e) {
            expect(e.path, '/foo/address');
            expect(router.root.findRoute('paramaddress').isActive, isTrue);
          }))
        ..addRoute(
          name: 'param_add',
          path: '/:aaaaaa/add',
          enter: (e) => fail('should invoke /foo/address'));
      return router.route('/foo/address');
    });

    test('add paths with a first param and one without', () {
      Router router = new Router();
      router.root
        ..addRoute(
          name: 'fooparam',
          path: '/:param/foo',
          enter: expectAsync((RouteEvent e) {
            expect(e.path, '/bar/foo');
            expect(router.root.findRoute('fooparam').isActive, isTrue);
          }))
        ..addRoute(
          name: 'bar',
          path: '/bar',
          enter: (e) => fail('should enter fooparam'));
      return router.route('/bar/foo');
    });

    test('add paths with a first param and one without 2', () {
      Router router = new Router();
      router.root
        ..addRoute(
          name: 'paramfoo',
          path: '/:param/foo',
          enter: (e) => fail('should enter barfoo'))
        ..addRoute(
          name: 'barfoo',
          path: '/bar/foo',
          enter: expectAsync((RouteEvent e) {
            expect(e.path, '/bar/foo');
            expect(router.root.findRoute('barfoo').isActive, isTrue);
          }))
        ;
      return router.route('/bar/foo');
    });

    test('add paths with a second param and one without', () {
      Router router = new Router();
      router.root
        ..addRoute(
          name: 'bazparamfoo',
          path: '/baz/:param/foo',
          enter: (e) => fail('should enter bazbarfoo'))
        ..addRoute(
          name: 'bazbarfoo',
          path: '/baz/bar/foo',
          enter: expectAsync((RouteEvent e) {
            expect(e.path, '/baz/bar/foo');
            expect(router.root.findRoute('bazbarfoo').isActive, isTrue);
          }))
        ;
      return router.route('/baz/bar/foo');
    });

    test('add paths with a first param and a second param', () {
      Router router = new Router();
      router.root
        ..addRoute(
          name: 'parambarfoo',
          path: '/:param/bar/foo',
          enter: (e) => fail('should enter bazparamfoo'))
        ..addRoute(
          name: 'bazparamfoo',
          path: '/baz/:param/foo',
          enter: expectAsync((RouteEvent e) {
            expect(e.path, '/baz/bar/foo');
            expect(router.root.findRoute('bazparamfoo').isActive, isTrue);
          }))
        ;
      return router.route('/baz/bar/foo');
    });

    test('add paths with two params and a param', () {
      Router router = new Router();
      router.root
        ..addRoute(
          name: 'param1param2foo',
          path: '/:param1/:param2/foo',
          enter: (e) => fail('should enter bazparamfoo'))
        ..addRoute(
          name: 'param1barfoo',
          path: '/:param1/bar/foo',
          enter: expectAsync((RouteEvent e) {
            expect(e.path, '/baz/bar/foo');
            expect(router.root.findRoute('param1barfoo').isActive, isTrue);
          }))
        ;
      return router.route('/baz/bar/foo');
    });
  });

  group('hierarchical routing', () {

    void _testParentChild(
        Pattern parentPath,
        Pattern childPath,
        String expectedParentPath,
        String expectedChildPath,
        String testPath) {
      var router = new Router();
      router.root.addRoute(
          name: 'parent',
          path: parentPath,
          enter: expectAsync((RouteEvent e) {
            expect(e.path, expectedParentPath);
            expect(e.route, isNotNull);
            expect(e.route.name, 'parent');
          }),
          mount: (Route child) {
            child.addRoute(
                name: 'child',
                path: childPath,
                enter: expectAsync((RouteEvent e) {
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
      var counters = <String, int>{
        'fooPreEnter': 0,
        'fooEnter': 0,
        'fooLeave': 0,
        'barPreEnter': 0,
        'barEnter': 0,
        'barLeave': 0,
        'bazPreEnter': 0,
        'bazEnter': 0,
        'bazLeave': 0
      };
      var router = new Router();
      router.root
        ..addRoute(path: '/foo',
            name: 'foo',
            preEnter: (_) => counters['fooPreEnter']++,
            enter: (_) => counters['fooEnter']++,
            leave: (_) => counters['fooLeave']++,
            mount: (Route route) => route
              ..addRoute(path: '/bar',
                  name: 'bar',
                  preEnter: (_) => counters['barPreEnter']++,
                  enter: (_) => counters['barEnter']++,
                  leave: (_) => counters['barLeave']++)
              ..addRoute(path: '/baz',
                  name: 'baz',
                  preEnter: (_) => counters['bazPreEnter']++,
                  enter: (_) => counters['bazEnter']++,
                  leave: (_) => counters['bazLeave']++));

      expect(counters, {
        'fooPreEnter': 0,
        'fooEnter': 0,
        'fooLeave': 0,
        'barPreEnter': 0,
        'barEnter': 0,
        'barLeave': 0,
        'bazPreEnter': 0,
        'bazEnter': 0,
        'bazLeave': 0
      });

      router.route('/foo/bar').then(expectAsync((_) {
        expect(counters, {
          'fooPreEnter': 1,
          'fooEnter': 1,
          'fooLeave': 0,
          'barPreEnter': 1,
          'barEnter': 1,
          'barLeave': 0,
          'bazPreEnter': 0,
          'bazEnter': 0,
          'bazLeave': 0
        });

        router.route('/foo/baz').then(expectAsync((_) {
          expect(counters, {
            'fooPreEnter': 1,
            'fooEnter': 1,
            'fooLeave': 0,
            'barPreEnter': 1,
            'barEnter': 1,
            'barLeave': 1,
            'bazPreEnter': 1,
            'bazEnter': 1,
            'bazLeave': 0
          });
        }));
      }));
    });

    void _testAllowLeave(bool allowLeave) {
      var completer = new Completer<bool>();
      bool barEntered = false;
      bool bazEntered = false;

      var router = new Router();
      router.root
        ..addRoute(name: 'foo', path: '/foo',
            mount: (Route child) => child
              ..addRoute(name: 'bar', path: '/bar',
                  enter: (RouteEnterEvent e) => barEntered = true,
                  leave: (RouteLeaveEvent e) => e.allowLeave(completer.future))
              ..addRoute(name: 'baz', path: '/baz',
                  enter: (RouteEnterEvent e) => bazEntered = true));

      router.route('/foo/bar').then(expectAsync((_) {
        expect(barEntered, true);
        expect(bazEntered, false);
        router.route('/foo/baz').then(expectAsync((_) {
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

  group('preEnter', () {

    void _testAllowEnter(bool allowEnter) {
      var completer = new Completer<bool>();
      bool barEntered = false;

      var router = new Router();
      router.root
        ..addRoute(name: 'foo', path: '/foo',
            mount: (Route child) => child
              ..addRoute(name: 'bar', path: '/bar',
                  enter: (RouteEnterEvent e) => barEntered = true,
                  preEnter: (RoutePreEnterEvent e) =>
                      e.allowEnter(completer.future)));

      router.route('/foo/bar').then(expectAsync((_) {
        expect(barEntered, allowEnter);
      }));
      completer.complete(allowEnter);
    }

    test('should allow navigation', () {
      _testAllowEnter(true);
    });

    test('should veto navigation', () {
      _testAllowEnter(false);
    });

    test('should allow prevent leaving on parameter changes', () {
      var counters = <String, int>{
          'fooPreEnter': 0,
          'fooEnter': 0,
          'fooLeave': 0,
          'barPreEnter': 0,
          'barEnter': 0,
          'barLeave': 0
      };
      var router = new Router();
      router.root
        ..addRoute(path: r'/foo/:param',
            name: 'foo',
            preEnter: (_) => counters['fooPreEnter']++,
            enter: (_) => counters['fooEnter']++,
            leave: (_) => counters['fooLeave']++,
            dontLeaveOnParamChanges: true)
        ..addRoute(path: '/bar',
              name: 'bar',
              preEnter: (_) => counters['barPreEnter']++,
              enter: (_) => counters['barEnter']++,
              leave: (_) => counters['barLeave']++);

      expect(counters, {
          'fooPreEnter': 0,
          'fooEnter': 0,
          'fooLeave': 0,
          'barPreEnter': 0,
          'barEnter': 0,
          'barLeave': 0
      });

      router.route('/foo/bar').then(expectAsync((_) {
        expect(counters, {
            'fooPreEnter': 1,
            'fooEnter': 1,
            'fooLeave': 0,
            'barPreEnter': 0,
            'barEnter': 0,
            'barLeave': 0
        });

        router.route('/foo/bar').then(expectAsync((_) {
          expect(counters, {
              'fooPreEnter': 1,
              'fooEnter': 1,
              'fooLeave': 0,
              'barPreEnter': 0,
              'barEnter': 0,
              'barLeave': 0
          });

          router.route('/foo/baz').then(expectAsync((_) {
            expect(counters, {
                'fooPreEnter': 2,
                'fooEnter': 2,
                'fooLeave': 0,
                'barPreEnter': 0,
                'barEnter': 0,
                'barLeave': 0
            });

            router.route('/bar').then(expectAsync((_) {
              expect(counters, {
                  'fooPreEnter': 2,
                  'fooEnter': 2,
                  'fooLeave': 1,
                  'barPreEnter': 1,
                  'barEnter': 1,
                  'barLeave': 0
              });
            }));
          }));
        }));
      }));
    });
  });

  group('Default route', () {

    void _testHeadTail(String path, String expectFoo, String expectBar) {
      var router = new Router();
      router.root
        ..addRoute(
            name: 'foo',
            path: '/foo',
            defaultRoute: true,
            enter: expectAsync((RouteEvent e) {
              expect(e.path, expectFoo);
            }),
            mount: (child) => child
              ..addRoute(
                  name: 'bar',
                  path: '/bar',
                  defaultRoute: true,
                  enter: expectAsync((RouteEvent e) =>
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
      var counters = <String, int>{
        'list_entered': 0,
        'article_123_entered': 0,
        'article_123_view_entered': 0,
        'article_123_edit_entered': 0
      };

      var router = new Router();
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

    test('shoud use location.assign/.replace when useFragment=true', () {
      var mockWindow = new MockWindow();
      var router = new Router(useFragment: true, windowImpl: mockWindow);
      router.root.addRoute(name: 'articles', path: '/articles');

      router.go('articles', {}).then(expectAsync((_) {
        var mockLocation = mockWindow.location;

        mockLocation.getLogs(callsTo('assign', anything))
            .verify(happenedExactly(1));
        expect(mockLocation.getLogs(callsTo('assign', anything)).last.args,
            ['#/articles']);
        mockLocation.getLogs(callsTo('replace', anything))
            .verify(happenedExactly(0));

        router.go('articles', {}, replace: true).then(expectAsync((_) {
          mockLocation.getLogs(callsTo('replace', anything))
              .verify(happenedExactly(1));
          expect(mockLocation.getLogs(callsTo('replace', anything)).last.args,
              ['#/articles']);
          mockLocation.getLogs(callsTo('assign', anything))
              .verify(happenedExactly(1));
        }));
      }));
    });

    test('shoud use history.push/.replaceState when useFragment=false', () {
      var mockWindow = new MockWindow();
      var router = new Router(useFragment: false, windowImpl: mockWindow);
      router.root.addRoute(name: 'articles', path: '/articles');

      router.go('articles', {}).then(expectAsync((_) {
        var mockHistory = mockWindow.history;

        mockHistory.getLogs(callsTo('pushState', anything))
            .verify(happenedExactly(1));
        expect(mockHistory.getLogs(callsTo('pushState', anything)).last.args,
            [null, '', '/articles']);
        mockHistory.getLogs(callsTo('replaceState', anything))
            .verify(happenedExactly(0));

        router.go('articles', {}, replace: true).then(expectAsync((_) {
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
      var mockWindow = new MockWindow();
      var router = new Router(windowImpl: mockWindow);
      router.root
        ..addRoute(
            name: 'a',
            path: '/:foo',
            mount: (child) => child
              ..addRoute(
                  name: 'b',
                  path: '/:bar'));

      var routeA = router.root.findRoute('a');

      router.go('a.b', {}).then(expectAsync((_) {
        var mockHistory = mockWindow.history;

        mockHistory.getLogs(callsTo('pushState', anything))
            .verify(happenedExactly(1));
        expect(mockHistory.getLogs(callsTo('pushState', anything)).last.args,
            [null, '', '/null/null']);

        router.go('a.b', {'foo': 'aaaa', 'bar': 'bbbb'}).then(expectAsync((_) {
          mockHistory.getLogs(callsTo('pushState', anything))
              .verify(happenedExactly(2));
          expect(mockHistory.getLogs(callsTo('pushState', anything)).last.args,
              [null, '', '/aaaa/bbbb']);

          router.go('b', {'bar': 'bbbb'}, startingFrom: routeA)
              .then(expectAsync((_) {
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
      var counters = <String, int>{
        'aEnter': 0,
        'bEnter': 0
      };

      var mockWindow = new MockWindow();
      var router = new Router(windowImpl: mockWindow);
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

        var routeA = router.root.findRoute('a');
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
      var mockWindow = new MockWindow();
      var router = new Router(windowImpl: mockWindow);
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

      var routeA = router.root.findRoute('a');

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

  group('findRoute', () {

    test('should return correct routes', () {
      Route routeFoo, routeBar, routeBaz, routeQux, routeAux;

      var router = new Router();
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

      expect(router.root.findRoute('foo'), same(routeFoo));
      expect(router.root.findRoute('foo.bar'), same(routeBar));
      expect(routeFoo.findRoute('bar'), same(routeBar));
      expect(router.root.findRoute('foo.bar.baz'), same(routeBaz));
      expect(router.root.findRoute('foo.qux'), same(routeQux));
      expect(router.root.findRoute('foo.qux.aux'), same(routeAux));
      expect(routeQux.findRoute('aux'), same(routeAux));
      expect(routeFoo.findRoute('qux.aux'), same(routeAux));

      expect(router.root.findRoute('baz'), isNull);
      expect(router.root.findRoute('foo.baz'), isNull);
    });

  });

  group('route', () {

    test('should parse query', () {
      var router = new Router();
      router.root
        ..addRoute(
            name: 'foo',
            path: '/:foo',
            enter: expectAsync((RouteEvent e) {
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
        var router = new Router();
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
        var router = new Router();
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
      var router = new Router();
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

  group('listen', () {

    group('fragment', () {

      test('shoud route current hash on listen', () {
        var mockWindow = new MockWindow();
        var mockHashChangeController = new StreamController<Event>(sync: true);

        mockWindow.when(callsTo('get onHashChange'))
            .alwaysReturn(mockHashChangeController.stream);
        mockWindow.location.when(callsTo('get hash')).alwaysReturn('#/foo');
        var router = new Router(useFragment: true, windowImpl: mockWindow);
        router.root.addRoute(name: 'foo', path: '/foo');
        router.onRouteStart.listen(expectAsync((RouteStartEvent start) {
          start.completed.then(expectAsync((_) {
            expect(router.findRoute('foo').isActive, isTrue);
          }));
        }, count: 1));
        router.listen(ignoreClick: true);
      });

    });

    group('pushState', () {

      testInit(mockWindow, [count = 1]) {
        mockWindow.location.when(callsTo('get hash')).alwaysReturn('');
        mockWindow.location.when(callsTo('get pathname')).alwaysReturn('/foo');
        var router = new Router(useFragment: false, windowImpl: mockWindow);
        router.root.addRoute(name: 'foo', path: '/foo');
        router.onRouteStart.listen(expectAsync((RouteStartEvent start) {
          start.completed.then(expectAsync((_) {
            expect(router.findRoute('foo').isActive, isTrue);
          }));
        }, count: count));
        router.listen(ignoreClick: true);
      }

      test('shoud route current path on listen with pop', () {
        var mockWindow = new MockWindow();
        var mockPopStateController = new StreamController<Event>(sync: true);
        mockWindow.when(callsTo('get onPopState'))
            .alwaysReturn(mockPopStateController.stream);
        testInit(mockWindow, 2);
        mockPopStateController.add(null);
      });

      test('shoud route current path on listen without pop', () {
        var mockWindow = new MockWindow();
        var mockPopStateController = new StreamController<Event>(sync: true);
        mockWindow.when(callsTo('get onPopState'))
            .alwaysReturn(mockPopStateController.stream);
        testInit(mockWindow);
      });

    });

  });

}

/// An alias for Router.root.findRoute(path)
r(Router router, String path) => router.root.findRoute(path);
