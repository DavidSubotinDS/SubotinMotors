<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/user.css" />
  </head>
  <body>
    <!-- Navbar -->
    <%@ include file="../components/navbar.jsp" %>

    <!-- Main -->
    <main>
      <div class="container pt-5">
        <div class="d-flex">
          <!-- Sidebar -->
          <aside class="sidebar-user pe-md-3">
            <ul>
              <li class="active-page">
                <a href="<%= request.getContextPath() %>/user"><i class="fa-solid fa-user"></i> Profile</a>
              </li>
              <li>
                <a href="<%= request.getContextPath() %>/user/my-posted-car"><i class="fa-solid fa-car"></i> My Posted Car</a>
              </li>
              <li>
                <a href="<%= request.getContextPath() %>/user/test-drive"><i class="fa-regular fa-calendar-check"></i> Appointment</a>
              </li>
              <li>
                <a href="<%= request.getContextPath() %>/user/payments"><i class="fa-solid fa-credit-card"></i> Payments</a>
              </li>
            </ul>
          </aside>

          <!-- Content -->
          <div class="content-wrapper">
            <div class="user d-flex justify-content-between">
              <div class="d-flex flex-column flex-md-row">
                <div class="user-image me-4" onclick="location.href='<%= request.getContextPath() %>/user/upload-picture'; ">
                  <c:if test="${profile.profilePicture == null}">
                    <img src="/images/user/user-default.png" alt="${profile.firstName}" />
                  </c:if>
                  <c:if test="${profile.profilePicture != null}">
                    <img src="data:${profile.profilePicture.fileType};base64,${profile.profilePicture.image}" alt="${profile.firstName}" />
                  </c:if>
                </div>
                <div class="user-details pt-4">
                  <h5 class="fw-semibold">${profile.firstName} ${profile.lastName}</h5>
                  <p class="text-secondary m-0">${profile.phoneNumber}</p>
                  <p class="text-secondary m-0">${profile.address}</p>
                </div>
              </div>
              <div class="pt-4">
                <a href="<%= request.getContextPath() %>/user/edit-profile">
                  <button class="btn btn-outline-secondary">Edit Profile</button>
                </a>
              </div>
            </div>
            <!-- About -->
            <c:if test="${profile.about != null}">
              <div class="about mt-3">
                <h5 class="fw-semibold">About Me</h5>
                <p>${profile.about}</p>
              </div>
            </c:if>
            <div class="about mt-3">
              <h5 class="fw-semibold">Seller payouts</h5>
              <c:choose>
                <c:when test="${!stripeEnabled}">
                  <p class="text-secondary mb-0">Stripe Connect is not configured in this environment.</p>
                </c:when>
                <c:when test="${paymentAccount != null && paymentAccount.transfersEnabled}">
                  <p class="text-success mb-2">Your Stripe payout account is ready.</p>
                  <a class="btn btn-outline-secondary" href="<%= request.getContextPath() %>/user/payments">View payments</a>
                </c:when>
                <c:otherwise>
                  <p class="text-secondary">Connect Stripe before an administrator can accept a bid on one of your cars.</p>
                  <a class="btn btn-primary" href="<%= request.getContextPath() %>/payments/seller/onboarding">Connect Stripe</a>
                </c:otherwise>
              </c:choose>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
