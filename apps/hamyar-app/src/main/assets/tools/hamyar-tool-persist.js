(function () {
  "use strict";
  function dump() {
    var o = {};
    try {
      for (var i = 0; i < localStorage.length; i++) {
        var k = localStorage.key(i);
        if (k) o[k] = localStorage.getItem(k);
      }
    } catch (e) {}
    try {
      if (Array.isArray(window.tableRecords)) o.__tableRecords = JSON.stringify(window.tableRecords);
    } catch (e) {}
    try {
      if (window.HamyarBioNotes) o.__bioNotes = JSON.stringify(window.HamyarBioNotes);
    } catch (e) {}
    return JSON.stringify(o);
  }
  function faDigits(n) {
    return String(n).replace(/[0-9]/g, function (d) {
      return String.fromCharCode(0x06f0 + parseInt(d, 10));
    });
  }
  function rebuildTable() {
    var tbody = document.getElementById("tableBody");
    var cnt = document.getElementById("recordCount");
    if (!window.tableRecords) return;
    if (cnt) cnt.textContent = faDigits(window.tableRecords.length);
    if (!tbody) return;
    tbody.innerHTML = "";
    window.tableRecords.slice().reverse().forEach(function (entry) {
      var tr = document.createElement("tr");
      tr.innerHTML =
        "<td>" + faDigits(entry.id || "") + "</td>" +
        "<td>" + (entry.name || "") + "</td>" +
        "<td>" + (entry.input || "") + "</td>" +
        "<td>" + (entry.output || "") + "</td>" +
        "<td>" + (entry.time || "") + "</td>";
      tbody.appendChild(tr);
    });
  }
  function apply(json) {
    if (!json) return;
    try {
      var o = JSON.parse(json);
      Object.keys(o).forEach(function (k) {
        if (k === "__tableRecords") {
          try { window.tableRecords = JSON.parse(o[k] || "[]"); rebuildTable(); } catch (e) {}
          return;
        }
        if (k === "__bioNotes") {
          try { window.HamyarBioNotes = JSON.parse(o[k] || "[]"); } catch (e) {}
          return;
        }
        try { origSet.call(localStorage, k, o[k]); } catch (e) {}
      });
    } catch (e) {}
  }
  var origSet = Storage.prototype.setItem;
  Storage.prototype.setItem = function (k, v) {
    origSet.call(this, k, v);
    try { if (window.HamyarTool) HamyarTool.onSave(dump()); } catch (e) {}
  };
  function hookRecords() {
    if (!window.tableRecords || window.tableRecords.__hamyarHooked) return;
    var arr = window.tableRecords;
    arr.__hamyarHooked = true;
    var p = arr.push.bind(arr);
    arr.push = function () {
      var r = p.apply(this, arguments);
      try { if (window.HamyarTool) HamyarTool.onSave(dump()); } catch (e) {}
      return r;
    };
  }
  setInterval(hookRecords, 700);
  window.HamyarToolDump = dump;
  window.HamyarToolApply = apply;
  document.addEventListener("visibilitychange", function () {
    try { if (window.HamyarTool) HamyarTool.onSave(dump()); } catch (e) {}
  });
})();
