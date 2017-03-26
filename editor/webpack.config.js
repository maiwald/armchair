module.exports = {
  context: __dirname + '/app/assets/javascripts',
  entry: './application.js',
  output: {
    path: __dirname + '/public/target',
    filename: 'application.js'
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
    modules: [
      __dirname + '/app/assets/javascripts',
      __dirname + '/node_modules'
    ]
  }
};
