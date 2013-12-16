({
  appDir: './app',
  baseUrl: 'scripts',
  mainConfigFile: './app/scripts/main.js',
  dir: './build',
  pragmasOnSave: {
    excludeCoffeeScript: true,
    excludeTpl: true
  },
  excludeShallow: [
    'css-builder',
    'less-builder',
    'lessc-server'
  ],
  findNestedDependencies: true,
  optimize: 'uglify',
  optimizeCss: 'standard.keepLines',
  fileExclusionRegExp: /^\.|spec|tests/,
  generateSourceMaps: false,
  preserveLicenseComments: false,
  skipDirOptimize: true,
  modules: [
    {
      name: 'main',
      include: [
        'styles'
      ],
      excludeShallow: [
        'spec_runner'
      ]
    },
    {
      name: 'styles'
    }
  ],
  uglify: {
    beautify: false
  }
})
