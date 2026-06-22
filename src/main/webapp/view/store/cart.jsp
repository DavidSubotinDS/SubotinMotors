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
      <h1 class="fw-bold mb-4">Shopping cart</h1>
      <c:if test="${not empty storeMessage}"><div class="alert alert-success">${storeMessage}</div></c:if>
      <c:if test="${param.checkoutCanceled != null}"><div class="alert alert-warning">Checkout was cancelled. Your reserved order remains visible in order history.</div></c:if>

      <c:choose>
        <c:when test="${empty cartItems}">
          <div class="text-center border rounded-4 p-5">
            <i class="fa-solid fa-cart-shopping text-secondary mb-3" style="font-size: 4rem"></i>
            <h2 class="h4">Your cart is empty</h2>
            <a class="btn btn-primary mt-2" href="${pageContext.request.contextPath}/parts">Browse car parts</a>
          </div>
        </c:when>
        <c:otherwise>
          <div class="row g-4">
            <div class="col-lg-8">
              <c:forEach items="${cartItems}" var="item">
                <div class="border rounded-4 p-3 mb-3">
                  <div class="row align-items-center g-3">
                    <div class="col-md-5">
                      <h2 class="h5 mb-1">${item.part.name}</h2>
                      <span class="small text-secondary">${item.part.sku} &middot; ${item.part.category}</span>
                    </div>
                    <div class="col-md-3">
                      <form:form action="${pageContext.request.contextPath}/cart/items/${item.idCartItem}" method="POST" class="d-flex gap-2">
                        <input class="form-control" type="number" name="quantity" min="0" max="${item.part.stockQuantity}" value="${item.quantity}" />
                        <button class="btn btn-outline-secondary" type="submit">Update</button>
                      </form:form>
                    </div>
                    <div class="col-md-2 fw-bold">
                      <fmt:formatNumber value="${item.lineTotalMinor / 100.0}" minFractionDigits="2" maxFractionDigits="2" /> EUR
                    </div>
                    <div class="col-md-2 text-end">
                      <form:form action="${pageContext.request.contextPath}/cart/items/${item.idCartItem}/remove" method="POST">
                        <button class="btn btn-outline-danger" type="submit"><i class="fa-solid fa-trash"></i></button>
                      </form:form>
                    </div>
                  </div>
                </div>
              </c:forEach>
            </div>
            <div class="col-lg-4">
              <div class="store-summary p-4">
                <h2 class="h4">Order summary</h2>
                <div class="d-flex justify-content-between border-top border-bottom py-3 my-3">
                  <span>Total</span>
                  <strong><fmt:formatNumber value="${cartTotalMinor / 100.0}" minFractionDigits="2" maxFractionDigits="2" /> EUR</strong>
                </div>
                <p class="small text-secondary">Delivery uses the address saved in your profile. No real payment is processed in sandbox mode.</p>
                <c:choose>
                  <c:when test="${stripeEnabled}">
                    <form:form action="${pageContext.request.contextPath}/store/checkout" method="POST">
                      <button class="btn btn-success w-100" type="submit"><i class="fa-brands fa-stripe me-2"></i>Secure checkout</button>
                    </form:form>
                  </c:when>
                  <c:otherwise>
                    <div class="alert alert-warning small mb-0">Start the application with the Stripe sandbox helper to enable checkout.</div>
                  </c:otherwise>
                </c:choose>
              </div>
            </div>
          </div>
        </c:otherwise>
      </c:choose>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
