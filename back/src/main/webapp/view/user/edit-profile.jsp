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
          <c:if test="${addressRequired or not empty addressError}">
            <div class="alert alert-warning">
              ${not empty addressError ? addressError : 'Add a complete physical address before checking out car parts.'}
            </div>
          </c:if>
          <!-- FORM -->
          <form:form action="editProfileProcess" method="POST" modelAttribute="profile">
            <form:hidden path="idProfile" />
            <c:if test="${addressRequired or param.checkoutReturn != null}">
              <input type="hidden" name="checkoutReturn" value="true" />
            </c:if>

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

            <hr />
            <h3 class="h5">Shipping address</h3>
            <p class="small text-secondary">Optional for general account use; required for physical car-parts delivery.</p>
            <form:errors path="physicalAddressConsistent" cssClass="error d-block mb-2" />

            <label class="fs-6 form-label">Street address</label>
            <form:errors path="streetAddress" cssClass="error d-block" />
            <form:input class="form-control" type="text" path="streetAddress" cssErrorClass="form-control error-border" />

            <label class="fs-6 form-label">City</label>
            <form:errors path="city" cssClass="error d-block" />
            <form:input class="form-control" type="text" path="city" cssErrorClass="form-control error-border" />

            <label class="fs-6 form-label">Postal code</label>
            <form:errors path="postalCode" cssClass="error d-block" />
            <form:input class="form-control" type="text" path="postalCode" cssErrorClass="form-control error-border" />

            <label class="fs-6 form-label">Country</label>
            <form:errors path="country" cssClass="error d-block" />
            <form:input class="form-control" type="text" path="country" cssErrorClass="form-control error-border" />

            <label class="fs-6 form-label">About</label>
            <form:input class="form-control" type="text" path="about" />

            <button class="btn btn-primary form-button mt-3" type="submit">${addressRequired ? 'Save address and return to cart' : 'Save Edit'}</button>
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
