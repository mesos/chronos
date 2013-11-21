({
    appDir: "./app",
    baseUrl: "scripts",
    mainConfigFile: "./app/scripts/main.js",
    dir: "./build",
    pragmasOnSave: {
      excludeTpl: true
    },
    excludeShallow: [
      'css-builder',
      'less-builder',
      'lessc-server'
    ],
    findNestedDependencies: true,
    optimize: "uglify",
    fileExclusionRegExp: /^\.|spec|tests/,
    optimizeCss: "standard.keepLines",
    generateSourceMaps: false,
    preserveLicenseComments: false,
    modules: [
      {
        name: "main",
        include: [
          "jquery",
          "styles"
        ],
        excludeShallow: [
          'spec_runner'
        ]
      },
      {
        name: "styles"
      }
    ]
})
