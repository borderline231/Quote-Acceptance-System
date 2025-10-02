# Hosting Your Next.js PDF Viewer

## Option 1: Vercel (Recommended - Easiest)

### Step 1: Prepare Your Project

```bash
# Initialize git repository
git init
git add .
git commit -m "Initial commit - PDF viewer"
```

### Step 2: Deploy to Vercel

**Method A: Using Vercel CLI**
```bash
# Install Vercel CLI
npm i -g vercel

# Deploy
vercel

# Follow the prompts:
# - Link to existing project? No
# - What's your project name? (your-app-name)
# - In which directory is your code? ./
# - Want to override settings? No
```

**Method B: Using GitHub**
1. Push code to GitHub:
```bash
git remote add origin https://github.com/yourusername/your-repo.git
git push -u origin main
```

2. Go to [vercel.com](https://vercel.com)
3. Click "New Project"
4. Import your GitHub repository
5. Click "Deploy"

### Environment Variables (if needed)
Add in Vercel dashboard:
- Settings → Environment Variables
- Add any API keys or config

---

## Option 2: Netlify

### Step 1: Build Configuration
Create `netlify.toml`:
```toml
[build]
  command = "npm run build"
  publish = ".next"

[[plugins]]
  package = "@netlify/plugin-nextjs"

[build.environment]
  NEXT_USE_NETLIFY_EDGE = "true"
```

### Step 2: Deploy
```bash
# Install Netlify CLI
npm install -g netlify-cli

# Build project
npm run build

# Deploy
netlify deploy --prod

# Or connect GitHub and auto-deploy
```

---

## Option 3: Railway

### Step 1: Install Railway
```bash
npm install -g @railway/cli
```

### Step 2: Deploy
```bash
# Login to Railway
railway login

# Initialize project
railway init

# Deploy
railway up
```

---

## Option 4: Render

### Step 1: Create render.yaml
```yaml
services:
  - type: web
    name: pdf-viewer
    runtime: node
    buildCommand: npm install && npm run build
    startCommand: npm start
    envVars:
      - key: NODE_ENV
        value: production
```

### Step 2: Deploy
1. Push to GitHub
2. Go to [render.com](https://render.com)
3. New → Web Service
4. Connect GitHub repo
5. Deploy

---

## Option 5: Self-Hosting (VPS/Cloud)

### For Ubuntu/Debian VPS:

```bash
# 1. Install Node.js
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# 2. Install PM2
sudo npm install -g pm2

# 3. Clone your project
git clone your-repo-url
cd your-project

# 4. Install dependencies
npm install

# 5. Build project
npm run build

# 6. Start with PM2
pm2 start npm --name "pdf-viewer" -- start
pm2 save
pm2 startup

# 7. Setup Nginx (optional)
sudo apt install nginx
```

Create nginx config `/etc/nginx/sites-available/pdf-viewer`:
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```

---

## Pre-Deployment Checklist

### 1. Update package.json
```json
{
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint"
  }
}
```

### 2. Create .env.local (if needed)
```env
NEXT_PUBLIC_API_URL=your-api-url
```

### 3. Update next.config.js
```js
/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  images: {
    unoptimized: true
  },
  webpack: (config) => {
    config.resolve.alias.canvas = false
    config.resolve.alias.encoding = false
    return config
  },
}

module.exports = nextConfig
```

### 4. Add .gitignore
```
# dependencies
/node_modules
/.pnp
.pnp.js

# testing
/coverage

# next.js
/.next/
/out/

# production
/build

# misc
.DS_Store
*.pem

# debug
npm-debug.log*
yarn-debug.log*
yarn-error.log*

# local env files
.env*.local

# vercel
.vercel

# typescript
*.tsbuildinfo
next-env.d.ts
```

---

## Quick Deploy Commands

### Vercel (Fastest)
```bash
npx vercel --prod
```

### Netlify
```bash
npx netlify-cli deploy --prod
```

### Railway
```bash
railway up
```

---

## Cost Comparison

| Platform | Free Tier | Paid Starting |
|----------|-----------|---------------|
| **Vercel** | Generous (100GB bandwidth/month) | $20/month |
| **Netlify** | 100GB bandwidth/month | $19/month |
| **Railway** | $5 credit/month | $5/month |
| **Render** | 750 hours/month | $7/month |
| **AWS Amplify** | 1000 build minutes | Pay as you go |

---

## Domain Setup

### For Custom Domain:
1. Buy domain from registrar (Namecheap, GoDaddy, etc.)
2. In hosting platform:
   - Add custom domain
   - Get DNS records
3. In domain registrar:
   - Update nameservers or
   - Add CNAME/A records
4. Wait for propagation (5-48 hours)

### SSL Certificate
- Vercel/Netlify: Automatic SSL
- Self-hosting: Use Let's Encrypt
```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

---

## Post-Deployment

### Monitor Your App
- Vercel: Built-in analytics
- Add error tracking: Sentry
- Add analytics: Google Analytics, Plausible

### Performance
Test with:
- Google PageSpeed Insights
- GTmetrix
- WebPageTest

---

## Troubleshooting

### Build Errors
```bash
# Clear cache and rebuild
rm -rf .next node_modules
npm install
npm run build
```

### PDF Worker Issues
Ensure `next.config.js` has:
```js
webpack: (config) => {
  config.resolve.alias.canvas = false
  config.resolve.alias.encoding = false
  return config
}
```

### Large PDF Performance
Consider CDN for PDF files:
- Cloudflare (free tier)
- AWS CloudFront
- Vercel Edge Network (automatic)

---

## Support Resources

- [Next.js Deployment Docs](https://nextjs.org/docs/deployment)
- [Vercel Docs](https://vercel.com/docs)
- [Netlify Docs](https://docs.netlify.com)
- [Railway Docs](https://docs.railway.app)