(function() {
    // A map to store anchorId to bookmarkName and payload
    const anchorMap = new Map();
    const BOOKMARK_PREFIX = 'risk_anchor_';

    function sanitizeName(s) {
        return String(s || '').replace(/[^A-Za-z0-9_]/g, '_');
    }

    window.Asc.plugin.init = function() {
        console.log('[risk-anchors] init');
        try { window.parent.postMessage(JSON.stringify({ action: 'risk.ready' }), '*'); } catch(_) {}
        // Fallback: poll localStorage for commands when cross-frame messaging is blocked
        try {
            var lastAnchorsVer = '';
            var lastGotoVer = '';
            setInterval(function() {
                try {
                    var aVer = localStorage.getItem('risk_anchors_ver') || '';
                    if (aVer && aVer !== lastAnchorsVer) {
                        lastAnchorsVer = aVer;
                        var raw = localStorage.getItem('risk_anchors_payload') || '[]';
                        var arr = [];
                        try { arr = JSON.parse(raw); } catch(_) { arr = []; }
                        if (Array.isArray(arr) && arr.length) {
                            console.log('[risk-anchors] polling anchors', arr.length);
                            handleSetAnchors(arr);
                        }
                    }
                    var gVer = localStorage.getItem('risk_goto_ver') || '';
                    if (gVer && gVer !== lastGotoVer) {
                        lastGotoVer = gVer;
                        var gid = localStorage.getItem('risk_goto_id') || '';
                        if (gid) {
                            console.log('[risk-anchors] polling goto', gid);
                            handleGotoAnchor(gid);
                        }
                    }
                } catch (e) {}
            }, 500);
        } catch (e) {}
    };

    window.Asc.plugin.button = function(id) {
        this.executeCommand("close", "");
    };

    // Listen for messages from the parent window (the Vue component)
    window.addEventListener("message", function(event) {
        try {
            var raw = event && event.data;
            var data = (typeof raw === 'string') ? JSON.parse(raw) : raw;
            console.log('[risk-anchors] received', data);
            if (!data || !data.action) return;
            switch (data.action) {
                case 'risk.setAnchors':
                    handleSetAnchors(Array.isArray(data.payload) ? data.payload : []);
                    break;
                case 'risk.gotoAnchor':
                    handleGotoAnchor(data.payload && data.payload.anchorId);
                    break;
                case 'risk.clearAnchors':
                    handleClearAnchors();
                    break;
                case 'forceSave':
                    try { window.Asc.plugin.executeMethod('Save', []); }
                    catch (e) { try { window.Asc.plugin.executeMethod('CallMenuCommand', ['FileSave']); } catch(_) {} }
                    try { window.parent.postMessage(JSON.stringify({ action: 'risk.forceSaved' }), '*'); } catch(_) {}
                    break;
            }
        } catch (error) {
            // Ignore non-JSON postMessages
        }
    });

    // Create bookmarks for all anchors using callCommand (document script API)
    function handleSetAnchors(anchors) {
        console.log('[risk-anchors] setAnchors count=', anchors.length);
        // First clear previous risk bookmarks
        clearAllRiskBookmarks(function() {
            anchorMap.clear();
            // Pass data via scope for callCommand
            Asc.scope._anchors = anchors;
            Asc.scope._prefix = BOOKMARK_PREFIX;
            console.log('[risk-anchors] callCommand add bookmarks');
            window.Asc.plugin.callCommand(function () {
                var doc = Api.GetDocument();
                var anchors = Asc.scope._anchors || [];
                var prefix = Asc.scope._prefix || 'risk_anchor_';
                var paragraphs = doc.GetAllParagraphs();
                for (var i = 0; i < anchors.length; i++) {
                    try {
                        var a = anchors[i] || {};
                        var rawName = String(a.anchorId || ('' + i));
                        var name = prefix + rawName.replace(/[^A-Za-z0-9_]/g, '_');
                        var hasIndices = (typeof a.paragraphIndex === 'number');
                        var added = false;

                        if (hasIndices && paragraphs && paragraphs[a.paragraphIndex]) {
                            var p = paragraphs[a.paragraphIndex];
                            var start = Math.max(0, a.startOffset || 0);
                            var end = Math.max(start, a.endOffset || start);
                            try {
                                var r = p.GetRange(start, end);
                                if (r) { r.AddBookmark(name); added = true; }
                            } catch (e1) {}
                        }

                        if (!added && a.text) {
                            try {
                                var range = doc.GetRangeByText(String(a.text), { matchCase: false, wholeWords: false });
                                if (range) { range.AddBookmark(name); added = true; }
                            } catch (e2) {}
                        }

                        // If still not added, try add an empty bookmark at doc start as last resort
                        if (!added) {
                            try {
                                var body = doc.GetBody();
                                var first = (body && body.GetElements && body.GetElements()[0]) || (paragraphs && paragraphs[0]);
                                if (first && first.GetRange) {
                                    var rr = first.GetRange(0, 0);
                                    if (rr) rr.AddBookmark(name);
                                }
                            } catch (e3) {}
                        }
                    } catch (e) {
                        // continue others
                    }
                }
            }, null, function () {
                // Cache mapping after creation
                for (var j = 0; j < anchors.length; j++) {
                    var a2 = anchors[j] || {};
                    var bookmarkName = BOOKMARK_PREFIX + sanitizeName(a2.anchorId || ('' + j));
                    anchorMap.set(a2.anchorId, { bookmarkName: bookmarkName, anchor: a2 });
                }
                try { window.parent.postMessage(JSON.stringify({ action: 'risk.anchorsSet', count: anchors.length }), '*'); } catch(_) {}
                console.log('[risk-anchors] anchors created & mapped', anchors.length);
            });
        });
    }

    function handleGotoAnchor(anchorId) {
        if (!anchorId) return;
        var rec = anchorMap.get(anchorId);
        var bookmarkName = rec && rec.bookmarkName ? rec.bookmarkName : (BOOKMARK_PREFIX + sanitizeName(anchorId));
        // Prefer direct bookmark navigation
        console.log('[risk-anchors] goto', bookmarkName);
        window.Asc.plugin.executeMethod('GoToBookmark', [bookmarkName]);
        try { window.parent.postMessage(JSON.stringify({ action: 'risk.located', anchorId: String(anchorId) }), '*'); } catch(_) {}
    }

    function handleClearAnchors() {
        console.log('[risk-anchors] clearAnchors');
        clearAllRiskBookmarks(function() {
            anchorMap.clear();
            try { window.parent.postMessage(JSON.stringify({ action: 'risk.cleared' }), '*'); } catch(_) {}
        });
    }

    function clearAllRiskBookmarks(done) {
        Asc.scope._prefix = BOOKMARK_PREFIX;
        console.log('[risk-anchors] callCommand clear bookmarks');
        window.Asc.plugin.callCommand(function () {
            var doc = Api.GetDocument();
            var prefix = Asc.scope._prefix || 'risk_anchor_';
            try {
                var list = doc.GetBookmarks();
                // list can be names array or bookmark objects depending on version
                for (var i = 0; i < list.length; i++) {
                    var item = list[i];
                    var name = (item && item.get_Name) ? item.get_Name() : item;
                    if (name && String(name).indexOf(prefix) === 0) {
                        try { doc.RemoveBookmark(String(name)); } catch (e) {}
                    }
                }
            } catch (e0) {
                // As a fallback, try remove some known names pattern
            }
        }, null, function () { if (typeof done === 'function') done(); });
    }
})();
