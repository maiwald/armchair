const ExtractTextPlugin = require("extract-text-webpack-plugin");
const sourceDir = __dirname + "/src";
const FlowBabelWebpackPlugin = require("flow-babel-webpack-plugin");

module.exports = {
  context: sourceDir,
  entry: "./application.js",
  output: {
    path: __dirname + "/build",
    filename: "bundle.js"
  },
  resolve: {
    modules: [sourceDir, __dirname + "/node_modules"]
  },
  module: {
    loaders: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: {
          loader: "babel-loader",
          options: {
            presets: [["es2015", { modules: false }], "react"],
            plugins: [require("babel-plugin-transform-object-rest-spread")]
          }
        }
      },
      {
        test: /\.s?css$/,
        exclude: /node_modules/,
        use: ExtractTextPlugin.extract({
          fallback: "style-loader",
          use: [
            {
              loader: "css-loader",
              options: {
                modules: true,
                camelCase: true
              }
            },
            { loader: "sass-loader" }
          ]
        })
      }
    ]
  },
  plugins: [new ExtractTextPlugin("style.css"), new FlowBabelWebpackPlugin()]
};
