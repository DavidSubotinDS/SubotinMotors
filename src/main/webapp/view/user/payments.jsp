<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/user.css" />
  </head>
  <body>
    <%@ include file="../components/navbar.jsp" %>
    <main>
      <div class="container py-5">
        <c:if test="${paymentMessage != null}">
          <div class="alert alert-success">${paymentMessage}</div>
        </c:if>
        <c:if test="${paymentWarning != null}">
          <div class="alert alert-warning">${paymentWarning}</div>
        </c:if>

        <div class="card mb-5">
          <div class="card-body d-flex justify-content-between align-items-center">
            <div>
              <h4 class="mb-1">Seller payout account</h4>
              <c:choose>
                <c:when test="${!stripeEnabled}">
                  <span class="text-secondary">Stripe Connect is disabled in this environment.</span>
                </c:when>
                <c:when test="${paymentAccount != null && paymentAccount.transfersEnabled}">
                  <span class="text-success fw-semibold">Ready to receive marketplace payouts</span>
                </c:when>
                <c:when test="${paymentAccount != null}">
                  <span class="text-warning fw-semibold">Onboarding status: ${paymentAccount.status}</span>
                </c:when>
                <c:otherwise>
                  <span class="text-secondary">Not connected</span>
                </c:otherwise>
              </c:choose>
            </div>
            <c:if test="${stripeEnabled && (paymentAccount == null || !paymentAccount.transfersEnabled)}">
              <a class="btn btn-primary" href="${pageContext.request.contextPath}/payments/seller/onboarding">
                ${paymentAccount == null ? 'Connect Stripe' : 'Continue onboarding'}
              </a>
            </c:if>
          </div>
        </div>

        <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-3">
          <h3 class="mb-0">Purchases</h3>
          <form action="${pageContext.request.contextPath}/user/payments" class="d-flex gap-2">
            <input type="hidden" name="salePage" value="${salePage.number}" />
            <input type="hidden" name="saleSort" value="${saleSort}" />
            <input type="hidden" name="saleDirection" value="${saleDirection}" />
            <select class="form-select" name="purchaseSort" aria-label="Sort purchases">
              <option value="createdAt" ${purchaseSort eq 'createdAt' ? 'selected' : ''}>Date</option>
              <option value="bid.car.make" ${purchaseSort eq 'bid.car.make' ? 'selected' : ''}>Car make</option>
              <option value="amountMinor" ${purchaseSort eq 'amountMinor' ? 'selected' : ''}>Amount</option>
              <option value="status" ${purchaseSort eq 'status' ? 'selected' : ''}>Status</option>
            </select>
            <select class="form-select" name="purchaseDirection" aria-label="Purchase sort direction">
              <option value="asc" ${purchaseDirection eq 'asc' ? 'selected' : ''}>Ascending</option>
              <option value="desc" ${purchaseDirection eq 'desc' ? 'selected' : ''}>Descending</option>
            </select>
            <button class="btn btn-outline-secondary" type="submit">Sort</button>
          </form>
        </div>
        <div class="table-responsive mb-5">
          <table class="table table-striped">
            <thead>
              <tr><th>Car</th><th>Amount</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              <c:forEach items="${purchases}" var="payment">
                <tr>
                  <td>${payment.bid.car.make} ${payment.bid.car.model} (${payment.bid.car.year})</td>
                  <td>${payment.bid.bidPrice} ${payment.currency.toUpperCase()}</td>
                  <td>${payment.status}</td>
                  <td>
                    <c:if test="${stripeEnabled && payment.status.equals('PENDING_CHECKOUT')}">
                      <form:form method="POST" action="${pageContext.request.contextPath}/payments/${payment.idPayment}/checkout">
                        <button class="btn btn-success" type="submit">Pay securely</button>
                      </form:form>
                    </c:if>
                    <c:if test="${stripeEnabled && payment.status.equals('CHECKOUT_CREATED')}">
                      <form:form method="POST" action="${pageContext.request.contextPath}/payments/${payment.idPayment}/checkout">
                        <button class="btn btn-success" type="submit">Return to checkout</button>
                      </form:form>
                    </c:if>
                  </td>
                </tr>
              </c:forEach>
              <c:if test="${empty purchases}">
                <tr><td colspan="4" class="text-secondary">No accepted bids awaiting payment.</td></tr>
              </c:if>
            </tbody>
          </table>
        </div>
        <c:if test="${purchasePage.totalPages > 1}">
          <nav aria-label="Purchase pages" class="mb-5">
            <ul class="pagination justify-content-end">
              <c:url var="previousPurchasesUrl" value="/user/payments">
                <c:param name="purchasePage" value="${purchasePage.number - 1}" />
                <c:param name="purchaseSort" value="${purchaseSort}" />
                <c:param name="purchaseDirection" value="${purchaseDirection}" />
                <c:param name="salePage" value="${salePage.number}" />
                <c:param name="saleSort" value="${saleSort}" />
                <c:param name="saleDirection" value="${saleDirection}" />
              </c:url>
              <li class="page-item ${purchasePage.first ? 'disabled' : ''}">
                <a class="page-link" href="${purchasePage.first ? '#' : previousPurchasesUrl}">Previous</a>
              </li>
              <li class="page-item disabled"><span class="page-link">${purchasePage.number + 1} / ${purchasePage.totalPages}</span></li>
              <c:url var="nextPurchasesUrl" value="/user/payments">
                <c:param name="purchasePage" value="${purchasePage.number + 1}" />
                <c:param name="purchaseSort" value="${purchaseSort}" />
                <c:param name="purchaseDirection" value="${purchaseDirection}" />
                <c:param name="salePage" value="${salePage.number}" />
                <c:param name="saleSort" value="${saleSort}" />
                <c:param name="saleDirection" value="${saleDirection}" />
              </c:url>
              <li class="page-item ${purchasePage.last ? 'disabled' : ''}">
                <a class="page-link" href="${purchasePage.last ? '#' : nextPurchasesUrl}">Next</a>
              </li>
            </ul>
          </nav>
        </c:if>

        <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-3">
          <h3 class="mb-0">Sales</h3>
          <form action="${pageContext.request.contextPath}/user/payments" class="d-flex gap-2">
            <input type="hidden" name="purchasePage" value="${purchasePage.number}" />
            <input type="hidden" name="purchaseSort" value="${purchaseSort}" />
            <input type="hidden" name="purchaseDirection" value="${purchaseDirection}" />
            <select class="form-select" name="saleSort" aria-label="Sort sales">
              <option value="createdAt" ${saleSort eq 'createdAt' ? 'selected' : ''}>Date</option>
              <option value="bid.car.make" ${saleSort eq 'bid.car.make' ? 'selected' : ''}>Car make</option>
              <option value="amountMinor" ${saleSort eq 'amountMinor' ? 'selected' : ''}>Amount</option>
              <option value="status" ${saleSort eq 'status' ? 'selected' : ''}>Status</option>
            </select>
            <select class="form-select" name="saleDirection" aria-label="Sale sort direction">
              <option value="asc" ${saleDirection eq 'asc' ? 'selected' : ''}>Ascending</option>
              <option value="desc" ${saleDirection eq 'desc' ? 'selected' : ''}>Descending</option>
            </select>
            <button class="btn btn-outline-secondary" type="submit">Sort</button>
          </form>
        </div>
        <div class="table-responsive">
          <table class="table table-striped">
            <thead>
              <tr><th>Car</th><th>Buyer</th><th>Amount</th><th>Status</th></tr>
            </thead>
            <tbody>
              <c:forEach items="${sales}" var="payment">
                <tr>
                  <td>${payment.bid.car.make} ${payment.bid.car.model} (${payment.bid.car.year})</td>
                  <td>${payment.buyer.username}</td>
                  <td>${payment.bid.bidPrice} ${payment.currency.toUpperCase()}</td>
                  <td>${payment.status}</td>
                </tr>
              </c:forEach>
              <c:if test="${empty sales}">
                <tr><td colspan="4" class="text-secondary">No payment-backed sales yet.</td></tr>
              </c:if>
            </tbody>
          </table>
        </div>
        <c:if test="${salePage.totalPages > 1}">
          <nav aria-label="Sale pages" class="mt-3">
            <ul class="pagination justify-content-end">
              <c:url var="previousSalesUrl" value="/user/payments">
                <c:param name="salePage" value="${salePage.number - 1}" />
                <c:param name="saleSort" value="${saleSort}" />
                <c:param name="saleDirection" value="${saleDirection}" />
                <c:param name="purchasePage" value="${purchasePage.number}" />
                <c:param name="purchaseSort" value="${purchaseSort}" />
                <c:param name="purchaseDirection" value="${purchaseDirection}" />
              </c:url>
              <li class="page-item ${salePage.first ? 'disabled' : ''}">
                <a class="page-link" href="${salePage.first ? '#' : previousSalesUrl}">Previous</a>
              </li>
              <li class="page-item disabled"><span class="page-link">${salePage.number + 1} / ${salePage.totalPages}</span></li>
              <c:url var="nextSalesUrl" value="/user/payments">
                <c:param name="salePage" value="${salePage.number + 1}" />
                <c:param name="saleSort" value="${saleSort}" />
                <c:param name="saleDirection" value="${saleDirection}" />
                <c:param name="purchasePage" value="${purchasePage.number}" />
                <c:param name="purchaseSort" value="${purchaseSort}" />
                <c:param name="purchaseDirection" value="${purchaseDirection}" />
              </c:url>
              <li class="page-item ${salePage.last ? 'disabled' : ''}">
                <a class="page-link" href="${salePage.last ? '#' : nextSalesUrl}">Next</a>
              </li>
            </ul>
          </nav>
        </c:if>
      </div>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
