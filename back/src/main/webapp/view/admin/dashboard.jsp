<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/admin.css" />
  </head>
  <body>
    <!-- Navbar -->
    <%@ include file="../components/navbar.jsp" %>

    <!-- Main -->
    <main>
      <div class="container pt-5">
        <div class="d-flex">
          <!-- Sidebar -->
          <aside class="sidebar-admin pe-md-3">
            <ul>
              <li class="active-page">
                <a href="<%= request.getContextPath() %>/admin"><i class="fa-solid fa-gauge-high"></i> Dashboard</a>
              </li>
              <li>
                <a href="<%= request.getContextPath() %>/admin/car-management"><i class="fa-solid fa-car"></i> Car Management</a>
              </li>
              <li>
                <a href="<%= request.getContextPath() %>/admin/store/parts"><i class="fa-solid fa-gears"></i> Parts Inventory</a>
              </li>
              <li>
                <a href="<%= request.getContextPath() %>/admin/store/orders"><i class="fa-solid fa-box"></i> Store Orders</a>
              </li>
              <li>
                <a href="<%= request.getContextPath() %>/admin/transactions"><i class="fa-solid fa-clock-rotate-left"></i> Legacy Transactions</a>
              </li>
            </ul>
          </aside>

          <!-- Content -->
          <div class="content-wrapper">
            <!-- List User -->
            <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-3">
              <h2 class="fw-bold mb-0 flex-grow-1">User</h2>
              <form action="${pageContext.request.contextPath}/admin/dashboard" class="d-flex gap-2">
                <input type="hidden" name="adminPage" value="${adminPage.number}" />
                <input type="hidden" name="adminSort" value="${adminSort}" />
                <input type="hidden" name="adminDirection" value="${adminDirection}" />
                <select class="form-select" name="userSort" aria-label="Sort users">
                  <option value="idUser" ${userSort eq 'idUser' ? 'selected' : ''}>ID</option>
                  <option value="username" ${userSort eq 'username' ? 'selected' : ''}>Username</option>
                  <option value="email" ${userSort eq 'email' ? 'selected' : ''}>Email</option>
                  <option value="profile.firstName" ${userSort eq 'profile.firstName' ? 'selected' : ''}>First name</option>
                  <option value="profile.lastName" ${userSort eq 'profile.lastName' ? 'selected' : ''}>Last name</option>
                </select>
                <select class="form-select" name="userDirection" aria-label="User sort direction">
                  <option value="asc" ${userDirection eq 'asc' ? 'selected' : ''}>Ascending</option>
                  <option value="desc" ${userDirection eq 'desc' ? 'selected' : ''}>Descending</option>
                </select>
                <button class="btn btn-outline-secondary" type="submit">Sort</button>
              </form>
            </div>
            <div class="table-responsive-md">
              <table class="table table-striped">
                <!-- Head -->
                <thead>
                  <tr>
                    <th>Id</th>
                    <th>First name</th>
                    <th>Last name</th>
                    <th>Email</th>
                    <th>Phone number</th>
                    <th>Address</th>
                    <th></th>
                  </tr>
                </thead>
                <!-- Body -->
                <tbody>
                  <c:forEach items="${listUser}" var="user">
                    <tr>
                      <td>${user.idUser}</td>
                      <td>${user.profile.firstName}</td>
                      <td>${user.profile.lastName}</td>
                      <td>${user.email}</td>
                      <td>${user.profile.phoneNumber}</td>
                      <td>${user.profile.formattedShippingAddress}</td>
                      <td>
                        <div class="dropdown">
                          <button class="btn btn-warning dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false"><i class="fa-regular fa-pen-to-square"></i></button>
                          <ul class="dropdown-menu dropdown-menu-dark">
                            <li>
                              <a class="dropdown-item" href="<%= request.getContextPath() %>/admin/edit-user?id=${user.profile.idProfile}">Edit Profile</a>
                            </li>
                            <li>
                              <form:form action="${pageContext.request.contextPath}/admin/mark-admin/${user.idUser}" method="POST">
                                <button class="dropdown-item" type="submit">Mark As Admin</button>
                              </form:form>
                            </li>
                          </ul>
                        </div>
                      </td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
            <c:if test="${userPage.totalPages > 1}">
              <nav aria-label="User pages">
                <ul class="pagination justify-content-end">
                  <c:url var="previousUsersUrl" value="/admin/dashboard">
                    <c:param name="userPage" value="${userPage.number - 1}" />
                    <c:param name="userSort" value="${userSort}" />
                    <c:param name="userDirection" value="${userDirection}" />
                    <c:param name="adminPage" value="${adminPage.number}" />
                    <c:param name="adminSort" value="${adminSort}" />
                    <c:param name="adminDirection" value="${adminDirection}" />
                  </c:url>
                  <li class="page-item ${userPage.first ? 'disabled' : ''}">
                    <a class="page-link" href="${userPage.first ? '#' : previousUsersUrl}">Previous</a>
                  </li>
                  <li class="page-item disabled"><span class="page-link">${userPage.number + 1} / ${userPage.totalPages}</span></li>
                  <c:url var="nextUsersUrl" value="/admin/dashboard">
                    <c:param name="userPage" value="${userPage.number + 1}" />
                    <c:param name="userSort" value="${userSort}" />
                    <c:param name="userDirection" value="${userDirection}" />
                    <c:param name="adminPage" value="${adminPage.number}" />
                    <c:param name="adminSort" value="${adminSort}" />
                    <c:param name="adminDirection" value="${adminDirection}" />
                  </c:url>
                  <li class="page-item ${userPage.last ? 'disabled' : ''}">
                    <a class="page-link" href="${userPage.last ? '#' : nextUsersUrl}">Next</a>
                  </li>
                </ul>
              </nav>
            </c:if>

            <!-- List Admin -->
            <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-3 mt-5">
              <h2 class="fw-bold mb-0 flex-grow-1">Admin</h2>
              <form action="${pageContext.request.contextPath}/admin/dashboard" class="d-flex gap-2">
                <input type="hidden" name="userPage" value="${userPage.number}" />
                <input type="hidden" name="userSort" value="${userSort}" />
                <input type="hidden" name="userDirection" value="${userDirection}" />
                <select class="form-select" name="adminSort" aria-label="Sort administrators">
                  <option value="idUser" ${adminSort eq 'idUser' ? 'selected' : ''}>ID</option>
                  <option value="username" ${adminSort eq 'username' ? 'selected' : ''}>Username</option>
                  <option value="email" ${adminSort eq 'email' ? 'selected' : ''}>Email</option>
                  <option value="profile.firstName" ${adminSort eq 'profile.firstName' ? 'selected' : ''}>First name</option>
                  <option value="profile.lastName" ${adminSort eq 'profile.lastName' ? 'selected' : ''}>Last name</option>
                </select>
                <select class="form-select" name="adminDirection" aria-label="Administrator sort direction">
                  <option value="asc" ${adminDirection eq 'asc' ? 'selected' : ''}>Ascending</option>
                  <option value="desc" ${adminDirection eq 'desc' ? 'selected' : ''}>Descending</option>
                </select>
                <button class="btn btn-outline-secondary" type="submit">Sort</button>
              </form>
            </div>
            <div class="table-responsive-md">
              <table class="table table-striped">
                <!-- Head -->
                <thead>
                  <tr>
                    <th>Id</th>
                    <th>First name</th>
                    <th>Last name</th>
                    <th>Email</th>
                    <th>Phone number</th>
                    <th>Address</th>
                    <th></th>
                  </tr>
                </thead>
                <!-- Body -->
                <tbody>
                  <c:forEach items="${listAdmin}" var="admin">
                    <tr>
                      <td>${admin.idUser}</td>
                      <td>${admin.profile.firstName}</td>
                      <td>${admin.profile.lastName}</td>
                      <td>${admin.email}</td>
                      <td>${admin.profile.phoneNumber}</td>
                      <td>${admin.profile.formattedShippingAddress}</td>
                      <td>
                        <div class="dropdown">
                          <button class="btn btn-warning dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false"><i class="fa-regular fa-pen-to-square"></i></button>
                          <ul class="dropdown-menu dropdown-menu-dark">
                            <li>
                              <a class="dropdown-item" href="<%= request.getContextPath() %>/admin/edit-user?id=${admin.profile.idProfile}">Edit Profile</a>
                            </li>
                          </ul>
                        </div>
                      </td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
            <c:if test="${adminPage.totalPages > 1}">
              <nav aria-label="Administrator pages">
                <ul class="pagination justify-content-end">
                  <c:url var="previousAdminsUrl" value="/admin/dashboard">
                    <c:param name="adminPage" value="${adminPage.number - 1}" />
                    <c:param name="adminSort" value="${adminSort}" />
                    <c:param name="adminDirection" value="${adminDirection}" />
                    <c:param name="userPage" value="${userPage.number}" />
                    <c:param name="userSort" value="${userSort}" />
                    <c:param name="userDirection" value="${userDirection}" />
                  </c:url>
                  <li class="page-item ${adminPage.first ? 'disabled' : ''}">
                    <a class="page-link" href="${adminPage.first ? '#' : previousAdminsUrl}">Previous</a>
                  </li>
                  <li class="page-item disabled"><span class="page-link">${adminPage.number + 1} / ${adminPage.totalPages}</span></li>
                  <c:url var="nextAdminsUrl" value="/admin/dashboard">
                    <c:param name="adminPage" value="${adminPage.number + 1}" />
                    <c:param name="adminSort" value="${adminSort}" />
                    <c:param name="adminDirection" value="${adminDirection}" />
                    <c:param name="userPage" value="${userPage.number}" />
                    <c:param name="userSort" value="${userSort}" />
                    <c:param name="userDirection" value="${userDirection}" />
                  </c:url>
                  <li class="page-item ${adminPage.last ? 'disabled' : ''}">
                    <a class="page-link" href="${adminPage.last ? '#' : nextAdminsUrl}">Next</a>
                  </li>
                </ul>
              </nav>
            </c:if>
            <!-- List Admin -->
          </div>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
