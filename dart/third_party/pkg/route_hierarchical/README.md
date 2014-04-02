Route
=====

Route is a client + server routing library for Dart that helps make building
single-page web apps and using `HttpServer` easier.

Installation
------------

Add this package to your pubspec.yaml file:

    dependencies:
      route_hierarchical: any

Then, run `pub install` to download and link in the package.

UrlMatcher
----------
Route is built around `UrlMatcher`, an interface that defines URL template
parsing, matching and reversing.


UrlTemplate
-----------
The default implementation of the `UrlMatcher` is `UrlTemplate`. As an example,
consider a blog with a home page and an article page. The article URL has the
form /article/1234. It can matched by the following template:
`/article/:articleId`.

Client Routing
--------------

Router is a stateful object that contains routes and can perform URL routing
on those routes.

The `Router` can listen to `Window.onPopState` (or fallback to
Window.onHashChange in older browsers) events and invoke the correct
handler so that the back button seamlessly works.

Example (client.dart):

```dart
library client;

import 'package:route_hierarchical/client.dart';

main() {
  var router = new Router();
  router.root
    ..addRoute(name: 'article', path: '/article/:articleId', enter: showArticle)
    ..addRoute(name: 'home', defaultRoute: true, path: '/', enter: showHome);
  router.listen();
}

void showHome(RouteEvent e) {
  // nothing to parse from path, since there are no groups
}

void showArticle(RouteEvent e) {
  var articleId = e.parameters['articleId'];
  // show article page with loading indicator
  // load article from server, then render article
}
```

The client side router can let you define nested routes.

```dart
var router = new Router();
router.root
  ..addRoute(
     name: 'usersList',
     path: '/users',
     defaultRoute: true,
     enter: showUsersList)
  ..addRoute(
     name: 'user',
     path: '/user/:userId',
     mount: (router) =>
       router
         ..addDefaultRoute(
             name: 'articleList',
             path: '/acticles',
             enter: showArticlesList)
         ..addRoute(
             name: 'article',
             path: '/article/:articleId',
             mount: (router) =>
               router
                 ..addDefaultRoute(
                     name: 'view',
                     path: '/view',
                     enter: viewArticle)
                 ..addRoute(
                     name: 'edit',
                     path: '/edit',
                     enter: editArticle)))
```

The mount parameter takes either a function that accepts an instance of a new
child router as the only parameter, or an instance of an object that implements
Routable interface.

```dart
typedef void MountFn(Router router);
```

or

```dart
abstract class Routable {
  void configureRoute(Route router);
}
```

In either case, the child router is instantiated by the parent router an
injected into the mount point, at which point child router can be configured
with new routes.

Routing with hierarchical router: when the parent router performs a prefix
match on the URL, it removes the matched part from the URL and invokes the
child router with the remaining tail.

For instance, with the above example lets consider this URL: `/user/jsmith/article/1234`.
Route "user" will match `/user/jsmith` and invoke the child router with `/article/1234`.
Route "article" will match `/article/1234` and invoke the child router with ``.
Route "view" will be matched as the default route.
The resulting route path will be: `user -> article -> view`, or simply `user.article.view`

Named Routes in Hierarchical Routers
------------------------------------

```dart
router.go('usersList');
router.go('user.articles', {'userId': 'jsmith'});
router.go('user.article.view', {
  'userId': 'jsmith',
  'articleId', 1234}
);
router.go('user.article.edit', {
  'userId': 'jsmith',
  'articleId', 1234}
);
```

If "go" is invoked on child routers, the router can automatically reconstruct
and generate the new URL from the state in the parent routers.

Server Routing
--------------

On the server, route gives you a utility function to match `HttpRequest`s
against `UrlPatterns`.

```dart
import 'urls.dart';
import 'package:route_hierarchical/server.dart';
import 'package:route_hierarchical/pattern.dart';

HttpServer.bind('0.0.0.0', 8888).then((server) {
  var router = new Router(server)
    ..filter(matchesAny(allUrls), authFilter)
    ..serve(homeUrl).listen(serverHome)
    ..serve(articleUrl, method: 'GET').listen(serveArticle);
});

Future<bool> authFilter(req) {
  return getUser(getUserIdCookie(req)).then((user) {
    if (user != null) {
      return true;
    }
    redirectToLoginPage(req);
    return false;
  });
}

serveArcticle(req) {
  var articleId = articleUrl.parse(req.path)[0];
  // retrieve article data and respond
}
```

Further Goals
-------------

 * Integration with Web UI so that the changing of UI views can happen
   automatically.
 * Handling different HTTP methods to help implement REST APIs.
 * Automatic generation of REST URLs from a single URL pattern, similar to Ruby
   on Rails
 * Helpers for nested views and key-value URL schemes common with complex apps.
 * [Done] ~~Server-side routing for the dart:io v2 HttpServer~~
 * [Done] ~~IE 9 support~~
