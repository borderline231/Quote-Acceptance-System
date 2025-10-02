'use client'

import React, { useState, useCallback } from 'react'
import { Document, Page, pdfjs } from 'react-pdf'
import 'react-pdf/dist/Page/AnnotationLayer.css'
import 'react-pdf/dist/Page/TextLayer.css'

// Set up PDF.js worker
pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.js`

export default function PDFViewer() {
  const [file, setFile] = useState<File | string | null>(null)
  const [numPages, setNumPages] = useState<number>(0)
  const [pageNumber, setPageNumber] = useState<number>(1)
  const [scale, setScale] = useState<number>(1.0)
  const [loading, setLoading] = useState<boolean>(false)
  const [error, setError] = useState<string | null>(null)

  // Handle file upload
  const handleFileUpload = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const uploadedFile = event.target.files?.[0]
    if (uploadedFile && uploadedFile.type === 'application/pdf') {
      setFile(uploadedFile)
      setPageNumber(1)
      setScale(1.0)
      setError(null)
    } else {
      setError('Please upload a valid PDF file')
    }
  }, [])

  // Document load success
  const onDocumentLoadSuccess = useCallback(({ numPages }: { numPages: number }) => {
    setNumPages(numPages)
    setLoading(false)
  }, [])

  // Document load error
  const onDocumentLoadError = useCallback((error: Error) => {
    setError(error.message)
    setLoading(false)
  }, [])

  // Page navigation
  const goToPreviousPage = () => setPageNumber(prev => Math.max(prev - 1, 1))
  const goToNextPage = () => setPageNumber(prev => Math.min(prev + 1, numPages))

  // Zoom controls
  const zoomIn = () => setScale(prev => Math.min(prev + 0.2, 3.0))
  const zoomOut = () => setScale(prev => Math.max(prev - 0.2, 0.5))
  const resetZoom = () => setScale(1.0)

  return (
    <div className="w-full max-w-6xl mx-auto p-4">
      <div className="bg-white rounded-lg shadow-lg overflow-hidden">
        <div className="bg-gray-50 border-b px-6 py-4">
          <h2 className="text-xl font-semibold text-gray-800">PDF Viewer</h2>
        </div>

        <div className="p-6">
          {/* Upload Section */}
          {!file && (
            <div className="border-2 border-dashed border-gray-300 rounded-lg p-12">
              <div className="text-center">
                <svg
                  className="mx-auto h-12 w-12 text-gray-400"
                  stroke="currentColor"
                  fill="none"
                  viewBox="0 0 48 48"
                >
                  <path
                    d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"
                    strokeWidth={2}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
                <label htmlFor="pdf-upload" className="cursor-pointer">
                  <span className="mt-2 block text-sm font-medium text-gray-900">
                    Click to upload PDF
                  </span>
                  <input
                    id="pdf-upload"
                    type="file"
                    accept=".pdf"
                    onChange={handleFileUpload}
                    className="hidden"
                  />
                </label>
                <p className="mt-1 text-sm text-gray-500">or drag and drop</p>
              </div>
            </div>
          )}

          {/* Error Message */}
          {error && (
            <div className="bg-red-50 text-red-800 px-4 py-3 rounded-lg mb-4">
              {error}
            </div>
          )}

          {/* PDF Controls */}
          {file && (
            <div>
              <div className="flex flex-wrap items-center justify-between gap-4 mb-4 p-4 bg-gray-50 rounded-lg">
                {/* Page Navigation */}
                <div className="flex items-center gap-2">
                  <button
                    onClick={goToPreviousPage}
                    disabled={pageNumber <= 1}
                    className="px-3 py-1 bg-white border rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    ← Prev
                  </button>
                  <span className="px-3 py-1">
                    Page {pageNumber} of {numPages}
                  </span>
                  <button
                    onClick={goToNextPage}
                    disabled={pageNumber >= numPages}
                    className="px-3 py-1 bg-white border rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Next →
                  </button>
                </div>

                {/* Zoom Controls */}
                <div className="flex items-center gap-2">
                  <button
                    onClick={zoomOut}
                    className="px-3 py-1 bg-white border rounded hover:bg-gray-100"
                  >
                    Zoom -
                  </button>
                  <span className="px-3 py-1 min-w-[80px] text-center">
                    {Math.round(scale * 100)}%
                  </span>
                  <button
                    onClick={zoomIn}
                    className="px-3 py-1 bg-white border rounded hover:bg-gray-100"
                  >
                    Zoom +
                  </button>
                  <button
                    onClick={resetZoom}
                    className="px-3 py-1 bg-white border rounded hover:bg-gray-100"
                  >
                    Reset
                  </button>
                </div>

                {/* New PDF Button */}
                <button
                  onClick={() => {
                    setFile(null)
                    setNumPages(0)
                    setPageNumber(1)
                    setError(null)
                  }}
                  className="px-4 py-1 bg-blue-500 text-white rounded hover:bg-blue-600"
                >
                  Load New PDF
                </button>
              </div>

              {/* PDF Display */}
              <div className="border rounded-lg overflow-auto bg-gray-100 p-4" style={{ maxHeight: '70vh' }}>
                <Document
                  file={file}
                  onLoadSuccess={onDocumentLoadSuccess}
                  onLoadError={onDocumentLoadError}
                  loading={
                    <div className="flex items-center justify-center p-12">
                      <div className="text-gray-500">Loading PDF...</div>
                    </div>
                  }
                  className="flex justify-center"
                >
                  <Page
                    pageNumber={pageNumber}
                    scale={scale}
                    renderTextLayer={true}
                    renderAnnotationLayer={true}
                    className="shadow-lg"
                  />
                </Document>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}