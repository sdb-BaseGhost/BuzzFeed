import api from './index'

export async function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export async function uploadVideo(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/upload/video', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
