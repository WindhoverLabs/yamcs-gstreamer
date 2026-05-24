# Requirements

## Purpose

This document defines the required behavior of `yamcs-gstreamer` as it exists today and the intended contract that future maintenance and feature work shall preserve unless an explicit change request updates this file.

The requirements are written for:

- Software developers extending or refactoring the plugin
- Agentic LLMs maintaining the repository
- Future test authors creating unit, component, and integration tests

Each requirement is written to be observable and, where practical, verifiable by automated tests.

## Scope

`yamcs-gstreamer` is a YAMCS datalink plugin that embeds GStreamer pipeline control into YAMCS. A single plugin instance manages exactly one active GStreamer pipeline at a time, while allowing multiple named pipeline definitions to be configured for later activation. The current maintained compatibility target is YAMCS `5.13.0`.

## Definitions

- Link instance: One configured instance of `com.windhoverlabs.yamcs.media.GStreamerLink`
- Pipeline definition: A configured pair of pipeline `name` and GStreamer launch `description`
- Active pipeline: The pipeline currently instantiated and running for a link instance
- Telemetry path: A slash-delimited path of the form `elementName/propertyName[/subproperty...]`
- Action: A YAMCS link action exposed to operators through YAMCS APIs or UI

## System Requirements

### SR-001 Repository Shape

- The project shall remain a Java Maven project that produces a YAMCS plugin artifact.
- The main runtime entry point shall remain `com.windhoverlabs.yamcs.media.GStreamerLink` or a documented replacement with equivalent behavior.

Verification:
- Build metadata inspection
- Packaging/integration test

### SR-002 Supported Runtime Role

- The plugin shall operate as a YAMCS datalink configured under the `dataLinks` section of a YAMCS instance configuration.

Verification:
- Configuration loading test
- YAMCS plugin integration test

## Configuration Requirements

### CR-001 Link Configuration Schema

A link instance configuration shall support these top-level fields:

- `name` as a required string
- `class` as a required string
- `activePipeline` as an optional string
- `pipelines` as an optional list of pipeline-definition maps
- `telemetry` as an optional list of telemetry-definition maps

Verification:
- Spec validation test

### CR-002 Link Class Name

- The configured `class` value shall be `com.windhoverlabs.yamcs.media.GStreamerLink`.

Verification:
- Documentation and configuration validation test

### CR-003 Pipeline Definition Schema

Each pipeline definition shall support:

- `name` as a string identifier intended for operator display and selection
- `description` as a string containing a valid GStreamer launch description

Verification:
- Spec validation test

### CR-004 Telemetry Definition Schema

Each telemetry definition shall support:

- `path` as a required string in telemetry-path format
- `name` as an optional display name

Verification:
- Spec validation test

### CR-005 Multiple Definitions, Single Active Instance

- A link instance may contain zero or more configured pipeline definitions.
- A link instance shall manage at most one instantiated pipeline at any moment.
- Running multiple pipelines simultaneously shall require multiple link instances.

Verification:
- Unit test for link state
- Integration test with multiple configured definitions

## Lifecycle Requirements

### LR-001 GStreamer Initialization

- Link initialization shall initialize the GStreamer Java binding before any pipeline is started.
- If GStreamer initialization fails, link initialization shall fail with a configuration error.

Verification:
- Unit test with failing GStreamer initialization seam

### LR-002 Start Behavior

When the link starts, it shall:

- Register its supported YAMCS actions
- Transition to a started state visible to YAMCS
- Become eligible to start its configured active pipeline

Verification:
- Unit test on `doStart()`
- Integration test through YAMCS link lifecycle

### LR-003 Active Pipeline Startup

- If `activePipeline` is configured and names a known pipeline definition, the link shall start that pipeline during enable/startup.
- If no active pipeline is configured, the link shall remain operational as a plugin but shall not instantiate a pipeline until one is selected.

Verification:
- Unit test with configured and unconfigured startup cases

### LR-004 Enable Behavior

- Enabling the link shall attempt to start the currently selected active pipeline.

Verification:
- Unit test on `doEnable()`

### LR-005 Disable Behavior

- Disabling the link shall stop and dispose the currently instantiated pipeline, if any.
- Disabling the link shall not remove configured pipeline definitions.

Verification:
- Unit test on `doDisable()`

### LR-006 Stop Behavior

Stopping the link shall:

- Stop the active pipeline if one exists
- Release GStreamer resources owned by the link
- Notify YAMCS that the link has stopped

Verification:
- Unit test on `doStop()`
- Integration shutdown test

### LR-007 Stop Wait Policy

- When stopping a pipeline, the implementation shall wait for the pipeline to reach the GStreamer `NULL` state before disposing it.
- The implementation may apply a timeout to avoid indefinite blocking.
- Timeout expiration shall result in a warning rather than process termination.

Verification:
- Unit test with mocked pipeline states

## Pipeline Management Requirements

### PR-001 Pipeline Lookup

