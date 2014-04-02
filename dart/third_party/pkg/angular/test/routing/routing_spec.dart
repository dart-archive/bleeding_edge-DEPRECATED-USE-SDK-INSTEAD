library routing_spec;

import '../_specs.dart';
import 'package:angular/mock/module.dart';
import 'dart:async';

main() {
  describe('routing', () {
    TestBed _;
    Router router;

    beforeEach(module((Module m) {
      _initRoutesCalls = 0;
      _router = null;
      router = new Router(useFragment: false, windowImpl: new MockWindow());
      m
        ..install(new AngularMockModule())
        ..factory(RouteInitializerFn, (_) => initRoutes)
        ..value(Router, router);
    }));

    beforeEach(inject((TestBed tb) {
      _ = tb;
    }));

    it('should call init of the RouteInitializer once', async(() {
      expect(_initRoutesCalls).toEqual(0);

      // Force the routing system to initialize.
      _.compile('<ng-view></ng-view>');

      expect(_initRoutesCalls).toEqual(1);
      expect(_router).toBe(router);
    }));

  });

  describe('routing DSL', () {
    Router router;
    TestBed _;

    afterEach(() {
      router = _ = null;
    });

    initRouter(initializer) {
      var module = new Module()
          ..value(RouteInitializerFn, initializer);
      var injector = new DynamicInjector(
          modules: [new AngularModule(), new AngularMockModule(), module]);
      injector.get(NgRoutingHelper); // force routing initialization
      router = injector.get(Router);
      _ = injector.get(TestBed);
    }

    it('should configure route hierarchy from provided config', async(() {
      var counters = {
        'foo': 0,
        'bar': 0,
        'baz': 0,
        'aux': 0,
      };
      initRouter((Router router, ViewFactory views) {
        views.configure({
          'foo': ngRoute(
              path: '/foo',
              enter: (_) => counters['foo']++,
              mount: {
                'bar': ngRoute(
                    path: '/bar',
                    enter: (_) => counters['bar']++
                ),
                'baz': ngRoute(
                    path: '/baz',
                    enter: (_) => counters['baz']++
                )
              }
          ),
          'aux': ngRoute(
              path: '/aux',
              enter: (_) => counters['aux']++
          )
        });
      });

      expect(router.root.getRoute('foo').name).toEqual('foo');
      expect(router.root.getRoute('foo.bar').name).toEqual('bar');
      expect(router.root.getRoute('foo.baz').name).toEqual('baz');
      expect(router.root.getRoute('aux').name).toEqual('aux');

      router.route('/foo');
      microLeap();
      expect(counters, equals({
        'foo': 1,
        'bar': 0,
        'baz': 0,
        'aux': 0,
      }));

      router.route('/foo/bar');
      microLeap();
      expect(counters, equals({
        'foo': 1,
        'bar': 1,
        'baz': 0,
        'aux': 0,
      }));

      router.route('/foo/baz');
      microLeap();
      expect(counters, equals({
        'foo': 1,
        'bar': 1,
        'baz': 1,
        'aux': 0,
      }));

      router.route('/aux');
      microLeap();
      expect(counters, equals({
        'foo': 1,
        'bar': 1,
        'baz': 1,
        'aux': 1,
      }));
    }));


    it('should set the default route', async(() {
      int enterCount = 0;
      initRouter((Router router, ViewFactory views) {
        views.configure({
          'foo': ngRoute(path: '/foo'),
          'bar': ngRoute(path: '/bar', defaultRoute: true),
          'baz': ngRoute(path: '/baz'),
        });
      });

      router.route('/invalidRoute');
      microLeap();

      expect(router.activePath.length).toBe(1);
      expect(router.activePath.first.name).toBe('bar');
    }));


    it('should call enter callback and show the view when routed', async(() {
      int enterCount = 0;
      initRouter((Router router, ViewFactory views) {
        views.configure({
          'foo': ngRoute(
              path: '/foo',
              enter: (_) => enterCount++,
              view: 'foo.html'
          ),
        });
      });
      _.injector.get(TemplateCache)
          .put('foo.html', new HttpResponse(200, '<h1>Foo</h1>'));

      Element root = _.compile('<ng-view></ng-view>');
      expect(root.text).toEqual('');

      router.route('/foo');
      microLeap();

      expect(enterCount).toBe(1);
      expect(root.text).toEqual('Foo');
    }));


    it('should call preEnter callback and load modules', async(() {
      int preEnterCount = 0;
      int modulesCount = 0;
      initRouter((Router router, ViewFactory views) {
        views.configure({
          'foo': ngRoute(
              path: '/foo',
              preEnter: (_) => preEnterCount++,
              modules: () {
                modulesCount++;
                return new Future.value();
              }
          ),
          'bar': ngRoute(
              path: '/bar'
          )
        });
      });

      router.route('/foo');
      microLeap();

      expect(preEnterCount).toBe(1);
      expect(modulesCount).toBe(1);

      router.route('/foo');
      microLeap();

      expect(preEnterCount).toBe(1);
      expect(modulesCount).toBe(1);

      router.route('/bar');
      microLeap();

      expect(preEnterCount).toBe(1);
      expect(modulesCount).toBe(1);

      router.route('/foo');
      microLeap();

      expect(preEnterCount).toBe(2);
      expect(modulesCount).toBe(1);
    }));


    it('should clear view on leave an call leave callback', async(() {
      int leaveCount = 0;
      initRouter((Router router, ViewFactory views) {
        views.configure({
          'foo': ngRoute(
              path: '/foo',
              leave: (_) => leaveCount++,
              view: 'foo.html'
          ),
          'bar': ngRoute(
              path: '/bar'
          ),
        });
      });
      _.injector.get(TemplateCache)
          .put('foo.html', new HttpResponse(200, '<h1>Foo</h1>'));

      Element root = _.compile('<ng-view></ng-view>');
      expect(root.text).toEqual('');

      router.route('/foo');
      microLeap();

      expect(root.text).toEqual('Foo');
      expect(leaveCount).toBe(0);

      router.route('/bar');
      microLeap();

      expect(root.text).toEqual('');
      expect(leaveCount).toBe(1);
    }));


    it('should synchronously load new directives from modules ', async(() {
      initRouter((Router router, ViewFactory views) {
        views.configure({
          'foo': ngRoute(
              path: '/foo',
              modules: () => [
                new Module()..type(NewDirective)
              ],
              view: 'foo.html'
          ),
        });
      });
      _.injector.get(TemplateCache)
          .put('foo.html', new HttpResponse(200, '<div make-it-new>Old!</div>'));

      Element root = _.compile('<ng-view></ng-view>');
      expect(root.text).toEqual('');

      router.route('/foo');
      microLeap();

      expect(root.text).toEqual('New!');
    }));


    it('should asynchronously load new directives from modules ', async(() {
      initRouter((Router router, ViewFactory views) {
        views.configure({
          'foo': ngRoute(
              path: '/foo',
              modules: () => new Future.value([
                new Module()..type(NewDirective)
              ]),
              view: 'foo.html'
          ),
        });
      });
      _.injector.get(TemplateCache)
          .put('foo.html', new HttpResponse(200, '<div make-it-new>Old!</div>'));

      Element root = _.compile('<ng-view></ng-view>');
      expect(root.text).toEqual('');

      router.route('/foo');
      microLeap();

      expect(root.text).toEqual('New!');
    }));


    it('should synchronously load new filters from modules ', async(() {
      initRouter((Router router, ViewFactory views) {
        views.configure({
          'foo': ngRoute(
              path: '/foo',
              modules: () => [
                new Module()..type(HelloFilter)
              ],
              view: 'foo.html'
          ),
        });
      });
      _.injector.get(TemplateCache)
          .put('foo.html', new HttpResponse(200, '<div>{{\'World\' | hello}}</div>'));

      Element root = _.compile('<ng-view></ng-view>');
      expect(root.text).toEqual('');

      router.route('/foo');
      microLeap();
      _.rootScope.apply();

      expect(root.text).toEqual('Hello, World!');
    }));


    it('should asynchronously load new filters from modules ', async(() {
      initRouter((Router router, ViewFactory views) {
        views.configure({
          'foo': ngRoute(
              path: '/foo',
              modules: () => new Future.value([
                new Module()..type(HelloFilter)
              ]),
              view: 'foo.html'
          ),
        });
      });
      _.injector.get(TemplateCache)
          .put('foo.html', new HttpResponse(200, '<div>{{\'World\' | hello}}</div>'));

      Element root = _.compile('<ng-view></ng-view>');
      expect(root.text).toEqual('');

      router.route('/foo');
      microLeap();
      _.rootScope.apply();

      expect(root.text).toEqual('Hello, World!');
    }));

  });
}

var _router;
var _initRoutesCalls = 0;

void initRoutes(Router router, ViewFactory view) {
  _initRoutesCalls++;
  _router = router;
}

@NgDirective(selector: '[make-it-new]')
class NewDirective {
  NewDirective(Element element) {
    element.innerHtml = 'New!';
  }
}

@NgFilter(name:'hello')
class HelloFilter {
  call(String str) {
    return 'Hello, $str!';
  }
}

