<section class="listing-discussion" id="discussion" aria-labelledby="discussion-title">
  <div class="listing-discussion-heading">
    <div>
      <span class="listing-discussion-kicker">Community discussion</span>
      <h2 id="discussion-title">${commentTitle}</h2>
      <p>${commentSubtitle}</p>
    </div>
    <span class="listing-comment-count">
      <i class="fa-regular fa-comments"></i>
      ${comments.size()}
    </span>
  </div>

  <c:if test="${not empty commentMessage}">
    <div class="alert alert-success">${commentMessage}</div>
  </c:if>
  <c:if test="${not empty commentError}">
    <div class="alert alert-danger">${commentError}</div>
  </c:if>

  <security:authorize access="isAuthenticated()">
    <form:form
      action="${commentAction}"
      method="POST"
      modelAttribute="commentForm"
      cssClass="listing-comment-form"
      enctype="multipart/form-data"
    >
      <label for="commentBody">Join the conversation</label>
      <form:textarea
        id="commentBody"
        path="body"
        rows="3"
        maxlength="1000"
        cssClass="form-control"
        placeholder="${commentPlaceholder}"
      />
      <div class="listing-comment-attachment">
        <label class="listing-comment-file-label" for="commentImageFile">
          <i class="fa-regular fa-image"></i>
          Add a picture
        </label>
        <input
          class="form-control"
          id="commentImageFile"
          type="file"
          name="imageFile"
          accept="image/jpeg,image/png"
          data-comment-image-input
        />
        <span>Optional JPEG or PNG, up to 5 MB.</span>
        <div class="listing-comment-preview" data-comment-image-preview hidden>
          <img alt="Selected comment attachment preview" />
          <button type="button" class="btn btn-sm btn-outline-secondary" data-comment-image-remove>
            Remove picture
          </button>
        </div>
      </div>
      <div class="listing-comment-form-footer">
        <span>Be helpful, specific, and respectful.</span>
        <button class="btn btn-primary" type="submit">
          <i class="fa-regular fa-paper-plane me-1"></i>
          Post comment
        </button>
      </div>
    </form:form>
  </security:authorize>

  <security:authorize access="!isAuthenticated()">
    <div class="listing-comment-login">
      <i class="fa-regular fa-comment-dots"></i>
      <div>
        <strong>Want to ask a question?</strong>
        <p class="mb-0">Log in to join this discussion.</p>
      </div>
      <a class="btn btn-outline-primary ms-md-auto" href="${pageContext.request.contextPath}/login">Log in</a>
    </div>
  </security:authorize>

  <div class="listing-comment-list">
    <c:forEach items="${comments}" var="comment">
      <article class="listing-comment ${comment.highlightClass}">
        <div class="listing-comment-avatar" aria-hidden="true">
          <i class="${comment.highlighted ? 'fa-solid fa-shield-halved' : 'fa-regular fa-user'}"></i>
        </div>
        <div class="listing-comment-content">
          <div class="listing-comment-meta">
            <strong><c:out value="${comment.authorName}" /></strong>
            <c:if test="${comment.highlighted}">
              <span class="listing-comment-badge">
                <i class="fa-solid fa-circle-check"></i>
                ${comment.badgeLabel}
              </span>
            </c:if>
            <time>${comment.createdAtDisplay}</time>
          </div>
          <p><c:out value="${comment.body}" /></p>
          <c:if test="${comment.hasImage}">
            <a
              class="listing-comment-image"
              href="data:${comment.imageFileType};base64,${comment.imageData}"
              target="_blank"
              rel="noopener"
              aria-label="Open attached comment image"
            >
              <img
                src="data:${comment.imageFileType};base64,${comment.imageData}"
                alt="Comment attachment"
                loading="lazy"
              />
            </a>
          </c:if>
        </div>
      </article>
    </c:forEach>

    <c:if test="${empty comments}">
      <div class="listing-comment-empty">
        <i class="fa-regular fa-comments"></i>
        <h3>No comments yet</h3>
        <p>Start the conversation with a useful question.</p>
      </div>
    </c:if>
  </div>
</section>
<script src="${pageContext.request.contextPath}/js/comment-image-preview.js"></script>
