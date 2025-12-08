//Needed when run locally with mounting from docker compose
//see line :
//- ./data/nginx/env.js:/var/www/mn-ui/dist/env.js:ro

//
//rename env.js !
//

window._env_ = {
    API_URL: 'https://<<YOUR_DOMAIN>>',
};
