<%@ include file="components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="components/header.jsp" %>
    <link rel="stylesheet" href="/css/about-us.css" />
  </head>
  <body>
    <!-- Navbar -->
    <%@ include file="components/navbar.jsp" %>

    <!-- Main -->
    <main>
      <div class="container-fluid jumbotron">
        <div class="container p-4">
          <h2 class="pt-5 fw-bolder">About Subotin Motors</h2>
        </div>
      </div>

      <div class="container mt-4 d-flex">
        <div class="about p-4">
          <h3 class="fw-bolder">Who we are</h3>
          <p class="text-secondary">
            Subotin Motors is a leading digital marketplace and solutions provider for the automotive industry that connects car shoppers with sellers. Founded in 2024 and headquartered in Serbia, the company empowers shoppers with the data, resources, and digital tools needed to make informed buying decisions and seamlessly connect with automotive retailers. In a rapidly changing market, Subotin Motors enables dealerships and OEMs with innovative technical solutions and data-driven intelligence to better reach and influence ready-to-buy shoppers, increase inventory turn, and gain market share.
          </p>
        </div>
        <img class="image-about" src="/images/background/about-us-02.jpg" alt="" />
      </div>
    </main>

    <!-- Footer -->
    <%@ include file="components/footer.jsp" %>
  </body>
</html>
