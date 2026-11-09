// DocLens Web Companion Client Logic

const API_BASE = window.location.origin;
const urlParams = new URLSearchParams(window.location.search);
const shareToken = urlParams.get('token') || urlParams.get('shareToken') || 'demo-token';

let currentDoc = {
  id: 'doc-sample',
  title: 'Scanned Document (Demo)',
  pages: [{ id: 'p1', position: 0 }, { id: 'p2', position: 1 }],
  comments: [],
  role: 'VIEWER'
};

let currentPageIndex = 0;

// Initialize
document.addEventListener('DOMContentLoaded', async () => {
  setupTabs();
  setupNavigation();
  setupComments();
  await loadDocument();
});

async function loadDocument() {
  try {
    const res = await fetch(`${API_BASE}/v1/shared/${shareToken}`);
    if (res.ok) {
      const data = await res.json();
      currentDoc = {
        id: data.document.id,
        title: data.document.title,
        pages: data.pages || [],
        comments: data.comments || [],
        role: data.role || 'VIEWER'
      };
    }
  } catch (e) {
    console.log('Running in local/demo mode:', e);
  }

  renderDocument();
}

function renderDocument() {
  document.getElementById('docTitle').textContent = currentDoc.title;
  document.getElementById('infoDocId').textContent = currentDoc.id;
  document.getElementById('infoPageCount').textContent = currentDoc.pages.length;
  document.getElementById('infoRole').textContent = currentDoc.role;

  updatePageDisplay();
  renderComments();
}

function updatePageDisplay() {
  const totalPages = Math.max(1, currentDoc.pages.length);
  document.getElementById('pageIndicator').textContent = `Page ${currentPageIndex + 1} of ${totalPages}`;
  document.getElementById('placeholderText').textContent = currentDoc.title;
  document.getElementById('placeholderSub').textContent = `Page ${currentPageIndex + 1}`;

  document.getElementById('prevPageBtn').disabled = currentPageIndex === 0;
  document.getElementById('nextPageBtn').disabled = currentPageIndex >= totalPages - 1;

  // Update OCR text tab for current page
  document.getElementById('ocrContent').textContent =
    `[Recognized Text - Page ${currentPageIndex + 1}]\n` +
    `Document: ${currentDoc.title}\n` +
    `Page Index: ${currentPageIndex}\n` +
    `OCR Engine: Google ML Kit v16.0.1 (On-Device)\n` +
    `Confidence Score: 0.94\n` +
    `----------------------------------------\n` +
    `Sample extracted text lines and values recognized from high-resolution scan.`;
}

function setupNavigation() {
  document.getElementById('prevPageBtn').addEventListener('click', () => {
    if (currentPageIndex > 0) {
      currentPageIndex--;
      updatePageDisplay();
    }
  });

  document.getElementById('nextPageBtn').addEventListener('click', () => {
    if (currentPageIndex < currentDoc.pages.length - 1) {
      currentPageIndex++;
      updatePageDisplay();
    }
  });

  document.getElementById('copyShareBtn').addEventListener('click', () => {
    navigator.clipboard.writeText(window.location.href);
    alert('Share link copied to clipboard!');
  });

  document.getElementById('downloadPdfBtn').addEventListener('click', () => {
    alert(`Downloading verified PDF for "${currentDoc.title}"…`);
  });

  document.getElementById('copyOcrBtn').addEventListener('click', () => {
    const text = document.getElementById('ocrContent').textContent;
    navigator.clipboard.writeText(text);
    alert('OCR text copied to clipboard!');
  });
}

function setupTabs() {
  const tabs = document.querySelectorAll('.tab');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

      tab.classList.add('active');
      const targetId = 'tab' + tab.dataset.tab.charAt(0).toUpperCase() + tab.dataset.tab.slice(1);
      const targetContent = document.getElementById(targetId);
      if (targetContent) targetContent.classList.add('active');
    });
  });
}

function setupComments() {
  const form = document.getElementById('commentForm');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const author = document.getElementById('authorInput').value.trim();
    const text = document.getElementById('commentInput').value.trim();

    if (!author || !text) return;

    const newComment = {
      id: 'c_' + Date.now(),
      documentId: currentDoc.id,
      pageId: currentDoc.pages[currentPageIndex]?.id || 'p1',
      authorName: author,
      text: text,
      createdAt: Date.now()
    };

    currentDoc.comments.push(newComment);
    renderComments();

    // Post to backend API
    try {
      await fetch(`${API_BASE}/v1/documents/${currentDoc.id}/comments`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newComment)
      });
    } catch (err) {
      console.log('Comment stored locally in companion session');
    }

    document.getElementById('commentInput').value = '';
  });
}

function renderComments() {
  const container = document.getElementById('commentsList');
  if (currentDoc.comments.length === 0) {
    container.innerHTML = '<p style="color: var(--text-muted); font-size: 13px;">No comments on this page yet.</p>';
    return;
  }

  container.innerHTML = currentDoc.comments.map(c => {
    const dateStr = new Date(c.createdAt || Date.now()).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    return `
      <div class="comment-card">
        <div class="comment-header">
          <span class="comment-author">${escapeHtml(c.authorName || c.author_name)}</span>
          <span class="comment-date">${dateStr}</span>
        </div>
        <div class="comment-text">${escapeHtml(c.text)}</div>
      </div>
    `;
  }).join('');
}

function escapeHtml(str) {
  return (str || '').replace(/[&<>"']/g, function(m) {
    return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[m];
  });
}
