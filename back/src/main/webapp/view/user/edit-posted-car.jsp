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
          <c:set var="accountNavActive" value="listings" />
          <%@ include file="../components/user-sidebar.jsp" %>
          <div class="account-content">
            <div class="form-wrapper medium">
          <h2 class="form-header">Edit Posted Car</h2>
          <!-- FORM -->
          <form:form action="editCarProcess" method="POST" modelAttribute="car">
            <form:hidden path="idCar" />

            <label class="form-label">Make</label>
            <div>
              <form:errors path="make" cssClass="error" />
            </div>
            <form:input class="form-control" type="text" path="make" cssErrorClass="form-control error-border" />

            <label class="form-label">Model</label>
            <div>
              <form:errors path="model" cssClass="error" />
            </div>
            <form:input class="form-control" type="text" path="model" cssErrorClass="form-control error-border" />

            <label class="form-label">Year</label>
            <div>
              <form:errors path="year" cssClass="error" />
            </div>
            <form:input class="form-control" type="text" path="year" cssErrorClass="form-control error-border" />

            <label class="form-label">Price</label>
            <div>
              <form:errors path="price" cssClass="error" />
            </div>
            <form:input class="form-control" type="number" path="price" cssErrorClass="form-control error-border" />

            <label class="form-label">Auction ends</label>
            <div>
              <form:errors path="auctionEndTime" cssClass="error" />
            </div>
            <form:input class="form-control" type="datetime-local" path="auctionEndTime" cssErrorClass="form-control error-border" />

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
