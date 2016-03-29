# UK federation Metadata Distribution Service

This is the metadata distribution service for the [UK Access Management Federation for Education and Research](http://ukfederation.org.uk) ("the UK federation").

Its job is to source new metadata from a master service and republish it after validating the embedded XML signatures. A failure to validate any of the signatures in a set of metadata files results in the update being abandoned; all signatures must validate before an update is performed.

As well as a version that runs in the Federation's current production environment, the repository includes a [Docker](https://www.docker.com) build for use in development or future deployments. The `*-docker` scripts are used to build and execute this version.

Operation of the main `fetchmeta` script can be influenced by creating a `CONFIG` file. This is not stored in the repository, but is often linked to either `CONFIG-test` or `CONFIG-docker` for development use.

## Licensing

Everything in this repository is Copyright (C) 2016, University of Edinburgh. Each file is made available to you under the following terms:

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

> <http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
