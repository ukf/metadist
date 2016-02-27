#
# Dockerfile for metadist / fetchmeta.
#
FROM java:openjdk-8

MAINTAINER Ian Young <ian@iay.org.uk>

#
# Acquire other OS tools.
#
RUN apt-get update && apt-get install -y rsync && \
	rm -rf /var/lib/apt/lists/*

#
# The /metadata volume is where output is sent.
#
VOLUME ["/metadata"]

#
# Copy in the main script and configuration.
#
WORKDIR /opt/metadist
COPY fetchmeta .
COPY ukfederation-2014.pem .
COPY CONFIG-docker CONFIG

#
# Make the transient directories used by the script.
#
# These are empty in the image, and although they are used by the script there's no
# need to preserve their contents across runs, so they don't become volumes.
#
RUN mkdir mirror && mkdir verify

#
# Acquire XmlSecTool tool.
#
RUN wget -q http://shibboleth.net/downloads/tools/xmlsectool/1.2.0/xmlsectool-1.2.0-bin.zip && \
	unzip xmlsectool-1.2.0-bin.zip && \
	rm xmlsectool-1.2.0-bin.zip

CMD ["./fetchmeta"]
