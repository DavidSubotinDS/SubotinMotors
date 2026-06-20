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

        <h3>Purchases</h3>
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

        <h3>Sales</h3>
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
      </div>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
