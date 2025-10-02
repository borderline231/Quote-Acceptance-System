# Next.js PDF Viewer Setup Instructions

## Prerequisites
Make sure you have a Next.js project with shadcn/ui already set up. If not, follow these steps:

```bash
# Create a new Next.js project
npx create-next-app@latest pdf-viewer-app --typescript --tailwind --app

# Navigate to project
cd pdf-viewer-app

# Initialize shadcn/ui
npx shadcn-ui@latest init
```

## Step 1: Install Required Dependencies

```bash
# Install react-pdf and its dependencies
npm install react-pdf

# Install additional required packages
npm install lucide-react
```

## Step 2: Install Required shadcn/ui Components

Run these commands to add the necessary shadcn/ui components:

```bash
npx shadcn-ui@latest add button
npx shadcn-ui@latest add card
npx shadcn-ui@latest add input
npx shadcn-ui@latest add label
npx shadcn-ui@latest add slider
npx shadcn-ui@latest add toast
npx shadcn-ui@latest add dropdown-menu
npx shadcn-ui@latest add skeleton
```

## Step 3: Add the PDF Viewer Component

1. Copy the `pdf-viewer-component.tsx` file to your components directory:
   - `app/components/pdf-viewer.tsx` or
   - `components/pdf-viewer.tsx`

## Step 4: Create a Page to Use the Component

Create a new page file `app/pdf-viewer/page.tsx`:

```tsx
import PDFViewer from '@/components/pdf-viewer'

export default function PDFViewerPage() {
  return (
    <div className="container mx-auto py-8">
      <PDFViewer />
    </div>
  )
}
```

## Step 5: Configure Next.js for PDF.js Worker

Add this to your `next.config.js`:

```js
/** @type {import('next').NextConfig} */
const nextConfig = {
  webpack: (config) => {
    config.resolve.alias.canvas = false
    config.resolve.alias.encoding = false
    return config
  },
}

module.exports = nextConfig
```

## Step 6: Add PDF.js CSS (Optional but Recommended)

Create or update `app/globals.css` to include:

```css
/* PDF.js text layer */
.react-pdf__Page__textContent {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  opacity: 0.2;
  line-height: 1;
  text-align: initial;
  -webkit-text-size-adjust: none;
  -moz-text-size-adjust: none;
  -ms-text-size-adjust: none;
  text-size-adjust: none;
  forced-color-adjust: none;
}

.react-pdf__Page__textContent span {
  position: absolute;
  white-space: pre;
  cursor: text;
  transform-origin: 0% 0%;
}

/* PDF.js annotations layer */
.react-pdf__Page__annotations {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
}

.react-pdf__Page__canvas {
  display: block;
  user-select: none;
}
```

## Usage

The PDF viewer component supports:

### Features
- **File Upload**: Click to upload or drag-and-drop PDF files
- **URL Loading**: Load PDFs from external URLs
- **Page Navigation**: Previous/Next buttons and direct page input
- **Zoom Controls**: Zoom in/out with slider and buttons
- **Rotation**: Rotate pages in 90-degree increments
- **Download**: Download the loaded PDF
- **Responsive**: Works on mobile and desktop
- **Dark Mode**: Fully supports dark mode via Tailwind CSS
- **Text Selection**: Select and copy text from PDFs
- **Annotations**: Display PDF annotations

### Keyboard Shortcuts (when PDF is loaded)
- `Arrow Left`: Previous page
- `Arrow Right`: Next page
- `+` or `=`: Zoom in
- `-`: Zoom out
- `0`: Reset zoom

### Mobile Gestures
- Swipe left/right for page navigation
- Pinch to zoom
- Touch-friendly controls

## Customization

You can customize the component by:

1. **Passing className prop**:
```tsx
<PDFViewer className="custom-styles" />
```

2. **Modifying the theme**: Update your `tailwind.config.js` and shadcn/ui theme

3. **Adding features**: The component is fully extensible with hooks and callbacks

## Troubleshooting

### Common Issues

1. **Worker not loading**: Make sure you're using the correct worker URL in the component
2. **Large PDFs slow**: Consider implementing lazy loading or virtualization for very large documents
3. **CORS errors with URLs**: Ensure the PDF URL allows cross-origin requests

### Browser Compatibility
- Chrome/Edge: Full support
- Firefox: Full support
- Safari: Full support (iOS 14.5+)
- Mobile browsers: Full support with touch gestures

## Example Implementation

```tsx
'use client'

import { useState } from 'react'
import PDFViewer from '@/components/pdf-viewer'

export default function DocumentsPage() {
  const [selectedPDF, setSelectedPDF] = useState<string | null>(null)

  const samplePDFs = [
    { name: 'Sample 1', url: 'https://example.com/sample1.pdf' },
    { name: 'Sample 2', url: 'https://example.com/sample2.pdf' },
  ]

  return (
    <div className="container mx-auto py-8">
      <h1 className="text-3xl font-bold mb-6">Document Viewer</h1>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-1">
          <h2 className="text-lg font-semibold mb-3">Quick Access</h2>
          <div className="space-y-2">
            {samplePDFs.map((pdf) => (
              <button
                key={pdf.name}
                onClick={() => setSelectedPDF(pdf.url)}
                className="w-full text-left p-2 rounded hover:bg-gray-100"
              >
                {pdf.name}
              </button>
            ))}
          </div>
        </div>

        <div className="lg:col-span-3">
          <PDFViewer />
        </div>
      </div>
    </div>
  )
}
```

## Performance Optimization

For better performance with large PDFs:

1. **Implement pagination**: Load only visible pages
2. **Use web workers**: Process PDFs in background
3. **Cache rendered pages**: Store rendered pages in memory
4. **Lazy load**: Load pages as user scrolls

## License

This component uses:
- react-pdf (MIT License)
- PDF.js (Apache 2.0 License)
- shadcn/ui components (MIT License)