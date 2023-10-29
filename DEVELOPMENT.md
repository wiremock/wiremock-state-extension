# Extension development notes

## Testing docker

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

## Releasing

To release the module, go to [GitHub Releases](https://github.com/wiremock/wiremock-extension-state/releases) and
issue the release using the changelog draft.
The new release will trigger the [Release GitHub Action](https://github.com/wiremock/wiremock-extension-state/actions/workflows/release.yml)
which will deploy the artifacts to Maven Central.
