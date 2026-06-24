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
          <h2 class="form-header">Reset Password</h2>
          <c:if test="${message != null}">
            <div class="alert alert-danger">${message}</div>
          </c:if>
          <c:choose>
            <c:when test="${tokenValid}">
              <form:form action="${pageContext.request.contextPath}/reset-password" method="POST" modelAttribute="resetPassword">
                <form:hidden path="token" />
                <label class="form-label">New password</label>
                <form:password class="form-control" path="password" />
                <form:errors path="password" cssClass="error" />
                <label class="form-label">Confirm password</label>
                <form:password class="form-control" path="confirmPassword" />
                <form:errors path="confirmPassword" cssClass="error" />
                <button class="btn btn-primary form-button mt-3" type="submit">Reset password</button>
              </form:form>
            </c:when>
            <c:otherwise>
              <p class="text-secondary">This reset link is invalid, expired, or has already been used.</p>
              <a class="btn btn-primary" href="<%= request.getContextPath() %>/forgot-password">Request a new link</a>
            </c:otherwise>
          </c:choose>
        </div>
      </div>
    </main>
    <%@ include file="components/footer.jsp" %>
  </body>
</html>
