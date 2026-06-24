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
              <li><a href="${pageContext.request.contextPath}/admin/store/parts"><i class="fa-solid fa-gears"></i> Parts Inventory</a></li>
              <li class="active-page"><a href="${pageContext.request.contextPath}/admin/store/orders"><i class="fa-solid fa-box"></i> Store Orders</a></li>
              <li><a href="${pageContext.request.contextPath}/admin/transactions"><i class="fa-solid fa-clock-rotate-left"></i> Legacy Transactions</a></li>
            </ul>
          </aside>
          <div class="content-wrapper">
            <a href="${pageContext.request.contextPath}/admin/store/orders" class="text-decoration-none">&larr; Store orders</a>
            <div class="d-flex flex-wrap justify-content-between align-items-center mt-3 mb-4 gap-3">
              <h2 class="fw-bold mb-0">Store order #${order.idOrder}</h2>
              <span class="order-status ${order.status eq 'PAID' ? 'order-status-paid' : (order.status eq 'PAYMENT_FAILED' || order.status eq 'EXPIRED' ? 'order-status-failed' : 'order-status-pending')}">${order.status}</span>
            </div>

            <div class="row g-4">
              <div class="col-lg-8">
                <div class="table-responsive">
                  <table class="table table-striped align-middle">
                    <thead>
                      <tr>
                        <th>Product</th>
                        <th>Identifier</th>
                        <th class="text-end">Unit price</th>
                        <th class="text-end">Quantity</th>
                        <th class="text-end">Subtotal</th>
                      </tr>
                    </thead>
                    <tbody>
                      <c:forEach items="${order.items}" var="item">
                        <tr>
                          <td>
                            <span class="fw-semibold"><c:out value="${item.partName}" /></span>
                            <c:if test="${not empty item.part}">
                              <br />
                              <small class="text-secondary">Product #${item.part.idPart}</small>
                            </c:if>
                          </td>
                          <td><code><c:out value="${item.sku}" /></code></td>
                          <td class="text-end text-nowrap">
                            <fmt:formatNumber value="${item.unitPriceMinor / 100.0}" minFractionDigits="2" maxFractionDigits="2" />
                            ${order.currency.toUpperCase()}
                          </td>
                          <td class="text-end">${item.quantity}</td>
                          <td class="text-end text-nowrap">
                            <fmt:formatNumber value="${item.lineTotalMinor / 100.0}" minFractionDigits="2" maxFractionDigits="2" />
                            ${order.currency.toUpperCase()}
                          </td>
                        </tr>
                      </c:forEach>
                      <c:if test="${empty order.items}">
                        <tr>
                          <td colspan="5" class="text-center text-secondary py-4">No order items recorded.</td>
                        </tr>
                      </c:if>
                    </tbody>
                    <tfoot>
                      <tr>
                        <th colspan="4" class="text-end">Order total</th>
                        <th class="text-end text-nowrap">
                          <fmt:formatNumber value="${order.totalMinor / 100.0}" minFractionDigits="2" maxFractionDigits="2" />
                          ${order.currency.toUpperCase()}
                        </th>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              </div>

              <div class="col-lg-4">
                <div class="bg-light rounded-4 p-4 mb-4">
                  <h3 class="h5">Customer</h3>
                  <p class="mb-1">
                    <strong><c:out value="${order.user.profile.firstName}" /> <c:out value="${order.user.profile.lastName}" /></strong>
                  </p>
                  <p class="text-secondary mb-1"><c:out value="${order.user.username}" /></p>
                  <p class="text-secondary mb-0"><c:out value="${order.user.email}" /></p>
                </div>

                <div class="bg-light rounded-4 p-4 mb-4">
                  <h3 class="h5">Order timeline</h3>
                  <p class="small mb-1">Created: ${order.createdAt}</p>
                  <p class="small mb-1">Updated: ${order.updatedAt}</p>
                  <c:choose>
                    <c:when test="${order.paidAt != null}">
                      <p class="small mb-0">Paid: ${order.paidAt}</p>
                    </c:when>
                    <c:otherwise>
                      <p class="small text-secondary mb-0">Paid: Not paid</p>
                    </c:otherwise>
                  </c:choose>
                </div>

                <div class="bg-light rounded-4 p-4 mb-4">
                  <h3 class="h5">Stripe references</h3>
                  <c:choose>
                    <c:when test="${not empty order.paymentIntentId}">
                      <small class="text-secondary">Payment intent</small>
                      <p><code><c:out value="${order.paymentIntentId}" /></code></p>
                    </c:when>
                    <c:otherwise>
                      <p class="text-secondary mb-3">Payment intent not recorded.</p>
                    </c:otherwise>
                  </c:choose>
                  <c:choose>
                    <c:when test="${not empty order.checkoutSessionId}">
                      <small class="text-secondary">Checkout session</small>
                      <p class="mb-0"><code><c:out value="${order.checkoutSessionId}" /></code></p>
                    </c:when>
                    <c:otherwise>
                      <p class="text-secondary mb-0">Checkout session not recorded.</p>
                    </c:otherwise>
                  </c:choose>
                </div>

                <div class="bg-light rounded-4 p-4">
                  <h3 class="h5">Delivery snapshot</h3>
                  <p class="mb-1"><c:out value="${order.shippingName}" /></p>
                  <p class="text-secondary mb-0"><c:out value="${order.shippingAddress}" /></p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
