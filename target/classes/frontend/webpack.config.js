// webpack.config.js

const path = require('path');

module.exports = {
    mode: 'development', // Change to 'production' for optimized builds
    entry: './src/index.js', // Your app's entry point
    output: {
        path: path.resolve(__dirname, 'dist'),
        filename: 'bundle.js',
        publicPath: '/', // Necessary for proper routing in SPA
    },
    module: {
        rules: [
            {
                test: /\.(js|jsx)$/, // Process both JS and JSX files
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader', // Transpile your JS/JSX
                    options: {
                        presets: ['@babel/preset-env', '@babel/preset-react'],
                    },
                },
            },
            {
                test: /\.css$/, // Process CSS files
                use: [
                    'style-loader',  // Injects styles into DOM
                    'css-loader',    // Turns CSS into CommonJS
                    'postcss-loader' // Processes CSS with PostCSS (Tailwind CSS, Autoprefixer)
                ],
            },
            // Add loaders for other assets (images, fonts, etc.) as needed.
        ],
    },
    resolve: {
        extensions: ['.js', '.jsx'], // Allow imports without extensions
    },
    devServer: {
        static: {
            directory: path.join(__dirname, 'public'), // Serve static files from public/
        },
        historyApiFallback: true, // For client-side routing
        port: 3000, // Or whichever port you prefer
        open: true, // Automatically open browser
        hot: true,  // Enable Hot Module Replacement
    },
};
