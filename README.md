# Wiremock State extension

Adds support to transport state across different stubs.

## Background

Wiremock supports [Response Templating](https://wiremock.org/docs/response-templating/) and [Scenarios](https://wiremock.org/docs/stateful-behaviour/)
to add dynamic behavior and state. Both approaches have limitations:

- `Response templating` only allows accessing data submitted in the same request
- `Scenarios` cannot transport any data other than the state value itself

In order to mock more complex scenarios which are similar to a sandbox for a web service, it can be required to use parts of a previous request.

## Example use case

Create a sandbox for a webservice. The web service has two APIs:

1. `POST` to create a new identity (`POST /identity`)
    - Request:
   ```json
   {
    "firstName": "John",
    "lastName": "Doe"
   }
    ```
    - Response:
   ```json
    {
      "id": "kn0ixsaswzrzcfzriytrdupnjnxor1is", # Random value
      "firstName": "John",
      "lastName": "Doe" 
   }
    ```
2. `GET` to retrieve this value (`GET /identity/kn0ixsaswzrzcfzriytrdupnjnxor1is`)

- Response:

  ```json
    {
      "id": "kn0ixsaswzrzcfzriytrdupnjnxor1is",
      "firstName": "John",
      "lastName": "Doe"
    }
  ```

The sandbox should have no knowledge of the data that is inserted. While the `POST` can be achieved
with [Response Templating](https://wiremock.org/docs/response-templating/),
the `GET` won't have any knowledge of the previous post.

# Usage

## Compatibility matrix

| `wiremock-extension-state` version | `WireMock` version |
|------------------------------------|--------------------|
| `0.0.3`+                           | `3.0.0-beta-11`+   |

## Installation

### Gradle

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/wiremock/wiremock-extension-state")
    }
}


dependencies {
    testImplementation("org.wiremock:wiremock-state-extension:<your-version>")
}
```

### Maven

Follow the instructions on [GitHub Docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry) to
add authentication to GitHub packages.  

```xml

<repositories>
    <repository>
        <id>github-wiremock-extension-state</id>
        <name>WireMock Extension State Apache Maven Packages</name>
        <url>https://maven.pkg.github.com/wiremock/wiremock-extension-state</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>org.wiremock</groupId>
        <artifactId>wiremock-state-extension</artifactId>
        <version>your-version</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Register extension

### Java

This extension makes use of Wiremock's `ExtensionFactory`, so only one extension has to be registered: `StateExtension`.
In order to use them, templating has to be enabled as well. A store for all state data has to be provided. This extension
provides a `CaffeineStore` which can be used - or you can provide your own store:

```java
public class MySandbox {
    private final WireMockServer server;

    public MySandbox() {
        var stateRecordingAction = new StateRecordingAction();
        var store = new CaffeineStore();
        server = new WireMockServer(
            options()
                .dynamicPort()
                .templatingEnabled(true)
                .globalTemplating(true)
                .extensions(new StateExtension(store))
        );
        server.start();
    }
}
```

### Standalone

This extension uses the `ServiceLoader` extension to be loaded by WireMock. As Standalone version, it will use `CaffeineStore` for
storing any data.

