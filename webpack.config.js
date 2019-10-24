const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin')
const CopyPlugin = require('copy-webpack-plugin');
const path = require('path');

const config = {
  target: 'web',
  entry: {
    index: './src/index.js',
    setup: './src/assets/js/setup.js',
    sync: './src/assets/js/loop.js',
  },
  output: {
    path: path.join(__dirname, 'dist'),
    filename: './js/[name].bundle.js',
    library: '[name]'
  },
  devServer: {
    contentBase: path.join(__dirname, 'dist'),
    compress: true,
    port: 9000,
    watchContentBase: true
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        use: 'babel-loader',
        exclude: /node_modules/
      }
    ]
  },
  plugins: [
    new webpack.DefinePlugin({
      dataSyncVersion: JSON.stringify(require('./package.json').version)
    }),
    new CopyPlugin([
      { from: './src/assets', to: './assets' },
      { from: './node_modules/@mdi/font/fonts', to: './assets/mdi/fonts' },
      { from: './node_modules/@mdi/font/css', to: './assets/mdi/css' },
    ]),
    new HtmlWebpackPlugin({
      template: './src/index.html',
      filename: 'index.html',
      chunks: ['index']
    }),
    new HtmlWebpackPlugin({
      template: './src/sync.html',
      filename: 'sync.html',
      chunks: ['sync']
    }),
    new HtmlWebpackPlugin({
      template: './src/setup-pages/step1.html',
      filename: './setup/step1.html',
      chunks: ['setup']
    }),
    new HtmlWebpackPlugin({
      template: './src/setup-pages/step2.html',
      filename: './setup/step2.html',
      chunks: ['setup']
    }),
    new HtmlWebpackPlugin({
      template: './src/setup-pages/step3.html',
      filename: './setup/step3.html',
      chunks: ['setup']
    }),
    new HtmlWebpackPlugin({
      template: './src/setup-pages/step4.html',
      filename: './setup/step4.html',
      chunks: ['setup']
    })
  ]
};

module.exports = config;