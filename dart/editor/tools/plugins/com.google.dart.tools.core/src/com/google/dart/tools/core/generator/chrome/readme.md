For more information about packaged apps, see 
http://developer.chrome.com/apps/about_apps.html.

`web/manifest.json` describes the Chrome packaged application.

`web/main.js` is the entry point to the application; it launches 
`web/{name.lower}.html`. `web/{name.lower}.html` and `web/{name.lower}.dart` 
are the entry points to the Dart application.

`web/{name.lower}.html` script tags included and modified since chrome apps 
don't have dart support, only javascript should be loaded. 
