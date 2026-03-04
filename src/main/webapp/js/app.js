/**
 * IT Help Desk — app.js
 * Day 4: Minor interactivity — reply form toggle
 * Vanilla JS, no framework required.
 */

document.addEventListener("DOMContentLoaded", function () {
  // ── Reply toggle ─────────────────────────────────────────────────────────
  // Each "↩ Reply" button shows/hides the inline reply form for that comment.
  // Clicking one button collapses any other open reply forms first.

  const replyToggles = document.querySelectorAll(".reply-toggle");

  replyToggles.forEach(function (btn) {
    btn.addEventListener("click", function () {
      const commentId = btn.getAttribute("data-comment-id");
      const targetForm = document.getElementById("reply-" + commentId);

      if (!targetForm) return;

      const isOpen = !targetForm.classList.contains("d-none");

      // Collapse all open reply forms
      document.querySelectorAll(".reply-form").forEach(function (form) {
        form.classList.add("d-none");
      });

      // If it was closed, open it; if already open, leave collapsed (toggle)
      if (!isOpen) {
        targetForm.classList.remove("d-none");
        // Focus the textarea for immediate typing
        const textarea = targetForm.querySelector("textarea");
        if (textarea) textarea.focus();
      }
    });
  });

  // ── Auto-dismiss flash alerts after 5 seconds ─────────────────────────────
  const alerts = document.querySelectorAll(
    ".alert.alert-success, .alert.alert-danger",
  );
  alerts.forEach(function (alert) {
    setTimeout(function () {
      // Use Bootstrap's dismiss if available, otherwise just hide
      if (window.bootstrap && bootstrap.Alert) {
        const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
        bsAlert.close();
      } else {
        alert.style.display = "none";
      }
    }, 5000);
  });

  // ── Priority badge preview on create-ticket form ──────────────────────────
  const prioritySelect = document.getElementById("priority");
  if (prioritySelect) {
    prioritySelect.addEventListener("change", function () {
      const val = prioritySelect.value;
      // Remove all priority classes
      prioritySelect.className = "form-select";
      if (val) {
        // Map priority → Bootstrap contextual border colour for quick feedback
        const colorMap = {
          LOW: "border-success",
          MEDIUM: "border-warning",
          HIGH: "border-orange", // custom, handled via CSS if desired
          CRITICAL: "border-danger",
        };
        const cls = colorMap[val];
        if (cls) prioritySelect.classList.add(cls);
      }
    });
  }
});
