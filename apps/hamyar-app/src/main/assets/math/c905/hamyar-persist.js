/* همیار: ذخیرهٔ پاسخ/آرشیو/نمره روی دستگاه */
(function () {
  if (window.__hamyarHooked) return;
  window.__hamyarHooked = true;

  function persist() {
    var payload = {
      stats: typeof stats !== "undefined" ? stats : {},
      repeatCount: typeof repeatCount !== "undefined" ? repeatCount : {},
      archive: typeof archive !== "undefined" ? archive : [],
      currentIndex: typeof currentIndex !== "undefined" ? currentIndex : 0,
      currentMode: typeof currentMode !== "undefined" ? currentMode : "all",
      currentRound: typeof currentRound !== "undefined" ? currentRound : 1,
      radios: {}
    };
    document.querySelectorAll('input[type="radio"]:checked').forEach(function (r) {
      payload.radios[r.name] = r.value;
    });
    var raw = JSON.stringify(payload);
    try { localStorage.setItem("hamyar_html_state", raw); } catch (e) {}
    if (window.Hamyar) {
      try { Hamyar.saveState(raw); } catch (e) {}
    }
  }

  function restore() {
    var raw = "";
    if (window.Hamyar) {
      try { raw = Hamyar.loadState() || ""; } catch (e) {}
    }
    if (!raw) {
      try { raw = localStorage.getItem("hamyar_html_state") || ""; } catch (e) {}
    }
    if (!raw) return;
    try {
      var p = JSON.parse(raw);
      if (p.stats && typeof stats !== "undefined") stats = p.stats;
      if (p.repeatCount && typeof repeatCount !== "undefined") repeatCount = p.repeatCount;
      if (p.archive && typeof archive !== "undefined") archive = p.archive;
      if (typeof currentIndex !== "undefined" && typeof p.currentIndex === "number") currentIndex = p.currentIndex;
      if (p.currentMode && typeof currentMode !== "undefined") currentMode = p.currentMode;
      if (typeof currentRound !== "undefined" && typeof p.currentRound === "number") currentRound = p.currentRound;
      if (p.radios) {
        Object.keys(p.radios).forEach(function (name) {
          var el = document.querySelector('input[name="' + name + '"][value="' + p.radios[name] + '"]');
          if (!el) return;
          el.checked = true;
          var opt = el.closest(".mc-option");
          if (opt) opt.classList.add("selected");
        });
      }
      if (typeof renderCard === "function") renderCard();
    } catch (e) {}
  }

  function wrap(name, after) {
    if (typeof window[name] !== "function") return;
    var orig = window[name];
    window[name] = function () {
      var cardId = null;
      try {
        if (typeof cardsOrder !== "undefined" && typeof currentIndex !== "undefined" && cardsOrder[currentIndex]) {
          cardId = String(cardsOrder[currentIndex].id);
        }
      } catch (e) {}
      var r = orig.apply(this, arguments);
      try { persist(); } catch (e) {}
      try { if (after) after(cardId); } catch (e) {}
      return r;
    };
  }

  wrap("confirmAnswer", function (cardId) {
    if (!window.Hamyar || !cardId || typeof stats === "undefined") return;
    var s = stats[cardId] || stats[parseInt(cardId, 10)];
    if (s && s.answered) Hamyar.recordItem(cardId, !!s.correct);
  });
  wrap("nextCard", null);
  wrap("showStats", function () {
    if (!window.Hamyar || typeof allCards === "undefined" || typeof stats === "undefined") return;
    var ok = 0, wrong = [], tot = allCards.length;
    allCards.forEach(function (c) {
      var s = stats[c.id];
      if (s && s.answered) {
        if (s.correct) ok++;
        else wrong.push(String(c.id));
      }
    });
    var pct = tot > 0 ? Math.round((ok * 100) / tot) : 0;
    Hamyar.recordExam(pct, tot, ok, wrong.join(","));
  });
  wrap("verifyAll", function () {
    if (!window.Hamyar) return;
    var ok = 0, tot = 0, wrong = [];
    document.querySelectorAll(".question[data-q]").forEach(function (q) {
      tot++;
      var id = q.getAttribute("data-q");
      var sel = q.querySelector('input[type="radio"]:checked');
      var fb = q.querySelector(".feedback");
      var correct = fb ? fb.getAttribute("data-correct") : "";
      var good = !!(sel && correct && sel.value === correct);
      if (sel) Hamyar.recordItem(id, good);
      if (good) ok++;
      else if (sel) wrong.push(id);
    });
    var pct = tot > 0 ? Math.round((ok * 100) / tot) : 0;
    Hamyar.recordExam(pct, tot, ok, wrong.join(","));
  });
  wrap("saveAnswers", null);
  wrap("restartAll", null);
  wrap("startRepeatRound", null);

  document.addEventListener("change", function () { try { persist(); } catch (e) {} });
  restore();
})();
