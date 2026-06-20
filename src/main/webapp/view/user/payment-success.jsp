<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
  </head>
  <body>
    <%@ include file="../components/navbar.jsp" %>
    <main>
      <div class="container py-5 text-center">
        <h1>Payment submitted</h1>
        <p class="text-secondary">
          Stripe is confirming the payment. The sale is completed only after the signed webhook is received.
        </p>
        <c:if test="${payment != null}">
          <p>Current status: <strong>${payment.status}</strong></p>
        </c:if>
        <a class="btn btn-primary" href="${pageContext.request.contextPath}/user/payments">View payments</a>
      </div>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
