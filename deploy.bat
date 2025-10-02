@echo off
echo.
echo ============================================
echo    Next.js PDF Viewer - Quick Deploy
echo ============================================
echo.

REM Check if git is initialized
if not exist ".git" (
    echo Initializing git repository...
    git init
    git add .
    git commit -m "Initial commit - PDF viewer"
)

echo Choose your deployment platform:
echo.
echo 1. Vercel (Recommended)
echo 2. Netlify
echo 3. Railway
echo 4. Build Only (No Deploy)
echo.

set /p choice="Enter choice [1-4]: "

if %choice%==1 goto vercel
if %choice%==2 goto netlify
if %choice%==3 goto railway
if %choice%==4 goto buildonly
goto invalid

:vercel
echo.
echo Deploying to Vercel...
echo.

REM Check if vercel CLI is installed
where vercel >nul 2>&1
if %errorlevel% neq 0 (
    echo Installing Vercel CLI...
    npm i -g vercel
)

echo Running Vercel deployment...
vercel --prod

echo.
echo Deployment complete!
goto end

:netlify
echo.
echo Deploying to Netlify...
echo.

REM Check if netlify CLI is installed
where netlify >nul 2>&1
if %errorlevel% neq 0 (
    echo Installing Netlify CLI...
    npm i -g netlify-cli
)

REM Create netlify.toml if it doesn't exist
if not exist "netlify.toml" (
    echo Creating netlify.toml...
    (
        echo [build]
        echo   command = "npm run build"
        echo   publish = ".next"
        echo.
        echo [[plugins]]
        echo   package = "@netlify/plugin-nextjs"
    ) > netlify.toml
)

echo Building project...
npm run build

echo Deploying to Netlify...
netlify deploy --prod

echo.
echo Deployment complete!
goto end

:railway
echo.
echo Deploying to Railway...
echo.

REM Check if railway CLI is installed
where railway >nul 2>&1
if %errorlevel% neq 0 (
    echo Installing Railway CLI...
    npm i -g @railway/cli
)

echo Login to Railway...
railway login

echo Initializing Railway project...
railway init

echo Deploying...
railway up

echo.
echo Deployment complete!
goto end

:buildonly
echo.
echo Building project...
npm run build
echo.
echo Build complete! Use 'npm start' to run locally
goto end

:invalid
echo.
echo Invalid choice. Exiting.
goto end

:end
echo.
echo ============================================
echo Post-deployment steps:
echo 1. Set up custom domain (optional)
echo 2. Configure environment variables
echo 3. Test PDF upload functionality
echo 4. Monitor performance
echo ============================================
echo.
pause