const csrfToken = document.head.querySelector("[name=csrf-token]").content;

function checkStatus(response) {
  if (response.status >= 200 && response.status < 300) {
    return response
  } else {
    var error = new Error(response.statusText)
    error.response = response
    throw error
  }
}

export function getJSON(url) {
  return fetch(url)
    .then(checkStatus)
    .then(res => res.json())
}

export function postJSON(url, payload) {
  return fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Requested-With': 'XMLHttpRequest',
      'X-CSRF-Token': csrfToken
    },
    body: JSON.stringify(payload),
    credentials: 'same-origin'
  })
    .then(checkStatus)
    .then(res => res.json())
}
