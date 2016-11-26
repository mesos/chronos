var path = require('path');
var webpack = require('webpack');
var ExtractTextPlugin = require("extract-text-webpack-plugin");

module.exports = {
  devtool: 'eval',
  entry: [
    'webpack-hot-middleware/client?reload=true',
    './components/Root.js'
  ],
  output: {
    path: path.join(__dirname, 'assets/js'),
    filename: 'bundle.js',
    publicPath: '/assets/js',
  },
  plugins: [
    new webpack.optimize.OccurenceOrderPlugin(),
    new webpack.HotModuleReplacementPlugin(),
    new webpack.NoErrorsPlugin(),
    new webpack.DefinePlugin({
      'process.env': { NODE_ENV: JSON.stringify(process.env.NODE_ENV) },
      __CLIENT__: JSON.stringify(true),
      __SERVER__: JSON.stringify(false),
    }),
    new webpack.ProvidePlugin({
        jQuery: 'jquery',
        $: 'jquery',
        jquery: 'jquery'
    }),
  ],
  resolve: {
    extensions: ['', '.js', '.jsx']
  },
  module: {
    loaders: [{
      test: /\.jsx?$/,
      loader: 'babel',
      exclude: /(node_modules)/,
    },
  ]
  }
};
