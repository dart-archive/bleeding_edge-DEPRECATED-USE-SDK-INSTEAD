
if (navigator.webkitStartDart) {
  navigator.webkitStartDart();
} else {
  var script = document.createElement('script');
  script.src = '{name.lower}.dart.precompiled.js';
  document.body.appendChild(script);
}
