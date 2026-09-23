"""Private topology and least-privilege users; generated hashes never enter image layers."""
import base64
import hashlib
import json
import os


def definitions():
    users = []
    passwords = []
    for name in ("backend", "identity", "notification", "operator"):
        password = os.environ.get("RABBITMQ_" + name.upper() + "_PASSWORD", "")
        if len(password) < 32 or password in passwords:
            raise ValueError("Distinct broker credentials of at least 32 characters are required")
        passwords.append(password)
        salt = os.urandom(4)
        users.append({"name": name, "password_hash": base64.b64encode(salt + hashlib.sha256(salt + password.encode()).digest()).decode(),
                      "hashing_algorithm": "rabbit_password_hashing_sha256", "tags": ["management"] if name == "operator" else []})
    exchanges = [{"name": name, "vhost": "autostrada", "type": "topic", "durable": True, "auto_delete": False, "internal": False, "arguments": {}}
                 for name in ("autostrada.events", "autostrada.commands", "autostrada.notification-routing")]
    queues, bindings = [], []
    for queue, exchange, routing in (("notification.business.v1", "autostrada.events", "marketplace.auction-ending-soon.v1"),
                                     ("notification.delivery.v1", "autostrada.commands", "identity.password-reset-delivery.v1")):
        sensitive = "delivery" in queue
        queues.append({"name": queue, "vhost": "autostrada", "durable": True, "auto_delete": False,
                       "arguments": {"x-message-ttl": 1800000} if sensitive else {}})
        bindings.append({"source": exchange, "vhost": "autostrada", "destination": queue, "destination_type": "queue", "routing_key": routing, "arguments": {}})
        queues.append({"name": queue + ".dlq", "vhost": "autostrada", "durable": True, "auto_delete": False,
                       "arguments": {"x-message-ttl": 1800000} if sensitive else {}})
        for attempt, delay in enumerate((5000, 30000, 120000), 1):
            queues.append({"name": queue + ".retry." + str(attempt), "vhost": "autostrada", "durable": True, "auto_delete": False,
                           "arguments": {"x-message-ttl": delay, "x-dead-letter-exchange": exchange, "x-dead-letter-routing-key": routing,
                                         "x-queue-type": "quorum", "x-overflow": "reject-publish", "x-dead-letter-strategy": "at-least-once"}})
    for queue in queues:
        bindings.append({"source": "autostrada.notification-routing", "vhost": "autostrada", "destination": queue["name"],
                         "destination_type": "queue", "routing_key": queue["name"], "arguments": {}})
    permissions = [
        {"user": "backend", "vhost": "autostrada", "configure": "^$", "write": "^autostrada\\.events$", "read": "^$"},
        {"user": "identity", "vhost": "autostrada", "configure": "^$", "write": "^autostrada\\.commands$", "read": "^$"},
        {"user": "notification", "vhost": "autostrada", "configure": "^$", "write": "^autostrada\\.notification-routing$",
         "read": "^notification\\.(business|delivery)\\.v1(\\.(retry\\.[123]|dlq))?$"},
        {"user": "operator", "vhost": "autostrada", "configure": "^$", "write": "^autostrada\\.notification-routing$",
         "read": "^notification\\.(business|delivery)\\.v1(\\.(retry\\.[123]|dlq))?$"}]
    return {"users": users, "vhosts": [{"name": "autostrada"}], "permissions": permissions, "exchanges": exchanges, "queues": queues, "bindings": bindings,
            "topic_permissions": [{"user": "backend", "vhost": "autostrada", "exchange": "autostrada.events", "write": "^marketplace\\.auction-ending-soon\\.v1$", "read": "^$"},
                                  {"user": "identity", "vhost": "autostrada", "exchange": "autostrada.commands", "write": "^identity\\.password-reset-delivery\\.v1$", "read": "^$"},
                                  {"user": "notification", "vhost": "autostrada", "exchange": "autostrada.notification-routing", "write": "^notification\\.(business|delivery)\\.v1\\.(retry\\.[123]|dlq)$", "read": "^$"},
                                  {"user": "operator", "vhost": "autostrada", "exchange": "autostrada.notification-routing", "write": "^notification\\.(business|delivery)\\.v1(\\.(retry\\.[123]|dlq))?$", "read": "^$"}]}


if __name__ == "__main__":
    os.umask(0o077)
    with open("/tmp/autostrada-definitions.json", "w", encoding="utf-8") as output:
        json.dump(definitions(), output)
    os.execvp("docker-entrypoint.sh", ["docker-entrypoint.sh", "rabbitmq-server"])
