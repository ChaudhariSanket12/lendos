import { useEffect, useMemo, useRef, useState } from 'react'
import { deleteDocument, uploadDocument } from '../services/documentStorage'
import { formatFileSize } from '../utils/imageCompression'
import '../styles/DocumentUploader.css'

const MAX_SIZE_BYTES = 10 * 1024 * 1024
const ALLOWED_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])

function getDocLabel(documentType) {
  return documentType === 'PAN' ? 'PAN Card' : 'Aadhaar Card'
}

function toDocumentViewModel(data, fallbackType) {
  return {
    id: data?.id,
    documentType: data?.documentType || fallbackType,
    documentUrl: data?.documentUrl || data?.url,
    storagePath: data?.storagePath || data?.path,
    originalSize: data?.originalSize,
    compressedSize: data?.compressedSize,
    verificationStatus: data?.verificationStatus || 'PENDING',
    filename: data?.filename,
    previewUrl: data?.previewUrl,
    uploadedAt: data?.uploadedAt,
  }
}

export default function DocumentUploader({
  documentType,
  onUploadComplete,
  onUploadError,
  existingDocument,
}) {
  const fileInputRef = useRef(null)
  const [isDragActive, setIsDragActive] = useState(false)
  const [status, setStatus] = useState(existingDocument ? 'success' : 'empty')
  const [progress, setProgress] = useState(0)
  const [errorMessage, setErrorMessage] = useState('')
  const [selectedFileSize, setSelectedFileSize] = useState(0)
  const [documentData, setDocumentData] = useState(
    existingDocument ? toDocumentViewModel(existingDocument, documentType) : null
  )
  const [isBusy, setIsBusy] = useState(false)

  const displayLabel = useMemo(() => getDocLabel(documentType), [documentType])
  const isVerified = documentData?.verificationStatus === 'VERIFIED'

  useEffect(() => {
    if (!existingDocument) return
    setDocumentData((prev) => ({
      ...prev,
      ...toDocumentViewModel(existingDocument, documentType),
      previewUrl: prev?.previewUrl || existingDocument.previewUrl,
    }))
    setStatus('success')
    setErrorMessage('')
  }, [documentType, existingDocument])

  useEffect(() => {
    return () => {
      if (documentData?.previewUrl?.startsWith('blob:')) {
        URL.revokeObjectURL(documentData.previewUrl)
      }
    }
  }, [documentData?.previewUrl])

  const validateFile = (file) => {
    if (!(file instanceof File)) return 'Please choose a valid image file.'
    if (!ALLOWED_TYPES.has(file.type)) return 'Only JPEG, PNG, or WebP files are allowed.'
    if (file.size > MAX_SIZE_BYTES) return 'File is too large (max 10MB).'
    return null
  }

  const openFilePicker = () => {
    if (!isBusy) {
      fileInputRef.current?.click()
    }
  }

  const clearState = () => {
    if (documentData?.previewUrl?.startsWith('blob:')) {
      URL.revokeObjectURL(documentData.previewUrl)
    }
    setDocumentData(null)
    setStatus('empty')
    setProgress(0)
    setSelectedFileSize(0)
    setErrorMessage('')
  }

  const handleFileProcessing = async (file) => {
    if (isVerified) {
      const verifiedError = new Error('Verified documents cannot be replaced.')
      setStatus('error')
      setErrorMessage(verifiedError.message)
      onUploadError?.(verifiedError)
      return
    }

    const validationError = validateFile(file)
    if (validationError) {
      setStatus('error')
      setErrorMessage(validationError)
      onUploadError?.(new Error(validationError))
      return
    }

    setIsBusy(true)
    setStatus('compressing')
    setProgress(30)
    setSelectedFileSize(file.size)
    setErrorMessage('')

    try {
      console.log('[DocumentUploader] Upload flow started', { documentType })
      const storageResult = await uploadDocument(file, documentType)
      const previewUrl = URL.createObjectURL(storageResult.compressedBlob)
      setProgress(75)
      setStatus('uploading')

      const merged = toDocumentViewModel(
        {
          ...storageResult,
          previewUrl,
        },
        documentType
      )
      setDocumentData(merged)
      setProgress(100)
      setStatus('success')
      console.log('[DocumentUploader] Upload flow completed', merged)
      onUploadComplete?.(merged)
    } catch (error) {
      console.error('[DocumentUploader] Upload flow failed', error)
      setStatus('error')
      setErrorMessage(error.message || 'Document upload failed')
      onUploadError?.(error)
    } finally {
      setIsBusy(false)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    }
  }

  const handleInputChange = async (event) => {
    const file = event.target.files?.[0]
    if (file) {
      await handleFileProcessing(file)
    }
  }

  const handleDrop = async (event) => {
    event.preventDefault()
    setIsDragActive(false)
    if (isBusy) return
    const file = event.dataTransfer.files?.[0]
    if (file) {
      await handleFileProcessing(file)
    }
  }

  const handleRemove = async () => {
    if (!documentData || isBusy) return
    setIsBusy(true)
    setStatus('uploading')
    setProgress(30)

    try {
      if (!documentData.id) {
        throw new Error('Document ID not found. Please refresh and try again.')
      }
      await deleteDocument(documentData.id)
      console.log('[DocumentUploader] Document removed', {
        documentType,
        documentId: documentData.id,
      })
      clearState()
      onUploadComplete?.({ documentType, removed: true })
    } catch (error) {
      console.error('[DocumentUploader] Remove failed', error)
      setStatus('error')
      setErrorMessage(error.message || 'Failed to remove document')
      onUploadError?.(error)
    } finally {
      setIsBusy(false)
    }
  }

  return (
    <div className={`document-uploader-card ${status}`}>
      <div className="document-uploader-header">
        <h4>
          📷 {displayLabel}{' '}
          {status === 'success' ? '✅' : status === 'error' ? '❌' : ''}
        </h4>
      </div>

      {status === 'empty' && (
        <div
          className={`document-dropzone ${isDragActive ? 'drag-active' : ''}`}
          role="button"
          tabIndex={0}
          aria-label={`Upload ${displayLabel}`}
          onClick={openFilePicker}
          onKeyDown={(event) => {
            if (event.key === 'Enter' || event.key === ' ') {
              event.preventDefault()
              openFilePicker()
            }
          }}
          onDragOver={(event) => {
            event.preventDefault()
            setIsDragActive(true)
          }}
          onDragLeave={(event) => {
            event.preventDefault()
            setIsDragActive(false)
          }}
          onDrop={handleDrop}
        >
          <div className="dropzone-inner">
            <div className="upload-icon">📁</div>
            <p>Drag & drop your {displayLabel} here</p>
            <p>or click to browse</p>
            <small>JPEG, PNG, WebP • Max 10MB</small>
          </div>
        </div>
      )}

      {(status === 'compressing' || status === 'uploading') && (
        <div className="document-upload-progress">
          <div className="loading-spinner" />
          <p>{status === 'compressing' ? 'Compressing image...' : 'Uploading document...'}</p>
          <div className="progress-track">
            <div className="progress-bar" style={{ width: `${progress}%` }} />
          </div>
          <small>Original: {formatFileSize(selectedFileSize)}</small>
          <small>{progress}%</small>
        </div>
      )}

      {status === 'success' && documentData && (
        <div className="document-upload-success">
          <div className="document-preview-area">
            <img
              src={documentData.previewUrl || documentData.documentUrl}
              alt={`${displayLabel} preview`}
              className="document-thumbnail"
            />
            <div className="document-meta">
              <p className="document-file-name">
                {documentData.filename || `${documentType}.webp`}
              </p>
              <p>Original: {formatFileSize(documentData.originalSize)}</p>
              <p>Compressed: {formatFileSize(documentData.compressedSize)}</p>
              {documentData.originalSize > 0 && documentData.compressedSize >= 0 && (
                <p>
                  Reduction:{' '}
                  {Math.max(
                    0,
                    Math.round((1 - documentData.compressedSize / documentData.originalSize) * 100)
                  )}
                  %
                </p>
              )}
              <p>Status: {documentData.verificationStatus || 'PENDING'}</p>
            </div>
          </div>
          <div className="document-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => window.open(documentData.documentUrl, '_blank', 'noopener,noreferrer')}
            >
              View
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleRemove}
              disabled={isBusy || isVerified}
            >
              Remove
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={openFilePicker}
              disabled={isBusy || isVerified}
            >
              Retake
            </button>
          </div>
          {isVerified && (
            <small className="verified-note">
              Verified document is locked. Contact support to request re-upload.
            </small>
          )}
        </div>
      )}

      {status === 'error' && (
        <div className="document-upload-error">
          <p>⚠️ Upload failed</p>
          <small>{errorMessage}</small>
          <button type="button" className="btn btn-secondary" onClick={() => setStatus('empty')}>
            Try Again
          </button>
        </div>
      )}

      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        onChange={handleInputChange}
        className="hidden-file-input"
        aria-label={`Select ${displayLabel}`}
      />
    </div>
  )
}
