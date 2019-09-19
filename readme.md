Bstreamer is a tool for load testing video streaming applications


### bserver
An application for client load testing

Usage:
```
$ bserver -c server.yaml

time      | client connections | errors       | throughput 
=========================================================
16:13:00  | 4000 [+0; -0]      | 0            | 17.6 GiB   
```
Server config example:
```
network:
  bind: ["0.0.0.0:5000"]
  sndbuf: 131072
  threads: 1
  watermarks:
    high: 131072
    low: 65536
  limits:
    packets: 10
    size: 131072
    time: 200
streaming:
  file:
    class: me.vzhilin.bstreamer.server.streaming.FileMediaPacketSource
    conf:
      basedir: video_samples
      file: jellyfish-5-mbps-hd-h264.mkv
  picture:
    class: me.vzhilin.bstreamer.server.streaming.SimplePictureSource
    conf:
      picture:
        width: 640
        height: 480
      encoder:
        bitrate: 400000
        fps: 25
        gop_size: 10
        max_b_frames: 1
```

### bclient
An application for server load testing

Usage:
```
$ bclient -c client.yaml

time      | server connections | errors       | throughput 
=========================================================
16:13:00  | 4000 [+0; -0]      | 0            | 17.6 GiB 
```

Client config example:
```
network:
  threads: 4
  rcvbuf: 131072
  connectTimeout: 5000
  idleTimeout: 5000
connections:
 - url: rtsp://localhost:5000/file?file=jellyfish-5-mbps-hd-h264.mkv
   n: 2000
```