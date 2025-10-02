import PDFViewer from '@/components/pdf-viewer'

export default function PDFViewerPage() {
  return (
    <main className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-950">
      <div className="container mx-auto py-8">
        <div className="mb-8 text-center">
          <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-2">
            PDF Document Viewer
          </h1>
          <p className="text-gray-600 dark:text-gray-400">
            Upload and view PDF documents with full controls
          </p>
        </div>

        <PDFViewer />
      </div>
    </main>
  )
}