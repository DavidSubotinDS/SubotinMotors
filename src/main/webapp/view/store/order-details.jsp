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
      <a href="${pageContext.request.contextPath}/orders" class="text-decoration-none">&larr; My orders</a>
      <div class="d-flex flex-wrap justify-content-between align-items-center mt-3 mb-4">
        <h1 class="fw-bold mb-0">Order #${order.idOrder}</h1>
        <span class="order-status ${order.status eq 'PAID' ? 'order-status-paid' : (order.status eq 'PAYMENT_FAILED' || order.status eq 'EXPIRED' ? 'order-status-failed' : 'order-status-pending')}">${order.status}</span>
      </div>
      <div class="row g-4">
        <div class="col-lg-8">
          <div class="border rounded-4 p-4">
            <c:forEach items="${order.items}" var="item">
              <div class="d-flex justify-content-between border-bottom py-3">
                <div><strong>${item.partName}</strong><br /><span class="small text-secondary">${item.sku} &middot; Qty ${item.quantity}</span></div>
                <span><fmt:formatNumber value="${item.lineTotalMinor / 100.0}" minFractionDigits="2" maxFractionDigits="2" /> ${order.currency.toUpperCase()}</span>
              </div>
            </c:forEach>
            <div class="d-flex justify-content-between fs-5 pt-3"><strong>Total</strong><strong><fmt:formatNumber value="${order.totalMinor / 100.0}" minFractionDigits="2" maxFractionDigits="2" /> ${order.currency.toUpperCase()}</strong></div>
          </div>
        </div>
        <div class="col-lg-4">
          <div class="bg-light rounded-4 p-4">
            <h2 class="h5">Delivery details</h2>
            <p class="mb-1">${order.shippingName}</p>
            <p class="text-secondary">${order.shippingAddress}</p>
            <p class="small mb-1">Created: ${order.createdAt}</p>
            <c:if test="${order.paidAt != null}"><p class="small">Paid: ${order.paidAt}</p></c:if>
            <c:if test="${order.status eq 'CHECKOUT_CREATED' && not empty order.checkoutUrl}">
              <a class="btn btn-success w-100 mt-2" href="${order.checkoutUrl}">Return to Stripe Checkout</a>
            </c:if>
          </div>
        </div>
      </div>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
