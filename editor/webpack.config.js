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
            presets: ["es2015", "react"]
          }
        }
      }
    ]
  }
};
