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
      <div class="mx-auto text-center border rounded-4 p-5" style="max-width: 720px">
        <i class="fa-solid fa-circle-check text-success mb-3" style="font-size: 5rem"></i>
        <h1 class="fw-bold">Checkout submitted</h1>
        <p class="lead">Stripe is confirming order #${order.idOrder}. The signed webhook—not the browser redirect—marks it paid.</p>
        <p>Current status: <strong>${order.status}</strong></p>
        <a class="btn btn-primary" href="${pageContext.request.contextPath}/orders/${order.idOrder}">View order</a>
      </div>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
