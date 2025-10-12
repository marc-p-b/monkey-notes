export async function authFetch(path, options = {}) {
    const token = localStorage.getItem("token");
    const url = import.meta.env.VITE_API_URL + '/' + path

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
