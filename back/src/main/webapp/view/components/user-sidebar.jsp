<c:choose>
  <c:when test="${accountNavActive eq 'profile'}"><c:set var="accountNavLabel" value="Profile" /></c:when>
  <c:when test="${accountNavActive eq 'bids'}"><c:set var="accountNavLabel" value="My bids" /></c:when>
  <c:when test="${accountNavActive eq 'followed'}"><c:set var="accountNavLabel" value="Followed auctions" /></c:when>
  <c:when test="${accountNavActive eq 'appointments'}"><c:set var="accountNavLabel" value="Appointments" /></c:when>
  <c:when test="${accountNavActive eq 'listings'}"><c:set var="accountNavLabel" value="My listings" /></c:when>
  <c:when test="${accountNavActive eq 'fixedListings'}"><c:set var="accountNavLabel" value="Car listings" /></c:when>
  <c:when test="${accountNavActive eq 'deposits'}"><c:set var="accountNavLabel" value="Reservation deposits" /></c:when>
  <c:when test="${accountNavActive eq 'sell'}"><c:set var="accountNavLabel" value="Sell a car" /></c:when>
  <c:when test="${accountNavActive eq 'orders'}"><c:set var="accountNavLabel" value="Parts orders" /></c:when>
  <c:when test="${accountNavActive eq 'notifications'}"><c:set var="accountNavLabel" value="Notifications" /></c:when>
  <c:otherwise><c:set var="accountNavLabel" value="Account menu" /></c:otherwise>
</c:choose>
<security:authentication property="principal.username" var="accountUsername" />

