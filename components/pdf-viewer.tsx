'use client'

import React, { useState, useCallback } from 'react'
import { Document, Page, pdfjs } from 'react-pdf'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Slider } from '@/components/ui/slider'
import {
  ChevronLeft,
  ChevronRight,
  ZoomIn,
  ZoomOut,
  FileText,
  Upload,
  Maximize,
  Download,
  RotateCw
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { useToast } from '@/components/ui/use-toast'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Skeleton } from "@/components/ui/skeleton"
import 'react-pdf/dist/Page/AnnotationLayer.css'
import 'react-pdf/dist/Page/TextLayer.css'

// Set up PDF.js worker
pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.js`

interface PDFViewerProps {
  className?: string
}

export default function PDFViewer({ className }: PDFViewerProps) {
  const [file, setFile] = useState<File | string | null>(null)
  const [numPages, setNumPages] = useState<number>(0)
  const [pageNumber, setPageNumber] = useState<number>(1)
  const [scale, setScale] = useState<number>(1.0)
  const [rotation, setRotation] = useState<number>(0)
  const [pageWidth, setPageWidth] = useState<number>(0)
  const { toast } = useToast()

  // Handle file upload
  const handleFileUpload = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const uploadedFile = event.target.files?.[0]

    if (uploadedFile && uploadedFile.type === 'application/pdf') {
      setFile(uploadedFile)
      setPageNumber(1)
      setScale(1.0)
      setRotation(0)
    } else {
      toast({
        title: "Invalid file",
        description: "Please upload a valid PDF file",
        variant: "destructive"
      })
    }
  }, [toast])

  // Handle URL input
  const handleUrlLoad = useCallback((url: string) => {
    if (url) {
      setFile(url)
      setPageNumber(1)
      setScale(1.0)
      setRotation(0)
    }
  }, [])

  // Document load success
  const onDocumentLoadSuccess = useCallback(({ numPages }: { numPages: number }) => {
    setNumPages(numPages)
    toast({
      title: "PDF loaded successfully",
      description: `Document has ${numPages} page${numPages > 1 ? 's' : ''}`,
    })
  }, [toast])

  // Document load error
  const onDocumentLoadError = useCallback((error: Error) => {
    toast({
      title: "Error loading PDF",
      description: error.message,
      variant: "destructive"
    })
  }, [toast])

  // Page navigation
  const goToPreviousPage = useCallback(() => {
    setPageNumber(prev => Math.max(prev - 1, 1))
  }, [])

  const goToNextPage = useCallback(() => {
    setPageNumber(prev => Math.min(prev + 1, numPages))
  }, [numPages])

  // Zoom controls
  const zoomIn = useCallback(() => {
    setScale(prev => Math.min(prev + 0.2, 3.0))
  }, [])

  const zoomOut = useCallback(() => {
    setScale(prev => Math.max(prev - 0.2, 0.5))
  }, [])

  const resetZoom = useCallback(() => {
    setScale(1.0)
  }, [])

  // Rotation
  const rotate = useCallback(() => {
    setRotation(prev => (prev + 90) % 360)
  }, [])

  // Download PDF
  const downloadPDF = useCallback(() => {
    if (file instanceof File) {
      const url = URL.createObjectURL(file)
      const link = document.createElement('a')
      link.href = url
      link.download = file.name
      link.click()
      URL.revokeObjectURL(url)
    } else if (typeof file === 'string') {
      window.open(file, '_blank')
    }
  }, [file])

  // Calculate page dimensions based on container
  const onPageLoadSuccess = useCallback(() => {
    const container = document.getElementById('pdf-container')
    if (container) {
      const containerWidth = container.clientWidth - 48 // padding
      setPageWidth(containerWidth)
    }
  }, [])

  return (
    <div className={cn("w-full max-w-7xl mx-auto p-4", className)}>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            PDF Viewer
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Upload Section */}
          {!file && (
            <div className="border-2 border-dashed border-gray-300 dark:border-gray-700 rounded-lg p-12">
              <div className="text-center space-y-4">
                <Upload className="h-12 w-12 mx-auto text-gray-400" />
                <div>
                  <Label htmlFor="pdf-upload" className="cursor-pointer">
                    <span className="text-lg font-semibold">Upload PDF File</span>
                    <Input
                      id="pdf-upload"
                      type="file"
                      accept=".pdf"
                      onChange={handleFileUpload}
                      className="hidden"
                    />
                  </Label>
                  <p className="text-sm text-gray-500 mt-2">or drag and drop</p>
                </div>

                <div className="max-w-md mx-auto">
                  <div className="flex gap-2">
                    <Input
                      type="url"
                      placeholder="Or enter PDF URL..."
                      onKeyPress={(e) => {
                        if (e.key === 'Enter') {
                          handleUrlLoad((e.target as HTMLInputElement).value)
                        }
                      }}
                    />
                    <Button
                      variant="outline"
                      onClick={(e) => {
                        const input = (e.currentTarget.previousElementSibling as HTMLInputElement)
                        handleUrlLoad(input.value)
                      }}
                    >
                      Load
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* PDF Controls */}
          {file && (
            <div className="space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-4 p-4 bg-gray-50 dark:bg-gray-900 rounded-lg">
                {/* Page Navigation */}
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={goToPreviousPage}
                    disabled={pageNumber <= 1}
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>

                  <div className="flex items-center gap-2">
                    <Input
                      type="number"
                      min={1}
                      max={numPages}
                      value={pageNumber}
                      onChange={(e) => {
                        const page = parseInt(e.target.value)
                        if (page >= 1 && page <= numPages) {
                          setPageNumber(page)
                        }
                      }}
                      className="w-16 text-center"
                    />
                    <span className="text-sm text-gray-500">of {numPages}</span>
                  </div>

                  <Button
                    variant="outline"
                    size="icon"
                    onClick={goToNextPage}
                    disabled={pageNumber >= numPages}
                  >
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>

                {/* Zoom Controls */}
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={zoomOut}
                    disabled={scale <= 0.5}
                  >
                    <ZoomOut className="h-4 w-4" />
                  </Button>

                  <div className="flex items-center gap-2 min-w-[200px]">
                    <Slider
                      value={[scale * 100]}
                      onValueChange={([value]) => setScale(value / 100)}
                      min={50}
                      max={300}
                      step={10}
                      className="flex-1"
                    />
                    <span className="text-sm font-medium w-12">{Math.round(scale * 100)}%</span>
                  </div>

                  <Button
                    variant="outline"
                    size="icon"
                    onClick={zoomIn}
                    disabled={scale >= 3.0}
                  >
                    <ZoomIn className="h-4 w-4" />
                  </Button>
                </div>

                {/* Additional Controls */}
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={resetZoom}
                    title="Fit to width"
                  >
                    <Maximize className="h-4 w-4" />
                  </Button>

                  <Button
                    variant="outline"
                    size="icon"
                    onClick={rotate}
                    title="Rotate"
                  >
                    <RotateCw className="h-4 w-4" />
                  </Button>

                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="outline">
                        Options
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent>
                      <DropdownMenuItem onClick={downloadPDF}>
                        <Download className="h-4 w-4 mr-2" />
                        Download
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => {
                        setFile(null)
                        setNumPages(0)
                        setPageNumber(1)
                      }}>
                        <Upload className="h-4 w-4 mr-2" />
                        Load New PDF
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </div>

              {/* PDF Display */}
              <div
                id="pdf-container"
                className="border rounded-lg overflow-auto bg-gray-100 dark:bg-gray-950 p-6"
                style={{ maxHeight: '80vh' }}
              >
                <Document
                  file={file}
                  onLoadSuccess={onDocumentLoadSuccess}
                  onLoadError={onDocumentLoadError}
                  loading={
                    <div className="flex flex-col items-center justify-center p-12 space-y-4">
                      <Skeleton className="h-[600px] w-full max-w-2xl" />
                      <p className="text-sm text-gray-500">Loading PDF...</p>
                    </div>
                  }
                  className="flex justify-center"
                >
                  <Page
                    pageNumber={pageNumber}
                    scale={scale}
                    rotate={rotation}
                    width={pageWidth || undefined}
                    onLoadSuccess={onPageLoadSuccess}
                    loading={
                      <Skeleton className="h-[600px] w-full max-w-2xl" />
                    }
                    className="shadow-lg"
                    renderTextLayer={true}
                    renderAnnotationLayer={true}
                  />
                </Document>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}