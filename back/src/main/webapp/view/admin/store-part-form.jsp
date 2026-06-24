<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/admin.css" />
  </head>
  <body>
    <%@ include file="../components/navbar.jsp" %>
    <main class="container py-5">
      <div class="mx-auto" style="max-width: 820px">
        <a href="${pageContext.request.contextPath}/admin/store/parts" class="text-decoration-none">← Parts inventory</a>
        <h1 class="fw-bold my-4">${partForm.idPart == 0 ? 'Add car part' : 'Edit car part'}</h1>
        <form:form modelAttribute="partForm" action="${pageContext.request.contextPath}/admin/store/parts" method="POST" class="row g-3">
          <form:hidden path="idPart" />
          <div class="col-md-5">
            <label class="form-label">SKU</label>
            <form:input path="sku" cssClass="form-control" />
            <form:errors path="sku" cssClass="text-danger small" />
          </div>
          <div class="col-md-7">
            <label class="form-label">Name</label>
            <form:input path="name" cssClass="form-control" />
            <form:errors path="name" cssClass="text-danger small" />
          </div>
          <div class="col-md-5">
            <label class="form-label">Category</label>
            <form:input path="category" cssClass="form-control" />
            <form:errors path="category" cssClass="text-danger small" />
          </div>
          <div class="col-md-4">
            <label class="form-label">Price in cents</label>
            <form:input path="priceMinor" type="number" min="1" cssClass="form-control" />
            <form:errors path="priceMinor" cssClass="text-danger small" />
          </div>
          <div class="col-md-3">
            <label class="form-label">Stock</label>
            <form:input path="stockQuantity" type="number" min="0" cssClass="form-control" />
            <form:errors path="stockQuantity" cssClass="text-danger small" />
          </div>
          <div class="col-12">
            <label class="form-label">Description</label>
            <form:textarea path="description" rows="5" cssClass="form-control" />
            <form:errors path="description" cssClass="text-danger small" />
          </div>
          <div class="col-12">
            <label class="form-label">Image URL (optional)</label>
            <form:input path="imageUrl" cssClass="form-control" />
            <form:errors path="imageUrl" cssClass="text-danger small" />
          </div>
          <div class="col-12 form-check ms-2">
            <form:checkbox path="active" cssClass="form-check-input" />
            <label class="form-check-label">Visible in catalog</label>
          </div>
          <div class="col-12">
            <button class="btn btn-primary" type="submit">Save product</button>
          </div>
        </form:form>
      </div>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
