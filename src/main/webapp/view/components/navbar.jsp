<nav class="navbar navbar-expand-lg sticky-top p-0">
  <div class="container">
    <a class="navbar-brand fw-bold fs-5" href="<%= request.getContextPath() %>/">Subotin Motors</a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarNav">
      <ul class="navbar-nav ms-auto me-0 me-lg-auto fw-semibold">
        <li class="nav-item me-0 me-lg-3">
          <a class="nav-link" href="<%= request.getContextPath() %>/">Home</a>
        </li>
        <li class="nav-item me-0 me-lg-3">
          <a class="nav-link" href="<%= request.getContextPath() %>/cars">Cars</a>
        </li>
        <li class="nav-item me-0 me-lg-3">
          <a class="nav-link" href="<%= request.getContextPath() %>/parts">Car Parts</a>
        </li>
        <li class="nav-item me-0 me-lg-3">
          <a class="nav-link" href="<%= request.getContextPath() %>/user/post-car">Sell a Car</a>
        </li>
      </ul>

      <!-- Login & Logout -->
      <security:authorize access="!isAuthenticated()">
        <div class="nav-button d-flex flex-column flex-lg-row">
          <a href="<%= request.getContextPath() %>/login" class="btn btn-outline-secondary me-0 me-lg-3 mb-3 mb-lg-0">Login</a>
          <a href="<%= request.getContextPath() %>/register" class="btn btn-primary mb-3 mb-lg-0">Register</a>
        </div>
      </security:authorize>

      <!-- User Menu -->
      <security:authorize access="isAuthenticated()">
        <a class="btn btn-outline-secondary me-3 position-relative" href="<%= request.getContextPath() %>/cart" aria-label="Shopping cart">
          <i class="fa-solid fa-cart-shopping"></i>
          <c:if test="${cartItemCount > 0}">
            <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">${cartItemCount}</span>
          </c:if>
        </a>
        <ul class="navbar-nav fw-semibold">
          <li class="nav-item dropdown">
            <a class="nav-link dropdown-toggle d-flex align-items-center" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
              <div class="nav-account me-2">
                <!-- Profile Picture -->
                <c:if test="${profileLog.profilePicture == null}">
                  <img src="/images/user/user-default.png" alt="Profile Picture" />
                </c:if>
                <c:if test="${profileLog.profilePicture != null}">
                  <img src="data:${profileLog.profilePicture.fileType};base64,${profileLog.profilePicture.image}" alt="Profile Picture" />
                </c:if>
              </div>
              ${profileLog.firstName}
            </a>
            <ul class="dropdown-menu">
              <li>
                <a class="dropdown-item" href="<%= request.getContextPath() %>/user"><i class="fa-solid fa-user"></i> Profile</a>
              </li>
              <li>
                <a class="dropdown-item" href="<%= request.getContextPath() %>/user/my-posted-car"><i class="fa-solid fa-car"></i> My Posted Car</a>
              </li>
              <li>
                <a class="dropdown-item" href="<%= request.getContextPath() %>/user/bids"><i class="fa-solid fa-gavel"></i> My Bids</a>
              </li>
              <li>
                <a class="dropdown-item" href="<%= request.getContextPath() %>/user/followed-auctions"><i class="fa-regular fa-star"></i> Followed Auctions</a>
              </li>
              <li>
                <a class="dropdown-item" href="<%= request.getContextPath() %>/user/notifications">
                  <i class="fa-regular fa-bell"></i> Notifications
                  <c:if test="${unreadNotificationCount > 0}">
                    <span class="badge bg-danger">${unreadNotificationCount}</span>
                  </c:if>
                </a>
              </li>
              <li>
                <a class="dropdown-item" href="<%= request.getContextPath() %>/user/test-drive"><i class="fa-regular fa-calendar-check"></i> Appointment</a>
              </li>
              <li>
                <a class="dropdown-item" href="<%= request.getContextPath() %>/orders"><i class="fa-solid fa-box"></i> Parts Orders</a>
              </li>
              <!-- Admin Dashboard -->
              <security:authorize access="hasRole('ADMIN')">
                <li>
                  <a class="dropdown-item" href="<%= request.getContextPath() %>/admin/dashboard"><i class="fa-solid fa-gauge-high"></i> Dashboard </a>
                </li>
              </security:authorize>

              <li class="dropdown-divider"></li>
              <li>
                <form:form action="${pageContext.request.contextPath}/logout" method="POST">
                  <button class="dropdown-item nav-logout btn btn-primary" type="submit">Logout</button>
                </form:form>
              </li>
            </ul>
          </li>
        </ul>
      </security:authorize>
    </div>
  </div>
</nav>