- Pipeline activation shall be driven by pipeline definition name.
- If a requested pipeline name does not match any configured definition, the link shall not create a new pipeline and shall emit an operator-visible diagnostic.

Verification:
- Unit test for missing pipeline selection

### PR-002 Replacement Semantics

- Starting a new pipeline while another pipeline is active shall stop and dispose the existing pipeline before the replacement pipeline is created.

Verification:
- Unit test with ordered interaction assertions

### PR-003 Pipeline Construction

- A pipeline shall be created from the configured GStreamer launch description using the GStreamer parser.
- Failure to create the pipeline from a description shall raise a configuration error.

Verification:
- Unit test with parser seam

### PR-004 Pipeline State Start

- After creation and bus setup, a selected pipeline shall be transitioned to playback.

Verification:
- Unit test asserting `play()` invocation

### PR-005 Link Status Mapping

The link shall expose status according to the active pipeline state:

- No instantiated pipeline: `DISABLED`
- GStreamer `PLAYING`: `OK`
- GStreamer `PAUSED`: `DISABLED`
- GStreamer `NULL`: `FAILED`
- Any other state: `UNAVAIL`

Verification:
- Unit test of `connectionStatus()`

### PR-006 Detailed Status String

- The link shall expose a human-readable detailed status string containing the selected pipeline name or a no-active-pipeline marker, plus the current runtime state or `NOT RUNNING`.

Verification:
- Unit test of `getDetailedStatus()`

## Telemetry Requirements

### TR-001 Telemetry Registration

For each configured telemetry definition, the link shall register one YAMCS system parameter with:

- Parameter path `links/<linkName>/<telemetry.path>`
- Value type `STRING`
- Display name equal to `telemetry.name` when provided, otherwise `telemetry.path`
- Long description equal to `telemetry.path`

Verification:
- Unit test of `setupSystemParameters()`

### TR-002 Telemetry Collection Preconditions

- Telemetry collection shall not require an active pipeline to succeed as a method call.
- If no pipeline is active, the link shall return no pipeline-derived telemetry values.

Verification:
- Unit test of `getSystemParameters()` with `pipeline == null`

### TR-003 Telemetry Value Source

- For each registered telemetry parameter and active pipeline, the runtime value shall be obtained by resolving the parameter's telemetry path against the active pipeline.

Verification:
- Unit test with mocked path resolver

### TR-004 Telemetry Publication Rule

- A telemetry parameter value shall be published only when a non-null path-resolution result is available.

Verification:
- Unit test for null and non-null path results

### TR-005 Static Telemetry on Missing Data

- If a telemetry path is not available in the currently active pipeline, the plugin shall not crash.
- The absence of updates for that telemetry path shall be tolerated as normal runtime behavior.

Verification:
- Unit/integration test with unavailable element or property

## Telemetry Path Requirements

### TP-001 Path Grammar

- A telemetry path shall contain at least two segments separated by `/`.
- The first segment shall identify a GStreamer element by name.
- The remaining segment or segments shall identify a property path under that element.

Verification:
- Unit test of path parser behavior

### TP-002 Read Resolution

Path reads shall support:

- Direct element-property reads such as `element/property`
- Nested reads through GStreamer `Structure` values such as `element/property/subproperty`

Verification:
- Unit test of `readPropertyByPath()`

### TP-003 Read Failure Handling

- Invalid paths, missing elements, and missing properties shall not throw to normal callers.
- Read failures shall produce a diagnostic result suitable for logging or operator inspection.

Verification:
- Unit test for invalid path and missing element/property cases

### TP-004 Write Resolution

- Property writes shall target an element identified by the first path segment.
- The implementation shall support writing direct element properties.
- Nested write behavior beyond direct element properties is not required unless explicitly added and documented.

Verification:
- Unit test of `writePropertyByPath()`

### TP-005 Type Coercion for Writes

When writing a direct property, the implementation shall attempt to coerce the string input value to the property's current runtime type for at least:

- `Integer`
- `Long`
- `Boolean`
- `Float`
- `Double`
- `String`
- `Character`

Verification:
- Unit tests per supported type

### TP-006 Unsupported Write Types

- Unsupported property types or failed coercions shall not crash the link.
- The write attempt shall return a failure indication and emit a diagnostic log entry.

Verification:
- Unit test with unsupported property type

## Action Requirements

### AR-001 Action Registration Set

Each started link instance shall register these common actions:

- `DumpAllElementNamesToLog`
- `DumpElementToLog`
- `DumpAllElementsToFile`
- `DumpPropertyToLog`
- `WritePropertyByPath`

In addition, the link shall register one pipeline-activation action for each configured pipeline definition.

Verification:
- Unit test of `doStart()`

### AR-002 Pipeline Activation Action

For each configured pipeline definition:

- An action shall be registered with a unique name derived from the pipeline name
- The action shall use push-button style
- Executing the action shall attempt to start the associated pipeline

Verification:
- Unit test of `SetActivePipelineAction`

### AR-003 Dump All Element Names Action

- Executing `DumpAllElementNamesToLog` shall enumerate installed GStreamer element factories and log each factory name.

