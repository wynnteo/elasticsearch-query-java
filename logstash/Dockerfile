# Use the official Logstash image from Docker Hub
FROM docker.elastic.co/logstash/logstash:8.14.1

# Copy your Logstash configuration file into the container
COPY mysql.conf /usr/share/logstash/mysql.conf

ENV LS_HOME /usr/share/logstash

# Install MySQL JDBC driver
RUN logstash-plugin install logstash-integration-jdbc

COPY mysql-connector-j-9.0.0.jar ${LS_HOME}/logstash-core/lib/jars/

# Start Logstash and load the configuration
CMD ["logstash", "-f", "/usr/share/logstash/mysql.conf"]