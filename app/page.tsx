import PDFViewer from '@/components/pdf-viewer-simple'

export default function Home() {
  return (
    <main className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
      <div className="container mx-auto py-8">
        <div className="mb-8 text-center">
          <h1 className="text-4xl font-bold text-gray-900 mb-2">
            PDF Document Viewer
          </h1>
          <p className="text-gray-600">
            Upload and view PDF documents with full controls
          </p>
        </div>
        <PDFViewer />
      </div>
    </main>
  )
}