Verification:
- Unit test with enumerator seam and logger capture

### AR-004 Dump Element Action

`DumpElementToLog` shall:

- Require a `name` parameter
- Operate on the active pipeline
- Log a diagnostic if no pipeline is active
- Log a diagnostic if the requested element is absent
- Serialize and log the requested element's properties when found

Verification:
- Unit tests for all four cases

### AR-005 Dump Property Action

`DumpPropertyToLog` shall:

- Require a `path` parameter
- Operate on the active pipeline
- Log a diagnostic if no pipeline is active
- Resolve the path and log the resulting value or diagnostic

Verification:
- Unit tests for active and inactive pipeline cases

### AR-006 Write Property Action

`WritePropertyByPath` shall:

- Require `path` and `value` parameters
- Fail the action if no pipeline is active
- Attempt the write against the active pipeline
- Log the resulting value or a failure diagnostic

Verification:
- Unit tests for active and inactive pipeline cases

### AR-007 Dump All Elements To File Action

`DumpAllElementsToFile` shall:

- Require a `file` parameter
- Initialize GStreamer if needed before introspection
- Enumerate installed GStreamer element factories
- Attempt to instantiate each element factory for documentation purposes
- Write a YAML document describing discovered elements and their properties

Verification:
- Unit test with temporary file
- Integration test with real GStreamer runtime

## YAML Documentation Export Requirements

### YR-001 Element Documentation Model

The YAML export generated by `DumpAllElementsToFile` shall represent each discovered element with:

- `name`
- `longName`
- `description`
- `properties`

Verification:
- Unit test of exported YAML structure

### YR-002 Property Documentation Model

Each exported property entry shall include:

- `name`
- `type`
- `readable`
- `writable`
- `enumValues` when applicable

Verification:
- Unit test of exported YAML structure

### YR-003 Enum Capture

- If an element property is backed by a Java enum, the export shall record the type as an enum type and shall include the set of enum constant names.

Verification:
- Unit test with enum-backed property

### YR-004 Null Omission

- Null-valued fields shall be omitted from exported YAML rather than emitted as explicit nulls.

Verification:
- Unit test of `CustomRepresenter`

### YR-005 Stable Property Ordering

- In exported YAML, the bean property named `name` shall appear before other bean properties when emitted by the custom representer.

Verification:
- Unit test of `CustomRepresenter`

## Logging And Diagnostics Requirements

### DR-001 Bus Message Logging

The active pipeline's GStreamer bus shall be observed and at minimum these message classes shall be logged:

- Errors
- Warnings
- Informational messages
- Tags
- Buffering progress
- Need-context requests

Verification:
- Unit test of bus-message handler behavior

### DR-002 Resilience Over Strict Failure

- Diagnostic and introspection features shall prefer returning/logging a failure description over throwing uncaught runtime exceptions during normal operator use.
- This applies in particular to property reads, property writes, and element-inspection actions.

Verification:
- Negative-path unit tests

## Packaging Requirements

### BR-001 Maven Build

- The repository shall build with Maven.

Verification:
- CI build

### BR-002 Shaded Runtime Dependencies

- The packaged artifact shall include the Java GStreamer binding and JNA runtime needed by the plugin.

Verification:
- Build artifact inspection

### BR-003 YAMCS Bundle Output

- The build shall produce a YAMCS extension bundle suitable for deployment to a YAMCS installation.

Verification:
- Packaging integration test

## Quality Requirements

### QR-001 No Mandatory Test Fixtures in Production Code

- Runtime behavior shall not depend on test-only classes or resources.

Verification:
- Build inspection

### QR-002 Backward Compatibility Policy

- Changes to action names, configuration schema, telemetry parameter naming, or detailed status semantics shall be treated as externally visible contract changes and shall require updates to this document.

Verification:
- Code review policy

### QR-003 Future Testability

New code added to this project should preserve or improve the ability to test:

- Link lifecycle without a full YAMCS server
- Path resolution without a full live GStreamer graph
- Action behavior with mock loggers and temporary files
- Packaging through Maven-based integration tests

Verification:
- Design review
- Test implementation feasibility

## Out Of Scope

The following are not required by the current codebase and shall not be assumed without explicit new requirements:

- Simultaneous execution of multiple active pipelines within one link instance
- Archive playback synchronization
- Embedded telemetry in recorded media frames
- Typed telemetry publication beyond YAMCS `STRING` values
- Guaranteed write support for nested structure subproperties

## Traceability Notes

This document intentionally matches the current repository structure:

- `GStreamerLink` exists to satisfy lifecycle, pipeline, status, and telemetry requirements
- `GStreamerUtils` exists to satisfy path resolution and write-coercion requirements
- `actions/*` exist to satisfy operator control and introspection requirements
- `model/*` and `CustomRepresenter` exist to satisfy YAML export requirements
- `pom.xml` exists to satisfy build, shading, and YAMCS bundle requirements

Any future refactor may replace these implementation units, but the externally observable requirements above shall remain binding until revised here.
