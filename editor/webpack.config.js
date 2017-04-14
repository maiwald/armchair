let sourceDir = __dirname + '/src';

module.exports = {
  context: sourceDir,
  entry: './application.js',
  output: {
    path: __dirname + '/build',
    filename: 'bundle.js'
  },
  module: {
    loaders: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['es2015', 'react']
          }
        }
      },
      {
        test: /\.s?css$/,
        exclude: /node_modules/,
        use: [
          { loader: 'style-loader' },
          {
            loader: 'css-loader',
            options: {
              modules: true,
              camelCase: true
            }
          },
          { loader: 'sass-loader' }
        ]
      }
    ]
  },
  resolve: {
    modules: [sourceDir, __dirname + '/node_modules']
  }
};
