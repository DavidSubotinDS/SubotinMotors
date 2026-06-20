<%@ include file="../components/taglib.jsp" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/admin.css" />
  </head>
  <body>
    <%@ include file="../components/navbar.jsp" %>

    <main>
      <div class="container pt-5">
        <div class="d-flex">
          <aside class="sidebar-admin pe-md-3">
            <ul>
              <li>
                <a href="<%= request.getContextPath() %>/admin"><i class="fa-solid fa-gauge-high"></i> Dashboard</a>
              </li>
              <li>
                <a href="<%= request.getContextPath() %>/admin/car-management"><i class="fa-solid fa-car"></i> Car Management</a>
              </li>
              <li class="active-page">
                <a href="<%= request.getContextPath() %>/admin/transactions"><i class="fa-solid fa-receipt"></i> Transactions</a>
              </li>
            </ul>
          </aside>

          <div class="content-wrapper">
            <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-3">
              <h2 class="fw-bold mb-0 flex-grow-1">Transactions</h2>
              <form action="${pageContext.request.contextPath}/admin/transactions" class="d-flex gap-2">
                <select class="form-select" name="sort" aria-label="Sort transactions">
                  <option value="paidAt" ${sort eq 'paidAt' ? 'selected' : ''}>Payment date</option>
                  <option value="createdAt" ${sort eq 'createdAt' ? 'selected' : ''}>Created date</option>
                  <option value="bid.car.make" ${sort eq 'bid.car.make' ? 'selected' : ''}>Car make</option>
                  <option value="buyer.username" ${sort eq 'buyer.username' ? 'selected' : ''}>Buyer</option>
                  <option value="seller.username" ${sort eq 'seller.username' ? 'selected' : ''}>Seller</option>
                  <option value="amountMinor" ${sort eq 'amountMinor' ? 'selected' : ''}>Amount</option>
                  <option value="status" ${sort eq 'status' ? 'selected' : ''}>Status</option>
                </select>
                <select class="form-select" name="direction" aria-label="Transaction sort direction">
                  <option value="asc" ${direction eq 'asc' ? 'selected' : ''}>Ascending</option>
                  <option value="desc" ${direction eq 'desc' ? 'selected' : ''}>Descending</option>
                </select>
                <button class="btn btn-outline-secondary" type="submit">Sort</button>
              </form>
            </div>

            <div class="table-responsive">
              <table class="table table-striped align-middle">
                <thead>
                  <tr>
                    <th>Car purchased</th>
                    <th>Buyer</th>
                    <th>Seller</th>
                    <th>Amount</th>
                    <th>Quantity</th>
                    <th>Status</th>
                    <th>Stripe identifier</th>
                    <th>Payment date</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach items="${transactions}" var="payment">
                    <tr>
                      <td>
                        <span class="fw-semibold">${payment.bid.car.make} ${payment.bid.car.model}</span>
                        <br />
                        <small class="text-secondary">${payment.bid.car.year} · Car #${payment.bid.car.idCar}</small>
                      </td>
                      <td>
                        ${payment.buyer.profile.firstName} ${payment.buyer.profile.lastName}
                        <br />
                        <small class="text-secondary">${payment.buyer.username}</small>
                      </td>
                      <td>
                        ${payment.seller.profile.firstName} ${payment.seller.profile.lastName}
                        <br />
                        <small class="text-secondary">${payment.seller.username}</small>
                      </td>
                      <td class="text-nowrap">
                        <fmt:formatNumber value="${payment.amountMinor / 100.0}" minFractionDigits="2" maxFractionDigits="2" />
                        ${payment.currency.toUpperCase()}
                      </td>
                      <td>1</td>
                      <td>
                        <c:choose>
                          <c:when test="${payment.status eq 'PAID'}">
                            <span class="badge bg-success">${payment.status}</span>
                          </c:when>
                          <c:when test="${payment.status eq 'PAYMENT_FAILED' || payment.status eq 'EXPIRED'}">
                            <span class="badge bg-danger">${payment.status}</span>
                          </c:when>
                          <c:otherwise>
                            <span class="badge bg-warning text-dark">${payment.status}</span>
                          </c:otherwise>
                        </c:choose>
                      </td>
                      <td>
                        <c:if test="${not empty payment.paymentIntentId}">
                          <small class="text-secondary">Payment</small><br />
                          <code>${payment.paymentIntentId}</code>
                        </c:if>
                        <c:if test="${not empty payment.checkoutSessionId}">
                          <c:if test="${not empty payment.paymentIntentId}"><br /></c:if>
                          <small class="text-secondary">Session</small><br />
                          <code>${payment.checkoutSessionId}</code>
                        </c:if>
                        <c:if test="${empty payment.paymentIntentId && empty payment.checkoutSessionId}">
                          <span class="text-secondary">Not created</span>
                        </c:if>
                      </td>
                      <td class="text-nowrap">
                        <c:choose>
                          <c:when test="${payment.paidAt != null}">${payment.paidAt}</c:when>
                          <c:otherwise><span class="text-secondary">Not paid</span></c:otherwise>
                        </c:choose>
                      </td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty transactions}">
                    <tr>
                      <td colspan="8" class="text-center text-secondary py-4">No transactions recorded yet.</td>
                    </tr>
                  </c:if>
                </tbody>
              </table>
            </div>

            <c:if test="${transactionPage.totalPages > 1}">
              <nav aria-label="Transaction pages">
                <ul class="pagination justify-content-end">
                  <c:url var="previousTransactionsUrl" value="/admin/transactions">
                    <c:param name="page" value="${transactionPage.number - 1}" />
                    <c:param name="sort" value="${sort}" />
                    <c:param name="direction" value="${direction}" />
                  </c:url>
                  <li class="page-item ${transactionPage.first ? 'disabled' : ''}">
                    <a class="page-link" href="${transactionPage.first ? '#' : previousTransactionsUrl}">Previous</a>
                  </li>
                  <li class="page-item disabled">
                    <span class="page-link">${transactionPage.number + 1} / ${transactionPage.totalPages}</span>
                  </li>
                  <c:url var="nextTransactionsUrl" value="/admin/transactions">
                    <c:param name="page" value="${transactionPage.number + 1}" />
                    <c:param name="sort" value="${sort}" />
                    <c:param name="direction" value="${direction}" />
                  </c:url>
                  <li class="page-item ${transactionPage.last ? 'disabled' : ''}">
                    <a class="page-link" href="${transactionPage.last ? '#' : nextTransactionsUrl}">Next</a>
                  </li>
                </ul>
              </nav>
            </c:if>
          </div>
        </div>
      </div>
    </main>

    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
