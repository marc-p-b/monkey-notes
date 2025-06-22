function requestLink(event, statusContainer) {
    const linkHref = event.target.href;
    fetch(linkHref, {
        method: 'GET'
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.text();
    }).then(bodyText => {
    console.log('Response body as text:', bodyText);
    statusContainer.textContent = bodyText;
    })
    .catch(error => {
        console.error('Fetch error:', error);
    });

}