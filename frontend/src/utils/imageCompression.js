export function formatFileSize(bytes) {
  if (!Number.isFinite(bytes) || bytes < 0) return '0 bytes'
  if (bytes < 1024) return `${bytes} bytes`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

function readFileAsDataURL(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => reject(new Error('Failed to read file data'))
    reader.readAsDataURL(file)
  })
}

function loadImage(source) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error('Failed to load image'))
    img.src = source
  })
}

function canvasToBlob(canvas, mimeType, quality) {
  return new Promise((resolve) => {
    canvas.toBlob((blob) => resolve(blob), mimeType, quality)
  })
}

export async function compressImageToWebP(file, options = {}) {
  if (!(file instanceof File)) {
    throw new Error('Invalid file input. Expected a File object.')
  }

  const { maxWidth = 1200, quality = 0.8 } = options
  console.log('[ImageCompression] Reading file...')
  console.log(`[ImageCompression] Original: ${formatFileSize(file.size)}`)

  const dataUrl = await readFileAsDataURL(file)
  console.log('[ImageCompression] FileReader complete. Creating image element...')

  const img = await loadImage(dataUrl)
  console.log(`[ImageCompression] Image loaded (${img.width}x${img.height})`)

  let newWidth = img.width
  let newHeight = img.height
  if (img.width > maxWidth) {
    const scale = maxWidth / img.width
    newWidth = Math.round(img.width * scale)
    newHeight = Math.round(img.height * scale)
  }

  console.log(`[ImageCompression] Resized dimensions: ${newWidth}x${newHeight}`)
  const canvas = document.createElement('canvas')
  canvas.width = newWidth
  canvas.height = newHeight

  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('Unable to initialize canvas context for compression')
  }

  ctx.drawImage(img, 0, 0, newWidth, newHeight)
  console.log('[ImageCompression] Canvas draw complete. Converting to WebP...')

  let blob = await canvasToBlob(canvas, 'image/webp', quality)
  if (!blob) {
    console.log('[ImageCompression] WebP conversion failed. Falling back to JPEG...')
    blob = await canvasToBlob(canvas, 'image/jpeg', 0.7)
  }

  if (!blob) {
    throw new Error('Compression failed. Could not generate compressed image.')
  }

  const reduction = file.size > 0 ? Math.max(0, (1 - blob.size / file.size) * 100) : 0
  console.log(
    `[ImageCompression] Compressed: ${formatFileSize(blob.size)} (${reduction.toFixed(1)}% reduction)`
  )

  return {
    blob,
    originalSize: file.size,
    compressedSize: blob.size,
    width: newWidth,
    height: newHeight,
  }
}
