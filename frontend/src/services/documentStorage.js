import { documentsApi } from '../api/documents'
import { compressImageToWebP, formatFileSize } from '../utils/imageCompression'

export async function uploadDocument(file, documentType) {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    throw new Error(`Invalid file type: ${file.type}. Allowed: JPEG, PNG, WebP`)
  }

  const MAX_SIZE = 10 * 1024 * 1024
  if (file.size > MAX_SIZE) {
    throw new Error(`File too large: ${formatFileSize(file.size)}. Max: 10MB`)
  }

  console.log(`[DocumentUpload] Compressing ${documentType}...`)
  console.log(`[DocumentUpload] Original size: ${formatFileSize(file.size)}`)

  const compressionResult = await compressImageToWebP(file, {
    maxWidth: 1200,
    quality: 0.8,
  })

  console.log(`[DocumentUpload] Compressed size: ${formatFileSize(compressionResult.compressedSize)}`)
  console.log(
    `[DocumentUpload] Reduction: ${Math.round(
      (1 - compressionResult.compressedSize / compressionResult.originalSize) * 100
    )}%`
  )

  const formData = new FormData()
  formData.append('file', compressionResult.blob, `${documentType.toLowerCase()}_card.webp`)
  formData.append('documentType', documentType)

  console.log('[DocumentUpload] Sending to backend API...')
  const response = await documentsApi.upload(formData)
  console.log('[DocumentUpload] Backend saved successfully!')

  return {
    id: response.id,
    url: response.documentUrl,
    path: response.storagePath,
    originalSize: compressionResult.originalSize,
    compressedSize: compressionResult.compressedSize,
    documentType,
    verificationStatus: response.verificationStatus,
    uploadedAt: response.uploadedAt,
    compressedBlob: compressionResult.blob,
  }
}

export async function deleteDocument(documentId) {
  await documentsApi.delete(documentId)
}
