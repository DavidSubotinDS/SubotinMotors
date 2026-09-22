package lithan.autostrada.identity.identity;

/** Identity of the authenticated caller, never an owner ID supplied in a request. */
public interface CurrentIdentity {
  int requireUserId();
}
