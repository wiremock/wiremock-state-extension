# Testing docker

Build and start the container:

```bash
./gradlew clean build -x test
```

```bash
docker run -it --rm \
-p 8080:8080 \
--name wiremock \
-v $PWD/build/libs:/var/wiremock/extensions \
-v $PWD/src/test/resources/remoteloader:/home/wiremock \
wiremock/wiremock:3x \
--global-response-templating
```
