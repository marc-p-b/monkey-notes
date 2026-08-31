window._env_ = {
    // Same origin: Traefik routes ${HOST}/api to the api service,
    // so no hostname needs substituting at deploy time.
    API_URL: '/api/',
};
