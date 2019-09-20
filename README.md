## Motivation
Imagine that you need to develop video streaming software that is capable to receive and send video from thousands ip cameras to
thousands video clients. You do not have so many ip cameras so you need piece of software to replace it. 

**Bstreamer** is a tool for load testing video streaming applications. Supported transport: RTSP over TCP (interleaved mode)

* **bserver** is RTSP server
* **bclient** is RTSP client

## bserver
Is capable to serve 10k connections with 40 gbps total bandwidth. 
It can stream h264 video files stored in matroska format and procedural-generated pictures.

#### Usage:
```
$ bserver -c server.yaml

time      | client connections | errors       | throughput 
=========================================================
16:13:00  | 4000 [+0; -0]      | 0            | 17.6 GiB   
```
##### server.yaml:
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
    class: FileMediaPacketSource
    conf:
      basedir: video_samples
      file: jellyfish-5-mbps-hd-h264.mkv
  picture:
    class: SimplePictureSource
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

## bclient
RTSP client aimed to receive video from multiple connections with maximum possible throughput

#### Usage:
```
$ bclient -c client.yaml

time      | server connections | errors       | throughput 
=========================================================
16:13:00  | 4000 [+0; -0]      | 0            | 17.6 GiB 
```

##### client.yaml:
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