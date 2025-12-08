export async function noAuthFetch(path, options = {}) {
    //TODO conf problem
    const url = window._env_.API_URL + path
    //const url = 'http://localhost:8080/' + path

    console.log('API URL ' + url)

    const headers = {
        ...(options.headers || {}),
        "Content-Type": "application/json",
    };

    const response = await fetch(url, { ...options, headers });

    return response;
}

export async function authFetch(path, options = {}) {
    //TODO conf problem
    const url = window._env_.API_URL + path
    //const url = 'http://localhost:8080/' + path

    const token = localStorage.getItem("token");
    console.log('API URL (no auth) ' + url)

    const headers = {
        ...(options.headers || {}),
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {})
    };

    const response = await fetch(url, { ...options, headers });

    if (response.status === 401) {
        localStorage.removeItem("token");
        window.location.href = "/login";
    }

    return response;
}
