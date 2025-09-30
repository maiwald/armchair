# GitHub Actions Workflows

## Deploy to GitHub Pages

The `deploy.yml` workflow automatically builds and deploys the Armchair application to GitHub Pages.

### How it works

1. **Trigger**: The workflow runs automatically on every push to the `main` branch, or can be triggered manually via the Actions tab.

2. **Build Process**: 
   - Sets up Node.js (using the version specified in `.node-version`)
   - Sets up Java 17 (required for shadow-cljs/ClojureScript compilation)
   - Installs npm dependencies
   - Creates a `.env` file with the necessary environment variables
   - Runs the build process (similar to `scripts/build.sh`) which:
     - Compiles ClojureScript using shadow-cljs
     - Compiles Sass stylesheets
     - Compiles Tailwind CSS
     - Versions the output files using the git commit hash
     - Updates `index.html` to reference the versioned files

3. **Deploy**: Uploads the `build` folder contents to GitHub Pages

### Setup Requirements

To enable GitHub Pages deployment for this repository:

1. Go to your repository Settings → Pages
2. Under "Build and deployment", select "GitHub Actions" as the source
3. The workflow will automatically deploy on the next push to `main`

### Manual Deployment

You can manually trigger a deployment:

1. Go to the Actions tab in your GitHub repository
2. Select "Deploy to GitHub Pages" workflow
3. Click "Run workflow"
4. Select the branch and click "Run workflow"

### Build Differences from scripts/build.sh

The workflow inline builds the project instead of calling `scripts/build.sh` directly because:
- `sed -i ""` (macOS syntax) doesn't work on Linux - the workflow uses `sed -i` (Linux syntax)
- Better visibility of each build step in the Actions logs
- More control over error handling and logging

The build logic is functionally identical to `scripts/build.sh`.
