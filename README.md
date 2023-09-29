# FHIR APIs Indexing with ZIO 

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Contributing](#contributing)
- [License](#license)

## Introduction

Welcome to the FHIR Indexing with ZIO and HAPI FHIR repository! This open-source project aims to provide a robust and efficient solution for indexing FHIR (Fast Healthcare Interoperability Resources) based APIs. The project leverages the power of the ZIO framework to handle concurrency and asynchronicity, while wrapping around the widely-used HAPI FHIR library for working with FHIR resources.

## Features

- **Asynchronous and Concurrent Indexing**: The project takes advantage of ZIO's powerful concurrency model to efficiently index FHIR resources from multiple APIs simultaneously.

- **Modular and Extensible Design**: The codebase is structured in a modular way, allowing easy extension and customization to fit different FHIR-based API implementations.

- **Authentication Support**: The library supports various authentication mechanisms for accessing secure FHIR endpoints, ensuring data privacy and security.

- **Error Handling**: Comprehensive error handling is implemented to provide meaningful feedback in case of issues during indexing.

- **Resource Filtering and Transformation**: Users can define custom filters and transformations to tailor the indexing process according to their specific requirements.

## Installation

### SBT

`libraryDependencies += "io.github.royashcenazi" % "fhir-indexer" % "v0.0.1"`

### Maven
```
   <dependency>
        <groupId>io.github.royashcenazi</groupId>
        <artifactId>fhir-indexer</artifactId>
        <version>v0.0.1</version>
    </dependency>
```

## Usage

See an example of usage in ```/src/main/scala/com.scalahealth.fhir.ZioApp```

## Contributing

We welcome contributions to the FHIR Indexing project! If you find any issues or have ideas for improvements, please feel free to open an issue or submit a pull request. Please follow our [Contribution Guidelines](./CONTRIBUTING.md) for more information on how to contribute.

## License

This project is licensed under the [MIT License](./LICENSE). You are free to use, modify, and distribute the code as per the terms mentioned in the license. 

---

We hope that our FHIR Indexing library proves to be a valuable tool in your FHIR-based projects. If you have any questions or need support, please don't hesitate to reach out to us through the issue tracker.

Happy indexing!
