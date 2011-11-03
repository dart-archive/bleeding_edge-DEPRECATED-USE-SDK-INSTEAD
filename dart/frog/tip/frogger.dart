#import('dart:dom');
#import('compiler.dart');

void main() {
  if (!document.implementation.hasFeature('dart', '')) {
    final systemPath = getRootPath(window) + '/..';
    final userPath = getRootPath(window.parent);
    window.addEventListener('DOMContentLoaded',
                            (e) => frogify(systemPath, userPath, window.parent),
                            false);
  }
}
