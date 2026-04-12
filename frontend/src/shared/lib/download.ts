import { DownloadFile } from '../../types/api'

export function downloadTextFile(file: DownloadFile) {
  const blob = new Blob([file.content], { type: file.mimeType })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = file.fileName
  anchor.click()
  URL.revokeObjectURL(url)
}
