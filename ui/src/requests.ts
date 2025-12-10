export async function noAuthFetch(path, options = {}) {
    const url = window._env_.API_URL + path
    const headers = {
        ...(options.headers || {}),
        "Content-Type": "application/json",
    };

    const response = await fetch(url, { ...options, headers });

    return response;
}

export async function authFetch(path, options = {}) {
    const url = window._env_.API_URL + path
    const token = localStorage.getItem("token");
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


export async function authPostFile(path, formData) {
    const url = window._env_.API_URL + path
    const token = localStorage.getItem("token");

    const response = await fetch(url, {
        headers: {
            'Authorization': `Bearer ${token}`
            // DO NOT add 'Content-Type'
        },
        method: 'POST',
        body: formData
    });

    if (response.status === 401) {
        localStorage.removeItem("token");
        window.location.href = "/login";
    }
}

