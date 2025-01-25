// (If you loaded FlexSearch via a script, then window.FlexSearch.Document is available)
const index = new FlexSearch.Document({
    document: {
        id: "id",

        // which fields to tokenize and search
        index: [
            "title",
            "description"
            // optionally "type" or "parent" if you want them searched
        ],
        store: ['title', 'description', 'type', 'file', 'parent', 'library', 'url'] // specify which fields to store
    },
    tokenize: "forward", // or "full", "strict", etc.
    // Tweak other config like encode, stemmer, depth, etc., as needed
});

let docData = []; // Will store the JSON array

function initializeData(libraryName) {
    fetch(`/${libraryName}.json`)
        .then(response => response.json())
        .then(data => {
            docData = data;
            data.forEach(item => {
                index.add(item);  // Add each doc item to the index
            });
        });
}

const searchInput = document.getElementById('search-input');

searchInput.addEventListener('input', async function() {
    const query = this.value.trim();
    if (!query) {
        hidePopup();
        return;
    }
    const results = await index.search(query, { enrich: true });
    
    // Flatten or combine these results
    const finalHits = [];
    for (let r of results) {
        finalHits.push(...r.result);
    }

    // Unique them by doc.id if needed:
    const unique = {};
    for (let hit of finalHits) {
        unique[hit.doc.id] = hit.doc;
    }

    displaySearchResults(Object.values(unique));
});

function displaySearchResults(docs) {
    const popup = document.getElementById('search-popup');
    const list = document.getElementById('search-results-list');

    // Clear old results
    list.innerHTML = '';

    // If no query or no docs found, hide the popup and return
    if (!docs || docs.length === 0) {
        hidePopup();
        return;
    }
    
    const nav = document.querySelector('nav');
    const navHeight = nav ? nav.offsetHeight : 64;
    popup.style.top = `${navHeight}px`;
    

    const searchInputRect = searchInput.getBoundingClientRect();
    const searchBarDistanceFromLeft = searchInputRect.left;
    
    popup.style.left = `${searchBarDistanceFromLeft}px`;

    // For each doc object, create a <li class="collection-item">
    docs.forEach(doc => {
        // doc might have { id, title, description, type, etc. }
        const li = document.createElement('li');
        li.className = 'collection-item';
        
        let parentHtml = ''
        if (doc.parent !== undefined) {
            parentHtml = `<span>${doc.parent}</span>`
        }

        // Example display with doc title, short description, etc.
        li.innerHTML = `
      <div class="result-line"><span style="font-weight: 600;">${doc.title}</span>${parentHtml}</div>
      <div class="result-line"><small class="result-description">${doc.description || ''}</small><small class="result-file">${doc.file}</small></div>
    `;

        // Optionally link to the doc
        li.addEventListener('click', () => {
            // E.g. navigate to doc link
            window.location.href = makeDocLink(doc);
        });

        list.appendChild(li);
    });

    // Show the popup
    popup.style.display = 'block';
}

function hidePopup() {
    const popup = document.getElementById('search-popup');
    popup.style.display = 'none';
}

// Example link function
function makeDocLink(doc) {
    if (doc.type === 'entity') {
        // e.g. /library/Artist.html
        return `/library/${doc.url}`;
    } else if (doc.type === 'method') {
        // e.g. /library/Artist.html#method-getName
        return `/library/${doc.url}#${doc.id}`;
    } else {
        return '#';
    }
}

