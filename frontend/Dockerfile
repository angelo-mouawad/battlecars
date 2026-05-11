FROM node:18
WORKDIR /usr/src/app
COPY package*.json ./
RUN npm install
COPY . .
RUN rm -f next-env.d.ts && \
    chmod -R 777 /usr/src/app
EXPOSE 3000
CMD [ "npm", "run", "dev" ]