<aside class="account-sidebar" aria-label="Account navigation">
  <button
    class="account-sidebar-toggle d-lg-none"
    type="button"
    data-bs-toggle="collapse"
    data-bs-target="#accountSidebarNav"
    aria-controls="accountSidebarNav"
    aria-expanded="false"
  >
    <span>
      <span class="account-sidebar-toggle-kicker">Account menu</span>
      <strong>${accountNavLabel}</strong>
    </span>
    <i class="fa-solid fa-bars" aria-hidden="true"></i>
  </button>

  <div class="collapse d-lg-block" id="accountSidebarNav">
    <div class="account-sidebar-card">
      <div class="account-sidebar-profile">
        <a class="account-sidebar-avatar" href="${pageContext.request.contextPath}/user/upload-picture" aria-label="Change profile picture">
          <c:choose>
            <c:when test="${empty profileLog.profilePicture}">
              <img src="${pageContext.request.contextPath}/images/user/user-default.png" alt="" />
            </c:when>
            <c:otherwise>
              <img src="data:${profileLog.profilePicture.fileType};base64,${profileLog.profilePicture.image}" alt="" />
            </c:otherwise>
          </c:choose>
        </a>
        <div class="account-sidebar-identity">
          <span>Your account</span>
          <strong>
            <c:choose>
              <c:when test="${not empty profileLog.firstName}">${profileLog.firstName}</c:when>
              <c:otherwise>${accountUsername}</c:otherwise>
            </c:choose>
          </strong>
        </div>
      </div>

      <nav class="account-nav">
        <p class="account-nav-heading">Overview</p>
        <a class="account-nav-link ${accountNavActive eq 'profile' ? 'active' : ''}" href="${pageContext.request.contextPath}/user/my-profile" ${accountNavActive eq 'profile' ? 'aria-current="page"' : ''}>
          <span class="account-nav-icon"><i class="fa-regular fa-user"></i></span>
          <span>Profile</span>
        </a>
        <a class="account-nav-link ${accountNavActive eq 'notifications' ? 'active' : ''}" href="${pageContext.request.contextPath}/user/notifications" ${accountNavActive eq 'notifications' ? 'aria-current="page"' : ''}>
          <span class="account-nav-icon"><i class="fa-regular fa-bell"></i></span>
          <span>Notifications</span>
          <c:if test="${unreadNotificationCount > 0}">
            <span class="account-nav-badge">${unreadNotificationCount}</span>
          </c:if>
        </a>

        <p class="account-nav-heading">Buying</p>
        <a class="account-nav-link ${accountNavActive eq 'bids' ? 'active' : ''}" href="${pageContext.request.contextPath}/user/bids" ${accountNavActive eq 'bids' ? 'aria-current="page"' : ''}>
          <span class="account-nav-icon"><i class="fa-solid fa-gavel"></i></span>
          <span>My bids</span>
        </a>
        <a class="account-nav-link ${accountNavActive eq 'followed' ? 'active' : ''}" href="${pageContext.request.contextPath}/user/followed-auctions" ${accountNavActive eq 'followed' ? 'aria-current="page"' : ''}>
          <span class="account-nav-icon"><i class="fa-regular fa-star"></i></span>
          <span>Followed auctions</span>
        </a>
        <a class="account-nav-link ${accountNavActive eq 'appointments' ? 'active' : ''}" href="${pageContext.request.contextPath}/user/test-drive" ${accountNavActive eq 'appointments' ? 'aria-current="page"' : ''}>
          <span class="account-nav-icon"><i class="fa-regular fa-calendar-check"></i></span>
          <span>Appointments</span>
        </a>
        <a class="account-nav-link" href="${pageContext.request.contextPath}/cars">
          <span class="account-nav-icon"><i class="fa-solid fa-magnifying-glass"></i></span>
          <span>Browse auctions</span>
        </a>
        <a class="account-nav-link" href="${pageContext.request.contextPath}/listings">
          <span class="account-nav-icon"><i class="fa-solid fa-tags"></i></span>
          <span>Browse car listings</span>
        </a>
        <a class="account-nav-link ${accountNavActive eq 'deposits' ? 'active' : ''}" href="${pageContext.request.contextPath}/user/listing-deposits" ${accountNavActive eq 'deposits' ? 'aria-current="page"' : ''}>
          <span class="account-nav-icon"><i class="fa-solid fa-receipt"></i></span>
          <span>Reservation deposits</span>
        </a>

        <p class="account-nav-heading">Selling</p>
        <a class="account-nav-link ${accountNavActive eq 'listings' ? 'active' : ''}" href="${pageContext.request.contextPath}/user/my-posted-car" ${accountNavActive eq 'listings' ? 'aria-current="page"' : ''}>
          <span class="account-nav-icon"><i class="fa-solid fa-car"></i></span>
          <span>My auctions</span>
        </a>
        <a class="account-nav-link ${accountNavActive eq 'sell' ? 'active' : ''}" href="${pageContext.request.contextPath}/user/post-car" ${accountNavActive eq 'sell' ? 'aria-current="page"' : ''}>
          <span class="account-nav-icon"><i class="fa-solid fa-circle-plus"></i></span>
          <span>Post an auction</span>
        </a>
        <a class="account-nav-link ${accountNavActive eq 'fixedListings' ? 'active' : ''}" href="${pageContext.request.contextPath}/user/listings" ${accountNavActive eq 'fixedListings' ? 'aria-current="page"' : ''}>
          <span class="account-nav-icon"><i class="fa-solid fa-tag"></i></span>
          <span>My car listings</span>
        </a>
        <a class="account-nav-link ${accountNavActive eq 'fixedListings' ? 'active' : ''}" href="${pageContext.request.contextPath}/user/listings/new">
          <span class="account-nav-icon"><i class="fa-solid fa-circle-plus"></i></span>
          <span>List a car</span>
        </a>

        <p class="account-nav-heading">Parts store</p>
        <a class="account-nav-link ${accountNavActive eq 'orders' ? 'active' : ''}" href="${pageContext.request.contextPath}/orders" ${accountNavActive eq 'orders' ? 'aria-current="page"' : ''}>
          <span class="account-nav-icon"><i class="fa-solid fa-box"></i></span>
          <span>My orders</span>
        </a>
        <a class="account-nav-link" href="${pageContext.request.contextPath}/parts">
          <span class="account-nav-icon"><i class="fa-solid fa-screwdriver-wrench"></i></span>
          <span>Shop car parts</span>
        </a>
      </nav>

      <div class="account-sidebar-footer">
        <a href="${pageContext.request.contextPath}/user/edit-profile">
          <i class="fa-solid fa-gear"></i>
          Account settings
        </a>
        <security:authorize access="hasRole('ADMIN')">
          <a href="${pageContext.request.contextPath}/admin/dashboard">
            <i class="fa-solid fa-gauge-high"></i>
            Admin dashboard
          </a>
        </security:authorize>
      </div>
    </div>
  </div>
</aside>
