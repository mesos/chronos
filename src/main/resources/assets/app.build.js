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
      CompilerOptions: {},
      CompilationLevel: 'SIMPLE_OPTIMIZATIONS',
      loggingLevel: 'WARNING'
    },
    fileExclusionRegExp: /^\.|spec/,
    generateSourceMaps: true,
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
