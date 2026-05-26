let nextId = 100

export function generateId() {
  return String(nextId++)
}

export function delay(ms = 300) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

export function mockResult(data) {
  return { code: 200, msg: 'success', data }
}
