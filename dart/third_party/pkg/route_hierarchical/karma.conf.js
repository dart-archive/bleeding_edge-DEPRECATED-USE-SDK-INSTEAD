module.exports = function(config) {
  config.set({
    //logLevel: config.LOG_DEBUG,
    basePath: '.',
    frameworks: ['dart-unittest'],

    // list of files / patterns to load in the browser
    // all tests must be 'included', but all other libraries must be 'served' and
    // optionally 'watched' only.
    files: [
      'test/*.dart',
      {pattern: '**/*.dart', watched: true, included: false, served: true},
      'packages/browser/dart.js',
      'packages/browser/interop.js'
    ],

    autoWatch: false,

    // If browser does not capture in given timeout [ms], kill it
    captureTimeout: 10000,
    // 5 minutes is enough time for dart2js to run on Travis...
    browserNoActivityTimeout: 300000,

    plugins: [
      'karma-dart',
      'karma-chrome-launcher',
      'karma-firefox-launcher',
      'karma-script-launcher',
      'karma-junit-reporter'
    ],

    customLaunchers: {
      Dartium: { base: 'ChromeCanary', flags: ['--no-sandbox'] },
      ChromeNoSandbox: { base: 'Chrome', flags: ['--no-sandbox'] }
    },

    junitReporter: {
      outputFile: 'test/out/unit.xml',
      suite: 'unit'
    }
  });
};
