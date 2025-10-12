<script lang="ts" setup>
//TODO refactor (imported from previous version)
    // function documentLoaded() {
    //
    //     const container = document.getElementById('sections');
    //     const topLinks = document.getElementById('topLinks');
    //     const ul = document.createElement('ul');
    //     container.appendChild(ul);
    //
    //     fetch('/ne/verb/' + namedEntityVerb, {
    //         method: 'GET'
    //     })
    //     .then(response => {
    //         if (!response.ok) {
    //             throw new Error('Network response was not ok');
    //         }
    //         return response.json();
    //     })
    //     .then(data => {
    //         data.forEach(ne => {
    //             const name = ne.value + ' count ' + ne.count;
    //
    //             const li = document.createElement('li');
    //
    //             const a = document.createElement('a');
    //             a.href = '/ne/values/' + ne.verb + '/' + ne.value;
    //             a.textContent = 'view';
    //             a.classList.add('clickableLink');
    //
    //             const span = document.createElement('span');
    //             span.appendChild(document.createTextNode(name))
    //             span.appendChild(document.createTextNode(' - '))
    //             span.appendChild(a);
    //
    //             li.appendChild(span);
    //             ul.appendChild(li);
    //         });
    //     })
    //     .catch(error => console.error('Error:', error));
    //
    //     document.addEventListener('click', function(event) {
    //         if (event.target.matches('a.clickableLink')) {
    //             event.preventDefault();
    //             const linkHref = event.target.href;
    //
    //             const ul = document.createElement('ul');
    //             event.target.appendChild(ul);
    //
    //             fetch(linkHref, {
    //                 method: 'GET'
    //             })
    //             .then(response => {
    //                 if (!response.ok) {
    //                     throw new Error('Network response was not ok');
    //                 }
    //                 return response.json();
    //             })
    //             .then(data => {
    //                 data.forEach(ne => {
    //                     const a = document.createElement('a');
    //                     a.textContent = ne.fileName + ' page ' + (ne.pageNumber + 1);
    //                     a.href = '/v/transcript/' + ne.fileId;
    //
    //                     const li = document.createElement('li');
    //                     li.appendChild(a);
    //                     ul.appendChild(li);
    //
    //
    //
    //                 });
    //             })
    //             .catch(error => console.error('Error:', error));
    //
    //
    //
    //
    //         }
    //     });
    //
    // }
    // document.addEventListener('DOMContentLoaded', documentLoaded);
</script>
