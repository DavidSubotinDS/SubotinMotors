<%@ include file="components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="components/header.jsp" %>
    <link rel="stylesheet" href="/css/form.css" />
  </head>
  <body>
    <%@ include file="components/navbar.jsp" %>
    <main>
      <div class="container d-flex justify-content-center">
        <div class="form-wrapper small">
          <h2 class="form-header">Forgot Password</h2>
          <p class="text-secondary">Enter the email or username linked to your account.</p>
          <c:if test="${message != null}">
            <div class="alert alert-info">${message}</div>
          </c:if>
          <form:form action="${pageContext.request.contextPath}/forgot-password" method="POST" modelAttribute="forgotPassword">
            <label class="form-label" for="identifier">Email or username</label>
            <form:input id="identifier" class="form-control" path="identifier" />
            <form:errors path="identifier" cssClass="error" />
            <button class="btn btn-primary form-button mt-3" type="submit">Send reset link</button>
          </form:form>
          <div class="mt-3 text-center">
            <a href="<%= request.getContextPath() %>/login">Back to login</a>
          </div>
        </div>
      </div>
    </main>
    <%@ include file="components/footer.jsp" %>
  </body>
</html>
