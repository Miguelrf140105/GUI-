module.exports = function (config) {
  if (process.platform === 'win32' && !process.env.CHROME_BIN) {
    const fs = require('fs');
    const path = require('path');
    const browser = [
      path.join(process.env.ProgramFiles || '', 'Google/Chrome/Application/chrome.exe'),
      path.join(process.env['ProgramFiles(x86)'] || '', 'Microsoft/Edge/Application/msedge.exe'),
    ].find((file) => fs.existsSync(file));
    if (browser) process.env.CHROME_BIN = browser;
  }
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('@angular-devkit/build-angular/plugins/karma'),
    ],
    reporters: ['progress'],
    port: 9876,
    hostname: '127.0.0.1',
    listenAddress: '127.0.0.1',
    colors: false,
    autoWatch: false,
    browsers: ['ChromeHeadless'],
    singleRun: true,
    restartOnFileChange: false,
    browserNoActivityTimeout: 60000,
    client: { clearContext: false },
  });
};
