<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/store.css" />
    <link rel="stylesheet" href="/css/discussion.css" />
  </head>
  <body>
    <%@ include file="../components/navbar.jsp" %>
    <main class="container py-5">
      <a class="text-decoration-none" href="${pageContext.request.contextPath}/parts">&larr; Back to parts</a>
      <div class="row g-4 mt-1 align-items-center">
        <div class="col-lg-6">
          <div class="part-visual rounded-4" style="height: 420px">
            <c:choose>
              <c:when test="${not empty part.imageUrl}"><img class="rounded-4" src="${part.imageUrl}" alt="${part.name}" /></c:when>
              <c:otherwise><i class="fa-solid fa-screwdriver-wrench" style="font-size: 8rem"></i></c:otherwise>
            </c:choose>
          </div>
        </div>
        <div class="col-lg-6">
          <span class="badge bg-primary">${part.category}</span>
          <h1 class="fw-bold mt-3">${part.name}</h1>
          <p class="text-secondary">SKU ${part.sku}</p>
          <p class="lead">${part.description}</p>
          <p class="store-price fs-2"><fmt:formatNumber value="${part.priceMinor / 100.0}" minFractionDigits="2" maxFractionDigits="2" /> EUR</p>
          <p class="${part.stockQuantity > 0 ? 'text-success' : 'text-danger'} fw-semibold">
            ${part.stockQuantity > 0 ? part.stockQuantity : 'No'} units available
          </p>
          <security:authorize access="isAuthenticated()">
            <c:if test="${part.stockQuantity > 0}">
              <form:form action="${pageContext.request.contextPath}/cart/items" method="POST" class="d-flex gap-2">
                <input type="hidden" name="idPart" value="${part.idPart}" />
                <input class="form-control" style="max-width: 110px" type="number" name="quantity" min="1" max="${part.stockQuantity}" value="1" />
                <button class="btn btn-primary flex-grow-1" type="submit"><i class="fa-solid fa-cart-plus me-2"></i>Add to cart</button>
              </form:form>
            </c:if>
          </security:authorize>
          <security:authorize access="!isAuthenticated()">
            <a class="btn btn-primary w-100" href="${pageContext.request.contextPath}/login">Log in to add to cart</a>
          </security:authorize>
        </div>
      </div>
      <c:set var="commentTitle" value="Product questions" />
      <c:set var="commentSubtitle" value="Ask about compatibility, installation, specifications, or delivery. Replies from the store team are highlighted." />
      <c:set var="commentPlaceholder" value="Ask a question about this car part..." />
      <c:set var="commentAction" value="${pageContext.request.contextPath}/parts/${part.idPart}/comments" />
      <%@ include file="../components/listing-comments.jsp" %>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
