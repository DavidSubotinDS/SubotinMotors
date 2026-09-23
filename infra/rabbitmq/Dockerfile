FROM rabbitmq:4.2.5-management@sha256:e43ca03df740d48770641b56ce1a26913c74a31e47c1ed36a58e8d91c10731b9
USER root
RUN apt-get update && apt-get install -y --no-install-recommends python3 && rm -rf /var/lib/apt/lists/*
COPY rabbitmq.conf /etc/rabbitmq/rabbitmq.conf
COPY boot.py /usr/local/bin/autostrada-broker
RUN chmod 755 /usr/local/bin/autostrada-broker
USER rabbitmq
ENTRYPOINT ["python3", "/usr/local/bin/autostrada-broker"]
