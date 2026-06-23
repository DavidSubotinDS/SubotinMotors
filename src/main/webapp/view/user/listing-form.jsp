<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/form.css" />
    <link rel="stylesheet" href="/css/user.css" />
  </head>
  <body>
    <%@ include file="../components/navbar.jsp" %>
    <main class="account-main">
      <div class="container py-4 py-lg-5">
        <div class="account-shell">
          <c:set var="accountNavActive" value="fixedListings" />
          <%@ include file="../components/user-sidebar.jsp" %>
          <div class="account-content">
            <div class="form-wrapper listing-form-wrapper">
              <h1 class="form-header">${editing ? 'Edit car listing' : 'List a car for sale'}</h1>
              <p class="text-secondary">Fixed-price listings are separate from timed auctions.</p>
              <c:choose>
                <c:when test="${editing}"><c:url var="listingFormAction" value="/user/listings/${listingForm.idListing}" /></c:when>
                <c:otherwise><c:url var="listingFormAction" value="/user/listings" /></c:otherwise>
              </c:choose>
              <form:form
                action="${listingFormAction}"
                method="POST"
                enctype="multipart/form-data"
                modelAttribute="listingForm">
                <form:hidden path="idListing" />
                <form:errors path="depositLessThanPrice" cssClass="error d-block mb-2" />

                <label class="form-label">Title</label>
                <form:errors path="title" cssClass="error d-block" />
                <form:input class="form-control" path="title" />

                <div class="row">
                  <div class="col-md-6">
                    <label class="form-label">Make</label>
                    <form:errors path="make" cssClass="error d-block" />
                    <form:input class="form-control" path="make" />
                  </div>
                  <div class="col-md-6">
                    <label class="form-label">Model</label>
                    <form:errors path="model" cssClass="error d-block" />
                    <form:input class="form-control" path="model" />
                  </div>
                </div>

                <div class="row">
                  <div class="col-md-6">
                    <label class="form-label">Production year</label>
                    <form:errors path="year" cssClass="error d-block" />
                    <form:input class="form-control" path="year" />
                  </div>
                  <div class="col-md-6">
                    <label class="form-label">Mileage (km)</label>
                    <form:errors path="mileage" cssClass="error d-block" />
                    <form:input class="form-control" type="number" min="0" path="mileage" />
                  </div>
                </div>

                <div class="row">
                  <div class="col-md-6">
                    <label class="form-label">Fuel type</label>
                    <form:errors path="fuelType" cssClass="error d-block" />
                    <form:input class="form-control" path="fuelType" placeholder="Petrol, diesel, electric..." />
                  </div>
                  <div class="col-md-6">
                    <label class="form-label">Transmission</label>
                    <form:errors path="transmission" cssClass="error d-block" />
                    <form:input class="form-control" path="transmission" placeholder="Manual or automatic" />
                  </div>
                </div>

                <div class="row">
                  <div class="col-md-6">
                    <label class="form-label">Listed price (EUR)</label>
                    <form:errors path="price" cssClass="error d-block" />
                    <form:input class="form-control" type="number" min="0.01" step="0.01" path="price" />
                  </div>
                  <div class="col-md-6">
                    <label class="form-label">Reservation deposit (EUR)</label>
                    <form:errors path="depositAmount" cssClass="error d-block" />
                    <form:input class="form-control" type="number" min="0.01" step="0.01" path="depositAmount" />
                  </div>
                </div>

                <label class="form-label">Description</label>
                <form:errors path="description" cssClass="error d-block" />
                <form:textarea class="form-control" rows="6" path="description" />

                <label class="form-label">Car picture ${editing ? '(optional replacement)' : ''}</label>
                <span class="error d-block">${fileError}</span>
                <input class="form-control" type="file" name="imageFile" accept="image/png,image/jpeg" ${editing ? '' : 'required'} />

                <button class="btn btn-primary form-button mt-3" type="submit">${editing ? 'Save listing' : 'Publish listing'}</button>
              </form:form>
            </div>
          </div>
        </div>
      </div>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
