// ================== Sidebar Toggle ==================
document.addEventListener("DOMContentLoaded", function () {
  const menuToggle = document.getElementById("menuToggle");
  const sidebar = document.getElementById("sidebar");
  const closeSidebar = document.getElementById("closeSidebar");
  const mainContent = document.getElementById("mainContent");

  if (menuToggle && sidebar && closeSidebar && mainContent) {
    // Get saved state
    const sidebarState = localStorage.getItem("sidebarState");
    if (sidebarState === "open") {
      sidebar.classList.remove("d-none");
      mainContent.classList.add("with-sidebar");
    } else {
      sidebar.classList.add("d-none");
      mainContent.classList.remove("with-sidebar");
    }

    // Toggle open
    menuToggle.addEventListener("click", function () {
      sidebar.classList.remove("d-none");
      mainContent.classList.add("with-sidebar");
      localStorage.setItem("sidebarState", "open");
    });

    // Toggle close
    closeSidebar.addEventListener("click", function () {
      sidebar.classList.add("d-none");
      mainContent.classList.remove("with-sidebar");
      localStorage.setItem("sidebarState", "closed");
    });
  }
});

// ================== File Input Label Update ==================
const imageInput = document.getElementById("imageInput");
if (imageInput) {
  imageInput.addEventListener("change", function () {
    const fileName = this.files[0]?.name || "Choose image";
    this.nextElementSibling.innerText = fileName;
  });
}

// ================== Auto-hide Alerts ==================
setTimeout(() => {
  const alert = document.querySelector(".alert");
  if (alert) {
    alert.classList.add("fade-out");
    setTimeout(() => alert.remove(), 600); // remove after fade
  }
}, 3000);

// ================== TinyMCE ==================
if (document.querySelector("textarea[name=description]")) {
  tinymce.init({
    selector: "textarea[name=description]",
    plugins: "link lists code table image media preview",
    toolbar:
      "undo redo | styles | bold italic underline | alignleft aligncenter alignright | bullist numlist outdent indent | link image media | preview code",
    menubar: false,
    branding: false,
    height: 300,
  });
}

// ================== Live Search ==================
const search = () => {
  let query = $("#search-input").val().trim();

  if (query === "") {
    $(".search-result").hide();
    return;
  }

  console.log("Searching for:", query);

  let url = `http://localhost:8080/search/${encodeURIComponent(query)}`;

  fetch(url)
    .then((response) => {
      if (!response.ok) {
        throw new Error("Network response was not ok");
      }
      return response.json();
    })
    .then((data) => {
      console.log("Data received:", data);

      if (data.length === 0) {
        $(".search-result").html(
          "<p class='list-group-item'>No results found</p>"
        );
        $(".search-result").show();
        return;
      }

      let text = `<div class='list-group'>`;
      data.forEach((contact) => {
        text += `<div class='search-result-item'>
            <a href='/user/${contact.cid}/contact'>${contact.name}</a>
         </div>`;
      });
      text += `</div>`;

      $(".search-result").html(text);
      $(".search-result").show();
    })
    .catch((error) => {
      console.error("Error during fetch:", error);
      $(".search-result").html(
        "<p class='list-group-item text-danger'>Error fetching results</p>"
      );
      $(".search-result").show();
    });
};
