document.addEventListener("DOMContentLoaded", () => {
  const input = document.querySelector("[data-comment-image-input]");
  const preview = document.querySelector("[data-comment-image-preview]");
  if (!input || !preview) {
    return;
  }

  const image = preview.querySelector("img");
  const removeButton = preview.querySelector("[data-comment-image-remove]");
  let previewUrl;

  const clearPreview = () => {
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      previewUrl = undefined;
    }
    input.value = "";
    image.removeAttribute("src");
    preview.hidden = true;
  };

  input.addEventListener("change", () => {
    if (!input.files || input.files.length === 0) {
      clearPreview();
      return;
    }
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
    previewUrl = URL.createObjectURL(input.files[0]);
    image.src = previewUrl;
    preview.hidden = false;
  });

  removeButton.addEventListener("click", clearPreview);
});
