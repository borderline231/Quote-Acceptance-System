#!/bin/bash

# Quick Deploy Script for Next.js PDF Viewer

echo "🚀 Next.js PDF Viewer - Quick Deploy"
echo "===================================="
echo ""

# Check if git is initialized
if [ ! -d .git ]; then
    echo "📦 Initializing git repository..."
    git init
    git add .
    git commit -m "Initial commit - PDF viewer"
fi

# Choose deployment platform
echo "Choose your deployment platform:"
echo "1) Vercel (Recommended)"
echo "2) Netlify"
echo "3) Railway"
echo "4) GitHub Pages (Static Export)"
echo "5) Build Only (No Deploy)"
echo ""
read -p "Enter choice [1-5]: " choice

case $choice in
    1)
        echo "🔷 Deploying to Vercel..."

        # Check if vercel CLI is installed
        if ! command -v vercel &> /dev/null; then
            echo "Installing Vercel CLI..."
            npm i -g vercel
        fi

        echo "Running Vercel deployment..."
        vercel --prod

        echo "✅ Deployment complete!"
        ;;

    2)
        echo "🔷 Deploying to Netlify..."

        # Check if netlify CLI is installed
        if ! command -v netlify &> /dev/null; then
            echo "Installing Netlify CLI..."
            npm i -g netlify-cli
        fi

        # Create netlify.toml if it doesn't exist
        if [ ! -f netlify.toml ]; then
            echo "Creating netlify.toml..."
            cat > netlify.toml << 'EOF'
[build]
  command = "npm run build"
  publish = ".next"

[[plugins]]
  package = "@netlify/plugin-nextjs"
EOF
        fi

        echo "Building project..."
        npm run build

        echo "Deploying to Netlify..."
        netlify deploy --prod

        echo "✅ Deployment complete!"
        ;;

    3)
        echo "🔷 Deploying to Railway..."

        # Check if railway CLI is installed
        if ! command -v railway &> /dev/null; then
            echo "Installing Railway CLI..."
            npm i -g @railway/cli
        fi

        echo "Login to Railway..."
        railway login

        echo "Initializing Railway project..."
        railway init

        echo "Deploying..."
        railway up

        echo "✅ Deployment complete!"
        ;;

    4)
        echo "🔷 Building for GitHub Pages..."

        # Update next.config.js for static export
        echo "Updating configuration for static export..."

        cat > next.config.static.js << 'EOF'
/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',
  images: {
    unoptimized: true
  },
  webpack: (config) => {
    config.resolve.alias.canvas = false
    config.resolve.alias.encoding = false
    return config
  },
  basePath: process.env.NODE_ENV === 'production' ? '/your-repo-name' : '',
}

module.exports = nextConfig
EOF

        echo "Building static site..."
        npm run build

        echo "Static site built in 'out' directory"
        echo "Push to GitHub and enable Pages in Settings → Pages"
        echo "✅ Build complete!"
        ;;

    5)
        echo "🔷 Building project..."
        npm run build
        echo "✅ Build complete! Use 'npm start' to run locally"
        ;;

    *)
        echo "Invalid choice. Exiting."
        exit 1
        ;;
esac

echo ""
echo "📝 Post-deployment steps:"
echo "1. Set up custom domain (optional)"
echo "2. Configure environment variables"
echo "3. Test PDF upload functionality"
echo "4. Monitor performance"