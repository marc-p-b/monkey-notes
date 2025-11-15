# Use official Node.js LTS image as the base
FROM node:25

CMD mkdir /ui

# Set working directory
WORKDIR ui

# Copy package.json and package-lock.json first (for caching dependencies)
#COPY ui/package*.json ./

# Install dependencies
#RUN npm install

# Copy the rest of the application code
#COPY ui .
