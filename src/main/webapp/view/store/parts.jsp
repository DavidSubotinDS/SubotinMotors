<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/store.css" />
  </head>
  <body>
    <%@ include file="../components/navbar.jsp" %>
    <main class="container py-5">
      <c:if test="${not empty storeMessage}"><div class="alert alert-info">${storeMessage}</div></c:if>
      <section class="store-hero p-4 p-md-5 mb-4">
        <div class="row align-items-center">
          <div class="col-lg-8">
            <span class="text-uppercase fw-semibold">Subotin Parts</span>
            <h1 class="display-5 fw-bold mt-2">Everything your car needs</h1>
            <p class="lead mb-0">Shop maintenance, safety and performance parts through secure sandbox checkout.</p>
          </div>
          <div class="col-lg-4 text-center d-none d-lg-block">
            <i class="fa-solid fa-gears" style="font-size: 7rem; opacity: .75"></i>
          </div>
        </div>
      </section>

      <form action="${pageContext.request.contextPath}/parts" class="row g-2 mb-4">
        <div class="col-md-5">
          <input class="form-control" name="keyword" value="${keyword}" placeholder="Search products or SKU" />
        </div>
        <div class="col-md-3">
          <select class="form-select" name="category">
            <option value="">All categories</option>
            <c:forEach items="${categories}" var="itemCategory">
              <option value="${itemCategory}" ${category eq itemCategory ? 'selected' : ''}>${itemCategory}</option>
            </c:forEach>
          </select>
        </div>
        <div class="col-md-2">
          <select class="form-select" name="sort">
            <option value="name" ${sort eq 'name' ? 'selected' : ''}>Name</option>
            <option value="priceMinor" ${sort eq 'priceMinor' ? 'selected' : ''}>Price</option>
            <option value="category" ${sort eq 'category' ? 'selected' : ''}>Category</option>
          </select>
        </div>
        <div class="col-md-1">
          <select class="form-select" name="direction">
            <option value="asc" ${direction eq 'asc' ? 'selected' : ''}>Asc</option>
            <option value="desc" ${direction eq 'desc' ? 'selected' : ''}>Desc</option>
          </select>
        </div>
        <div class="col-md-1 d-grid">
          <button class="btn btn-primary" type="submit"><i class="fa-solid fa-magnifying-glass"></i></button>
        </div>
      </form>

      <div class="row g-4">
        <c:forEach items="${parts}" var="part">
          <div class="col-sm-6 col-lg-3">
            <article class="part-card bg-white">
              <div class="part-visual">
                <c:choose>
                  <c:when test="${not empty part.imageUrl}">
                    <img src="${part.imageUrl}" alt="${part.name}" />
                  </c:when>
                  <c:otherwise><i class="fa-solid fa-screwdriver-wrench"></i></c:otherwise>
                </c:choose>
              </div>
              <div class="p-3">
                <span class="badge bg-light text-dark">${part.category}</span>
                <h2 class="h5 mt-2">${part.name}</h2>
                <p class="small text-secondary mb-2">SKU ${part.sku}</p>
                <div class="d-flex justify-content-between align-items-center">
                  <span class="store-price"><fmt:formatNumber value="${part.priceMinor / 100.0}" minFractionDigits="2" maxFractionDigits="2" /> EUR</span>
                  <span class="small ${part.stockQuantity > 0 ? 'text-success' : 'text-danger'}">
                    ${part.stockQuantity > 0 ? 'In stock' : 'Sold out'}
                  </span>
                </div>
                <a class="btn btn-outline-primary w-100 mt-3" href="${pageContext.request.contextPath}/parts/${part.idPart}">View product</a>
              </div>
            </article>
          </div>
        </c:forEach>
      </div>

      <c:if test="${empty parts}">
        <div class="alert alert-light border text-center py-5">No products match your search.</div>
      </c:if>

      <c:if test="${partPage.totalPages > 1}">
        <nav class="mt-4" aria-label="Product pages">
          <ul class="pagination justify-content-center">
            <c:forEach begin="0" end="${partPage.totalPages - 1}" var="pageNumber">
              <c:url var="pageUrl" value="/parts">
                <c:param name="page" value="${pageNumber}" />
                <c:param name="keyword" value="${keyword}" />
                <c:param name="category" value="${category}" />
                <c:param name="sort" value="${sort}" />
                <c:param name="direction" value="${direction}" />
              </c:url>
              <li class="page-item ${partPage.number eq pageNumber ? 'active' : ''}">
                <a class="page-link" href="${pageUrl}">${pageNumber + 1}</a>
              </li>
            </c:forEach>
          </ul>
        </nav>
      </c:if>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
