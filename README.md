# yamcs-gstreamer

**yamcs-gstreamer** is a YAMCS (Yet Another Mission Control System) plugin that integrates GStreamer pipelines into YAMCS. This plugin allows users to add, manage, and control multiple GStreamer pipelines directly from YAMCS, enabling sophisticated media processing and streaming capabilities within a mission control system.

## Features

- **Multiple Pipeline Support:**  
  Configure and run multiple GStreamer pipelines concurrently within YAMCS.
  
- **Element & Property Management:**  
  Easily view, read, and modify properties of GStreamer elements using YAML serialization and custom actions.
  
- **YAMCS Integration:**  
  Seamless integration with YAMCS telemetry and system parameters, enabling you to monitor and control media streams alongside mission-critical data.

- **Future Enhancements:**  
  - **Archive Playback Integration:** Integrate YAMCS with the archive playback system to enable video playback synchronized with telemetry data.
  - **Embedded Telemetry:** Optionally embed telemetry directly within recorded video/audio frames for richer context in media streams.

## Getting Started

### Prerequisites

- **YAMCS:**  
  [Insert YAMCS version requirements and links to YAMCS documentation here.]

- **GStreamer:**  
  [Insert GStreamer version requirements and installation instructions here.]

- **Java:**  
  [Insert Java version and any additional prerequisites here.]

### Installation

[//]: # (TODO: Provide detailed installation instructions, including how to add the plugin to your YAMCS installation, Maven coordinates (if applicable), and any configuration steps.)

1. Download or clone the repository.
2. Build using Maven with `mvn clean install`
3. Copy the target/yamcs-gstreamer-<version>.jar to the YAMCS lib directory.
3. Configure YAMCS to load the plugin as described in [Configuration](#configuration).

## Usage

[//]: # (TODO: Provide detailed usage instructions with examples on how to configure and use the plugin within YAMCS.)

- **Configuring Pipelines:**  
  Define one or more GStreamer pipelines in your YAMCS configuration file. Each pipeline can be controlled via YAMCS actions.
  
- **Available Actions:**  
  - Set active pipeline (1 per instance)
  - Dump element factory names to log.
  - Dump element properties to log.
  - Read element property value and dump to log using a path-like notation.
  - Write element properties using a path-like notation.
  - Set active pipelines.
  
NOTE: Some GStreamer plugins are not quite fully compliant with the GStreamer API and requirements.  Some of these Actions may not work, may generate errors and may throw exceptions.  The plugin is designed to be resilient to this, generate an error message, but continue running.

## Configuration

This is a "datalink" plugin so configuration goes into the "dataLinks" section of the YAMCS instance YAML configuration file.  Below is a sample configuration.

```yaml
dataLinks:
  - name: video-pipeline1
    class: com.windhoverlabs.yamcs.media.GStreamerLink
    activePipeline: "Test Pattern SMPTE local"
    pipelines:
      - name: "Test Pattern SMPTE local"
        description: "videotestsrc name=videotestsrc0 pattern=smpte	! autovideosink"
      - name: "Test Pattern SMPTE to Lorenzo"
        description: "videotestsrc name=videotestsrc0 pattern=smpte	! x264enc name=encoder0 bitrate=2000 speed-preset=superfast tune=zerolatency key-int-max=30 insert-vui=true ! mpegtsmux name=muxer0 latency=0 ! rtpmp2tpay name=rtpmp2tpay0 ! udpsink name=udpsink0 host=172.16.100.208 port=5004 async=true "
      - name: "Test Pattern Checkers-8"
        description: "videotestsrc name=videotestsrc0 pattern=checkers-8        ! autovideosink"
      - name: "Test Pattern Ball"
        description: "videotestsrc name=videotestsrc0 pattern=ball              ! autovideosink"
    telemetry:
      - path: udpsink0/host
      - path: udpsink0/port
      - path: udpsink0/async
      - path: videotestsrc0/blocksize
      - path: videotestsrc0/num-buffers
      - path: videotestsrc0/typefind
      - path: videotestsrc0/do-timestamp
      - path: videotestsrc0/pattern
      - path: videotestsrc0/timestamp-offset
      - path: autovideosink0-actual-sink-xvimage/stats/average-rate
      - path: autovideosink0-actual-sink-xvimage/stats/dropped
```

name
: This is the unique name you give to the plugin instance.

class
: This must be "com.windhoverlabs.yamcs.media.GStreamerLink"

activePipeline
: This is the name of the pipeline to activate at startup. 

### "pipeline" section
name
: This is the name of the pipeline.  This will appear in YAMCS Web and is used to activate pipelines at runtime.

description
: This is the gstreamer pipeline description.  See [GStreamer documentation](https://gstreamer.freedesktop.org/documentation/tools/gst-launch.html) for more information.

### "telemetry" section
Telemetry defined in this section will be displayed in YAMCS Web in both the "Links" display as well as the "Telemetry" display.  All telemetry will be added
to the telemetry list, but will only be updated when the parameters are available.  It is possible that some parameters are not available with the currently active pipeline.  When this happens, the telemetry will go static.  No error will be thrown.  Which telemetry is updated depends on the telemetry defined and the pipeline active.

path
: This is the path to the parameter in the form "elementname/propertyname[/subproperty...]"


## Development

[//]: # (TODO: Include instructions for developers who wish to build or contribute to the project.)

- Clone the repository.
- Build with maven.
- Copy the .jar file to the YAMCS lib directory.  You can also add a symbolic link from the YAMCS lib directory to the target directory to simplify development.

## Future Work

- **Archive Playback Integration:**  
  Integrate with YAMCS archive playback system for synchronized video and telemetry.
  
- **Embedded Telemetry:**  
  Add functionality to embed telemetry data directly within the recorded video/audio streams.

## License

This project is licensed under the BSD 3-Clause License.

## Acknowledgements

- Thanks to the YAMCS community for their support.
- Thanks to the maintainers of GStreamer and SnakeYAML for their excellent libraries.
- Special thanks to Lorenzo for the sample code and remote testing.
