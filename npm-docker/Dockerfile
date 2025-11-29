FROM node:25

CMD mkdir /ui

WORKDIR ui

RUN echo "cd /ui" > /vite.sh
RUN echo "npm install" >> /vite.sh
RUN echo "npm run dev -- --host" >> /vite.sh

RUN echo "cd /ui" > /dist.sh
RUN echo "npm install" >> /dist.sh
RUN echo "rm -rf /ui/dist" >> /dist.sh
RUN echo "npm run build" >> /dist.sh

CMD ["/bin/sh", "/vite.sh"]