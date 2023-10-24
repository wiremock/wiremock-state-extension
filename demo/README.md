# WireMock State extension Demo

<p align="center">
    <a href="https://wiremock.org" target="_blank">
        <img width="512px" src="https://wiremock.org/images/logos/wiremock/logo_wide.svg" alt="WireMock Logo"/>
    </a>
</p>

This is a demonstration of the WireMock State Extension, showcasing its initial feature set and usage examples.

## Initial Feature Set: The State

- Creating, updating, and deleting a state.
- Storing different states in association with a context.
- Request matcher to check for the existence of a context.
- Handlebars support.

## State

A context can have multiple properties. Each property can be overwritten individually.

### Context

The parent container of a state. Multiple contexts can exist.

### Example: Modeling Queues

- Adding new (unmodifiable) states to the context.
- Insert as the first or last item.
- Read access to the first/last/index.
- Delete first/last/index.
- Check size.
- Suggested by @ioanngolovko.



### StateList

### Example: Improving Usability

- Allow defining default values (suggested by @alexandre-chopin).
- Expose the "list" to Handlebars.
- Allow building listing endpoints.
- Query "special" properties: updateCount, listSize, list, property existence.
- Delete single properties.
- Loadable in WireMock standalone and Docker container.

<p align="center">
    <a>      
        <img width="512px" src="demo/Images/Example1.png" alt="Example1"/>
    </a>
</p>


<p align="center">
    <a>      
        <img width="512px" src="demo/Images/Example2.png" alt="Example2"/>
    </a>
</p>



## Getting Started

To get started with the WireMock State Extension, follow these steps:

1. [Install WireMock](https://github.com/wiremock/wiremock) if you haven't already.
2. [Install the State Extension](link-to-extension) by following the installation instructions.
3. Use the provided examples to understand how to create, manage, and interact with states.

## Usage

You can start experimenting with the WireMock State Extension by using the provided examples. Explore the capabilities of the extension for modeling queues and improving usability.

## Contributions

Contributions and feedback are welcome. If you have ideas or suggestions for improving the WireMock State Extension, feel free to contribute or share your thoughts.




