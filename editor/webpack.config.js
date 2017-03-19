module.exports = {
  entry: './app/assets/javascripts/application.js',
  output: {
    path: './public/target',
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
            presets: ["es2015", "react"]
          }
        }
      }
    ]
  }
};
