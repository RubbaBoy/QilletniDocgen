// (If you loaded FlexSearch via a script, then window.FlexSearch.Document is available)
const index = new FlexSearch.Document({
    document: {
        id: "id",

        // which fields to tokenize and search
        index: [
            "title",
            "description",
        ],
        store: ['title', 'description', 'type', 'file', 'parent', 'library', 'url'], // specify which fields to store
    },
    field: {
        title: {
            boost: 3 // weigh 'title' more than the default (1)
        },
        description: {
            boost: 1 // or omit for default weighting
        }
    },
    tokenize: "forward", // or "full", "strict", etc.
    depth: 3,     // higher = deeper subscript matches 
    threshold: 2, // fuzzy searching, higher = more lenient
    bool: "and"
});

let docData = []; // Will store the JSON array

function initializeData(indexPath) {
    fetch(indexPath)
        .then(response => response.json())
        .then(data => {
            docData = data;
            data.forEach(item => {
                index.add(item);  // Add each doc item to the index
            });
        });
}

const searchInput = document.getElementById('search-input');

function flattenFlexsearchResults(rawResults) {
    const flattenedHits = [];

    // 1. Flatten in the order groups are listed
    for (const group of rawResults) {
        flattenedHits.push(...group.result);
    }

    // 2. Deduplicate by `id`, preserving first occurrence order
    const seen = new Set();
    const finalDocs = [];
    for (const item of flattenedHits) {
        if (!seen.has(item.id)) {
            seen.add(item.id);
            finalDocs.push(item.doc);
        }
    }

    return finalDocs;
}

searchInput.addEventListener('input', async function() {
    const query = this.value.trim();
    if (!query) {
        hidePopup();
        return;
    }

    const rawResults = await index.search(query, { limit: 10, enrich: true });
    
    let flattenedResults = flattenFlexsearchResults(rawResults);
    
    displaySearchResults(flattenedResults);
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
        // li.className = 'collection-item';

        // Create the <a> element
        const a = document.createElement('a');
        a.href = doc.url;
        a.className = 'result-link';
        a.tabIndex = 0;

        // Optionally add aria roles or labels if needed
        a.setAttribute('aria-label', `View details about ${doc.title}`);

        let parentHtml = '';
        if (doc.parent !== undefined && doc.parent !== null) {
            parentHtml = `<span>${doc.parent}</span>`;
        }

        // Add the HTML content to the <a>
        a.innerHTML = `
          <div class="result-line"><span class="result-title">${doc.title}</span>${parentHtml}</div>
          <div class="result-line"><small class="result-description">${doc.description || ''}</small><small class="result-file">${doc.file}</small></div>
        `;

        li.appendChild(a);
        list.appendChild(li);
    });

    // Show the popup
    popup.style.display = 'block';
}

function hidePopup() {
    const popup = document.getElementById('search-popup');
    popup.style.display = 'none';
}

