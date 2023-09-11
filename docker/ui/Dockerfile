#
# conductor:ui - Netflix Conductor UI
#
FROM public.ecr.aws/docker/library/node:18.17.1
LABEL maintainer="Netflix OSS <conductor@netflix.com>"

RUN apt update
# Install the required packages for the node build
# to run on alpine
# RUN apt install -y python3 py3-pip make g++

# Required Packages
RUN apt install -y procps screen default-mysql-client ncat

RUN mkdir -p /app
 
# A directory within the virtualized Docker environment
# Becomes more relevant when using Docker Compose later
WORKDIR /app

# Copies package.json to Docker environment in a separate layer as a performance optimization
COPY ./ui/package.json ./
 
# Installs all node packages. Cached unless package.json changes
RUN yarn install
 
# Copies everything else over to Docker environment
# node_modules excluded in .dockerignore. 
COPY ./ui .

# Creating a new user deploy with group id 3130 and name conductor
RUN groupadd -g 3130 conductor \
    && useradd --uid 3130 --home-dir /home/deploy --gid conductor --create-home --no-user-group deploy

RUN chown -R deploy:conductor /app

USER deploy:conductor

CMD [ "yarn", "start" ]
