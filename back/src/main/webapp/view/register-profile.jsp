<%@ include file="components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="components/header.jsp" %>
    <link rel="stylesheet" href="/css/form.css" />
  </head>
  <body>
    <!-- Navbar -->
    <%@ include file="components/navbar.jsp" %>

    <!-- Main -->
    <main>
      <div class="container d-flex justify-content-center">
        <div class="form-wrapper medium">
          <h2 class="form-header">Register</h2>
          <!-- FORM -->
          <form:form action="profileProcess" method="POST" modelAttribute="profile">
            <label class="form-label fs-6">First name</label>
            <div>
              <form:errors path="firstName" cssClass="error" />
            </div>
            <form:input class="form-control" type="text" path="firstName" cssErrorClass="form-control error-border" />

            <label class="form-label fs-6">Last name</label>
            <div>
              <form:errors path="lastName" cssClass="error" />
            </div>
            <form:input class="form-control" type="text" path="lastName" cssErrorClass="form-control error-border" />

            <label class="form-label fs-6">Phone number</label>
            <div>
              <form:errors path="phoneNumber" cssClass="error" />
            </div>
            <form:input class="form-control" type="text" path="phoneNumber" cssErrorClass="form-control error-border" />

            <hr />
            <h3 class="h5">Shipping address <span class="text-secondary fw-normal">(optional)</span></h3>
            <p class="small text-secondary">You can skip this during registration. A complete address is required only when ordering physical car parts.</p>
            <form:errors path="physicalAddressConsistent" cssClass="error d-block mb-2" />

            <label class="form-label fs-6">Street address</label>
            <form:errors path="streetAddress" cssClass="error d-block" />
            <form:input class="form-control" type="text" path="streetAddress" cssErrorClass="form-control error-border" />

            <label class="form-label fs-6">City</label>
            <form:errors path="city" cssClass="error d-block" />
            <form:input class="form-control" type="text" path="city" cssErrorClass="form-control error-border" />

            <label class="form-label fs-6">Postal code</label>
            <form:errors path="postalCode" cssClass="error d-block" />
            <form:input class="form-control" type="text" path="postalCode" cssErrorClass="form-control error-border" />

            <label class="form-label fs-6">Country</label>
            <form:errors path="country" cssClass="error d-block" />
            <form:input class="form-control" type="text" path="country" cssErrorClass="form-control error-border" />

            <label class="form-label fs-6">About</label>
            <form:input class="form-control" type="text" path="about" />

            <button class="btn btn-primary form-button mt-3" type="submit">Register</button>
          </form:form>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <%@ include file="components/footer.jsp" %>
  </body>
</html>
