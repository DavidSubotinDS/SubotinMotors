<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/admin.css" />
    <link rel="stylesheet" href="/css/store.css" />
  </head>
  <body>
    <%@ include file="../components/navbar.jsp" %>
    <main>
      <div class="container pt-5">
        <div class="d-flex">
          <aside class="sidebar-admin pe-md-3">
            <ul>
              <li><a href="${pageContext.request.contextPath}/admin"><i class="fa-solid fa-gauge-high"></i> Dashboard</a></li>
              <li><a href="${pageContext.request.contextPath}/admin/car-management"><i class="fa-solid fa-car"></i> Car Management</a></li>
              <li class="active-page"><a href="${pageContext.request.contextPath}/admin/store/parts"><i class="fa-solid fa-gears"></i> Parts Inventory</a></li>
              <li><a href="${pageContext.request.contextPath}/admin/store/orders"><i class="fa-solid fa-box"></i> Store Orders</a></li>
              <li><a href="${pageContext.request.contextPath}/admin/transactions"><i class="fa-solid fa-clock-rotate-left"></i> Legacy Transactions</a></li>
            </ul>
          </aside>
          <div class="content-wrapper">
            <div class="d-flex flex-wrap justify-content-between align-items-center mb-3">
              <h2 class="fw-bold mb-0">Parts inventory</h2>
              <a class="btn btn-primary" href="${pageContext.request.contextPath}/admin/store/parts/new"><i class="fa-solid fa-plus me-2"></i>Add product</a>
            </div>
            <c:if test="${not empty storeMessage}"><div class="alert alert-success">${storeMessage}</div></c:if>
            <div class="table-responsive">
              <table class="table table-striped align-middle">
                <thead><tr><th>SKU</th><th>Product</th><th>Category</th><th>Price</th><th>Stock</th><th>Status</th><th></th></tr></thead>
                <tbody>
                  <c:forEach items="${parts}" var="part">
                    <tr>
                      <td><code>${part.sku}</code></td>
                      <td>${part.name}</td>
                      <td>${part.category}</td>
                      <td><fmt:formatNumber value="${part.priceMinor / 100.0}" minFractionDigits="2" maxFractionDigits="2" /> EUR</td>
                      <td>${part.stockQuantity}</td>
                      <td><span class="badge ${part.active ? 'bg-success' : 'bg-secondary'}">${part.active ? 'Active' : 'Hidden'}</span></td>
                      <td class="text-end">
                        <a class="btn btn-sm btn-outline-primary" href="${pageContext.request.contextPath}/admin/store/parts/${part.idPart}/edit">Edit</a>
                        <form:form class="d-inline" action="${pageContext.request.contextPath}/admin/store/parts/${part.idPart}/active" method="POST">
                          <input type="hidden" name="active" value="${!part.active}" />
                          <button class="btn btn-sm ${part.active ? 'btn-outline-danger' : 'btn-outline-success'}" type="submit">${part.active ? 'Hide' : 'Activate'}</button>
                        </form:form>
                      </td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
