
//TODO noAuthFetch()


export async function authFetch(path, options = {}) {
    const token = localStorage.getItem("token");

    //TODO env ??
    //const url = import.meta.env.VITE_API_URL + '/' + path
    const url = 'https://notes.monkeynotes.fr/api/' + path

    //const url = 'http://localhost:8080' + '/' + path

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
