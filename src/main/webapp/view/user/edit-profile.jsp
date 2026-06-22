<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/form.css" />
    <link rel="stylesheet" href="/css/user.css" />
  </head>
  <body>
    <!-- Navbar -->
    <%@ include file="../components/navbar.jsp" %>

    <!-- Main -->
    <main class="account-main">
      <div class="container py-4 py-lg-5">
        <div class="account-shell">
          <c:set var="accountNavActive" value="profile" />
          <%@ include file="../components/user-sidebar.jsp" %>
          <div class="account-content">
            <div class="form-wrapper medium">
          <h2 class="form-header">Edit Profile</h2>
          <!-- FORM -->
          <form:form action="editProfileProcess" method="POST" modelAttribute="profile">
            <form:hidden path="idProfile" />

            <label class="fs-6 form-label">Email</label>
            <div>
              <form:errors path="email" cssClass="error" />
            </div>
            <form:input class="form-control" type="email" path="email" cssErrorClass="form-control error-border" />

            <label class="fs-6 form-label">First name</label>
            <div>
              <form:errors path="firstName" cssClass="error" />
            </div>
            <form:input class="form-control" type="text" path="firstName" cssErrorClass="form-control error-border" />

            <label class="fs-6 form-label">Last name</label>
            <div>
              <form:errors path="lastName" cssClass="error" />
            </div>
            <form:input class="form-control" type="text" path="lastName" cssErrorClass="form-control error-border" />

            <label class="fs-6 form-label">Phone number</label>
            <div>
              <form:errors path="phoneNumber" cssClass="error" />
            </div>
            <form:input class="form-control" type="text" path="phoneNumber" cssErrorClass="form-control error-border" />

            <label class="fs-6 form-label">Address</label>
            <div>
              <form:errors path="address" cssClass="error" />
            </div>
            <form:input class="form-control" type="text" path="address" cssErrorClass="form-control error-border" />

            <label class="fs-6 form-label">About</label>
            <form:input class="form-control" type="text" path="about" />

            <button class="btn btn-primary form-button mt-3" type="submit">Save Edit</button>
          </form:form>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
