import { useState } from 'react';
import { MessageSquare, Send } from 'lucide-react';

import Button from '../ui/Button.jsx';
import Alert from '../ui/Alert.jsx';

export default function CommentThread({ comments = [], onSubmit, signedIn }) {
  const [body, setBody] = useState('');
  const [imageFile, setImageFile] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await onSubmit({ body, imageFile });
      setBody('');
      setImageFile(null);
      event.currentTarget.reset();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="discussion" id="discussion">
      <div className="section-header">
        <h2>
          <MessageSquare aria-hidden="true" size={20} /> Discussion
        </h2>
      </div>

      {comments.length === 0 ? (
        <div className="empty-state">No comments yet.</div>
      ) : (
        <div className="comment-list">
          {comments.map((comment) => (
            <article className="comment-card" key={comment.idComment}>
              <header>
                <strong>{comment.authorName}</strong>
                <span>{comment.createdAtDisplay}</span>
              </header>
              {comment.badgeLabel && <small className="comment-badge">{comment.badgeLabel}</small>}
              <p>{comment.body}</p>
              {comment.imageDataUrl && (
                <img className="comment-image" src={comment.imageDataUrl} alt="" />
              )}
            </article>
          ))}
        </div>
      )}

      {signedIn ? (
        <form className="comment-form" onSubmit={submit}>
          {error && <Alert title="Comment not posted">{error}</Alert>}
          <textarea
            name="body"
            rows="4"
            placeholder="Write a thoughtful comment"
            value={body}
            onChange={(event) => setBody(event.target.value)}
          />
          <div className="form-actions">
            <input
              type="file"
              name="imageFile"
              accept="image/png,image/jpeg,image/webp"
              onChange={(event) => setImageFile(event.target.files?.[0] ?? null)}
            />
            <Button type="submit" disabled={busy}>
              <Send aria-hidden="true" size={16} /> Post
            </Button>
          </div>
        </form>
      ) : (
        <div className="empty-state">Sign in to join the discussion.</div>
      )}
    </section>
  );
}
