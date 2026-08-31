FROM node:25

COPY ui /ui

RUN echo "cd /ui" > /vite.sh
RUN echo "npm install" >> /vite.sh
RUN echo "npm run dev -- --host" >> /vite.sh

CMD ["/bin/sh", "/vite.sh"]
