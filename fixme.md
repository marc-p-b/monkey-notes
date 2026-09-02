
# agent exception

```
traefik-1  | 104.23.225.137 - - [02/Sep/2026:10:36:34 +0000] "GET /api/image/mb/msc9d64912303aebf1a443dc50791e83d3d7e9d867ba133c789e506350327f42cd/0 HTTP/2.0" 200 9604 "-" "-" 102 "api@docker" "http://172.21.0.4:8080" 14ms
traefik-1  | 104.23.225.136 - - [02/Sep/2026:10:36:44 +0000] "GET /api/transcript/msb06c3d8d7bc845befed85032303caf495c09a8d645a68aca5eb8784620206c2c HTTP/2.0" 200 862 "-" "-" 103 "api@docker" "http://172.21.0.4:8080" 18ms
traefik-1  | 104.23.225.137 - - [02/Sep/2026:10:36:50 +0000] "GET /api/transcript/ms06c71be87231256aa4af23f66076cf1621801aa3684b234763bf98f438f5daea HTTP/2.0" 200 3286 "-" "-" 104 "api@docker" "http://172.21.0.4:8080" 30ms
traefik-1  | 104.23.225.136 - - [02/Sep/2026:10:36:55 +0000] "GET /api/agent/prepare?fileIds=ms06c71be87231256aa4af23f66076cf1621801aa3684b234763bf98f438f5daea HTTP/2.0" 200 1027 "-" "-" 105 "api@docker" "http://172.21.0.4:8080" 18ms
api-1      | ERROR url http://notes.monkeynotes.fr/agent/ask
api-1      | org.springframework.web.client.HttpClientErrorException$Unauthorized: 401 Unauthorized: [no body]
api-1      | 	at org.springframework.web.client.HttpClientErrorException.create(HttpClientErrorException.java:106)
api-1      | 	at org.springframework.web.client.DefaultResponseErrorHandler.handleError(DefaultResponseErrorHandler.java:183)
api-1      | 	at org.springframework.web.client.DefaultResponseErrorHandler.handleError(DefaultResponseErrorHandler.java:137)
api-1      | 	at org.springframework.web.client.ResponseErrorHandler.handleError(ResponseErrorHandler.java:63)
api-1      | 	at org.springframework.web.client.RestTemplate.handleResponse(RestTemplate.java:942)
api-1      | 	at org.springframework.web.client.RestTemplate.doExecute(RestTemplate.java:891)
api-1      | 	at org.springframework.web.client.RestTemplate.execute(RestTemplate.java:790)
api-1      | 	at org.springframework.web.client.RestTemplate.postForEntity(RestTemplate.java:538)
api-1      | 	at fr.monkeynotes.mn.service.AgentService.uploadKnowledgeFile(AgentService.java:265)
api-1      | 	at fr.monkeynotes.mn.service.AgentService.newAssistant(AgentService.java:163)
api-1      | 	at fr.monkeynotes.mn.service.AgentService.getOrCreateAssistant(AgentService.java:92)
api-1      | 	at fr.monkeynotes.mn.controller.AgentController.agentStreamLink(AgentController.java:71)
api-1      | 	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
api-1      | 	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
```