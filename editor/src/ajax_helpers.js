const csrfToken = document.head.querySelector('[name=csrf-token]').content;

function isSuccessfulResponse(response) {
  return response.status >= 200 && response.status < 300;
}

function fetchJSON(url, options) {
  return new Promise((resolve, reject) => {
    fetch(url, options).then(response => {
      let callback = isSuccessfulResponse(response) ? resolve : reject;
      response.json().then(callback);
    });
  });
}

export function getJSON(url) {
  return fetchJSON(url);
}

export function postJSON(url, payload) {
  return fetchJSON(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Requested-With': 'XMLHttpRequest',
      'X-CSRF-Token': csrfToken
    },
    body: JSON.stringify(payload),
    credentials: 'same-origin'
  });
}
