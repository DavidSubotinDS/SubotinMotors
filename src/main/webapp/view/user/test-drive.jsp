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
          <c:set var="accountNavActive" value="appointments" />
          <%@ include file="../components/user-sidebar.jsp" %>
          <div class="account-content">
            <div class="form-wrapper small">
          <h2 class="form-header">Test Drive</h2>
          <p class="fw-bold fs-5 m-0 ms-1">${car.make} ${car.model} ${car.year}</p>
          <div class="car-thumbnail mb-3">
            <img class="img-thumbnail" src="data:${car.carPicture.fileType};base64,${car.carPicture.image}" alt="${car.make}" />
          </div>
          <!-- FORM -->
          <form:form action="testDriveProcess" method="POST" modelAttribute="testDrive">
            <p class="error">${message}</p>
            <label class="fs-6 form-label">Date</label>
            <form:errors path="date" cssClass="error" />
            <form:input class="form-control" type="date" path="date" id="inputDate" />

            <input type="hidden" name="carId" value="${car.idCar}" />

            <button class="btn btn-primary form-button" type="submit">Test Drive</button>
          </form:form>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <%@ include file="../components/footer.jsp" %>
    <script src="/js/test-drive.js"></script>
  </body>
</html>
