/**
 * Creates a new window on application launch.
 *
 * @see http://developer.chrome.com/trunk/apps/app.runtime.html
 * @see http://developer.chrome.com/trunk/apps/app.window.html
 */
chrome.app.runtime.onLaunched.addListener(function() {
  chrome.app.window.create('{name.lower}.html',
    {id: '{name.lower}', width: 800, height: 600});
});
