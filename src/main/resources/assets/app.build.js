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
    optimize: "closure",
    closure: {
      CompilerOptions: {
      },
      charset: 'UTF-8',
      CompilationLevel: 'SIMPLE_OPTIMIZATIONS',
      loggingLevel: 'SEVERE'
    },
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
        name: "styles",
        include: [
          "fonts"
        ]
      },
      {
        name: "fonts"
      }
    ]
})