The standalone jar can be downloaded from [GitHub](https://github.com/wiremock/wiremock-extension-state/packages/1902576) .

```bash
java -cp "wiremock-state-extension-standalone-0.0.4.jar:wiremock-standalone-3.0.0-beta-11.jar" wiremock.Run
```

## Record a state

The state is recorded in `withServeEventListener` of a stub. The following parameters have to be provided:

<table>
<tr>
<th>Parameter</th>
<th>Type</th>
<th>Example</th>
</tr>
<tr>
<td>

`context`

</td>
<td>String</td>
<td>

- `"context": "{{jsonPath response.body '$.id'}}"`
- `"context": "{{request.pathSegments.[3]}}"`

</td>
</tr>
<tr>
<td>

`state`

</td>
<td>Object</td>
<td>

```json
  {
  "id": "{{jsonPath response.body '$.id'}}",
  "firstName": "{{jsonPath request.body '$.firstName'}}",
  "lastName": "{{jsonPath request.body '$.lastName'}}",
}
  ```

</td>
</tr>
</table>

Templating (as in [Response Templating](https://wiremock.org/docs/response-templating/)) is supported for these. The following models are exposed:

- `request`: All model elements of as in [Response Templating](https://wiremock.org/docs/response-templating/)
- `response`: `body` and `headers`

Full example:

```json
{
  "request": {},
  "response": {},
  "withServeEventListener": [
    {
      "name": "recordState",
      "parameters": {
        "context": "{{jsonPath response.body '$.id'}}",
        "state": {
          "id": "{{jsonPath response.body '$.id'}}",
          "firstName": "{{jsonPath request.body '$.firstName'}}",
          "lastName": "{{jsonPath request.body '$.lastName'}}"
        }
      }
    }
  ]
}
```

To record a complete response body, use:

```json
{
  "request": {},
  "response": {},
  "withServeEventListener": [
    {
      "name": "recordState",
      "parameters": {
        "context": "{{jsonPath response.body '$.id'}}",
        "state": {
          "fullBody": "{{jsonPath response.body '$'}}"
        }
      }
    }
  ]
}

```

## Deleting a state

Similar to recording a state, its deletion can be initiated in  `withServeEventListener` of a stub. The following parameters have to be provided:

<table>
<tr>
<th>Parameter</th>
<th>Type</th>
<th>Example</th>
</tr>
<tr>
<td>

`context`

</td>
<td>String</td>
<td>

- `"context": "{{jsonPath response.body '$.id'}}"`
- `"context": "{{request.pathSegments.[3]}}"`

</td>
</tr>
</table>

Templating (as in [Response Templating](https://wiremock.org/docs/response-templating/)) is supported for these. The following models are exposed:

- `request`: All model elements of as in [Response Templating](https://wiremock.org/docs/response-templating/)
- `response`: `body` and `headers`

Full example:

```json
{
  "request": {},
  "response": {},
  "withServeEventListener": [
    {
      "name": "deleteState",
      "parameters": {
        "context": "{{jsonPath response.body '$.id'}}"
      }
    }
  ]
}

```

### state expiration

This extension provides a `CaffeineStore` which uses [caffeine](https://github.com/ben-manes/caffeine) to store the current state and to achieve an expiration (
to avoid memory leaks).
The default expiration is 60 minutes. The default value can be overwritten (`0` = default = 60 minutes):

```java
int expiration=1024;
    var store=new CaffeineStore(expiration);
```

## Match a request against a context

To have a WireMock stub only apply when there's actually a matching context, you can use the `StateRequestMatcher` . This helps to model different
behavior for requests with and without a matching context. The parameter supports templates.

### Positive context exists match

```json
{
  "request": {
    "method": "GET",
    "urlPattern": "/test/[^\/]+",
    "customMatcher": {
      "name": "state-matcher",
      "parameters": {
        "hasContext": "{{request.pathSegments.[1]}}"
      }
    }
  },
  "response": {
    "status": 200
  }
}
```

### Context update count match

Whenever the serve event listener `recordState` is processed, the internal context update counter is increased. The number can be used
for request matching as well. The following matchers are available:

- `updateCountEqualTo`
- `updateCountLessThan`
- `updateCountMoreThan`

As for other matchers, templating is supported.

```json
{
  "request": {
    "method": "GET",
    "urlPattern": "/test/[^\/]+",
    "customMatcher": {
      "name": "state-matcher",
      "parameters": {
        "hasContext": "{{request.pathSegments.[1]}}",
        "updateCountEqualTo": "1"
      }
    }
  },
  "response": {
    "status": 200
  }
}
```

### Negative context exists match

```json
{
  "request": {
    "method": "GET",
    "urlPattern": "/test/[^\/]+",
    "customMatcher": {
      "name": "state-matcher",
      "parameters": {
        "hasNotContext": "{{request.pathSegments.[1]}}"
      }
    }
  },
  "response": {
    "status": 400
  }
}
```

## Retrieve a state

A state can be retrieved using a handlebar helper. In the example above, the `StateHelper` is registered by the name `state`.
In a `jsonBody`, the state can be retrieved via: `"clientId": "{{state context=request.pathSegments.[1] property='firstname'}}",`

The handler has two parameters:

- `context`:  has to match the context data was registered with
- `property`: the property of the state context to retrieve, so e.g. `firstName`
  - `property='updateCount` retrieves the number of updates to a certain state.
    The number matches the one described in [Context update count match](#context-update-count-match)

To retrieve a full body, use: `{{{state context=request.pathSegments.[1] property='fullBody'}}}` .

### Error handling

Missing Helper properties as well as unknown context properties are reported as error. Wiremock renders them in the field, itself, so there won't be an
exception.

Example response with error:

```json
{
  "id": "kn0ixsaswzrzcfzriytrdupnjnxor1is",
  "firstName": "[ERROR: No state for context kn0ixsaswzrzcfzriytrdupnjnxor1is, property firstName found]",
  "lastName": "Doe"
}
```

# Example

## Java

```java
class StateExtensionExampleTest {

    private static final String TEST_URL = "/test";
    private static final Store<String, Object> store = new CaffeineStore();
    private static final ObjectMapper mapper = new ObjectMapper();

    @RegisterExtension
    public static WireMockExtension wm = WireMockExtension.newInstance()
        .options(
            wireMockConfig().dynamicPort().dynamicHttpsPort()
                .extensions(new StateExtension(store))
        )
        .build();


    private void createPostStub() throws JsonProcessingException {
        wm.stubFor(
            post(urlEqualTo(TEST_URL))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            mapper.readTree(
                                mapper.writeValueAsString(Map.of("id", "{{randomValue length=32 type='ALPHANUMERIC' uppercase=false}}")))
                        )
                )
                .withServeEventListener(
                    "recordState",
                    Parameters.from(
                        Map.of(
                            "context", "{{jsonPath response.body '$.id'}}",
                            "state", Map.of(
                                "id", "{{jsonPath response.body '$.id'}}",
                                "firstName", "{{jsonPath request.body '$.contextValue'}}",
                                "lastName", "{{jsonPath request.body '$.contextValue'}}"
                            )
                        )
                    )
                )
        );
    }

    private void createGetStub() throws JsonProcessingException {
        wm.stubFor(
            get(urlPathMatching(TEST_URL + "/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            mapper.readTree(
                                mapper.writeValueAsString(Map.of(
                                        "id", "{{state context=request.pathSegments.[1] property='id'}}",
                                        "firstName", "{{state context=request.pathSegments.[1] property='firstName'}}",
                                        "lastName", "{{state context=request.pathSegments.[1] property='lastName'}}"
                                    )
                                )
                            )
                        )
                )
        );
    }
}
```

## JSON

### `POST`

```json
{
  "request": {
    "method": "POST",
    "url": "/test",
    "headers": {
      "content-type": {
        "contains": "json"
      },
      "accept": {
        "contains": "json"
      }
    }
  },
  "response": {
    "status": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "jsonBody": {
      "id": "{{randomValue length=32 type='ALPHANUMERIC' uppercase=false}}",
      "firstName": "{{jsonPath request.body '$.firstName'}}",
      "lastName": "{{jsonPath request.body '$.lastName'}}"
    }
  },
  "withServeEventListener": [
    {
      "name": "recordState",
      "parameters": {
        "context": "{{jsonPath response.body '$.id'}}",
        "state": {
          "id": "{{jsonPath response.body '$.id'}}",
          "firstName": "{{jsonPath request.body '$.firstName'}}",
          "lastName": "{{jsonPath response.body '$.lastName'}}"
        }
      }
    }
  ]
}
```

### `GET`

```json
{
  "request": {
    "method": "GET",
    "urlPattern": "/test/[^\/]+",
    "headers": {
      "accept": {
        "contains": "json"
      }
    }
  },
  "response": {
    "status": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "jsonBody": {
      "id": "{{state context=request.pathSegments.[1] property='id'}}",
      "firstName": "{{state context=request.pathSegments.[1] property='firstName'}}",
      "lastName": "{{state context=request.pathSegments.[1] property='lastName'}}"
    }
  }
}
```

