
# bserver
Is an ip camera emulator. It is capable to serve 10k connections with 40 gbps total bandwidth.
It can stream h264 video files stored in matroska format and procedural-generated pictures.

#### Usage:
Install software and start server:
```
$ bserver -c server.yaml

time      | client connections | errors       | throughput 
=========================================================
16:13:00  | 4000 [+0; -0]      | 0            | 17.6 GiB   
```

Now server is ready to stream video. Launching *ffplay* to view video: 

```shell script
ffplay -rtsp_transport tcp  rtsp://localhost:5000/picture
```

When new client is connected you'll see ```+1``` here, in connections column:
```
time      | client connections | errors       | throughput 
=========================================================
16:14:00  | 1 [+1; -0]         | 0            | 1.6 MB
            |   |   |
            |   |   +-- lost connections during the past second
            |   +------ new connections during the past second            
            +---------- total connections   
```

Value in a column ```errors``` increments when server is unable to send data chunk just in time. It happens when network is not fast enough to process huge traffic amount.


##### Settings:
| Parameter | Description | default value|
| ---|----|----|
| network.bind | socket addresses on which the server is listening  | 0.0.0.0:5000 |
| network.sndbuf | Specifies the total per-socket buffer space reserved for sends | 131072 |
| network.threads | number of network-working threads | 1 |
| network.watermarks.high | See Netty's [WriteBufferWaterMark](https://netty.io/4.1/api/io/netty/channel/WriteBufferWaterMark.html) | 131072 |
| network.watermarks.low |   | 65536 |
| network.limits  | Limiting amount of data that can be written when socket is available for write|  |
| network.limits.packets | do not send more than X packets| 10 packets |
| network.limits.size | do not send more than X bytes | 131072 bytes|
| network.limits.time | do not send more than X milliseconds | 200 ms |
| streaming | Streaming source configuration | 2 sources: from filesystem (*.mkv) and from generated video |
| streaming.file.class | Streaming source java class | FileMediaPacketSource |
| streaming.file.conf.basedir | default directory | ${application.directory}\video_samples |
| streaming.file.conf.file | default video file | jellyfish-5-mbps-hd-h264.mkv 
| streaming.picture.class | Streaming source java class | SimplePictureSource |
| streaming.picture.conf.picture.width | picture width| 640 
| streaming.picture.conf.picture.height | picture height | 480
| streaming.picture.conf.encoder.bitrate | h264 encoder bitrate | 400000
| streaming.picture.conf.encoder.fps | frames per second | 25
| streaming.picture.conf.encoder.gop_size | GOP size | 10
| streaming.picture.conf.encoder.max_b_frames | number of b-frames in GOP | 1

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

Any streaming source configuration parameter can be overriden with URI parameter, like this:

| URL                                                                | Description                                                      |
| -------------------------------------------------------------------|------------------------------------------------------------------|
| rtsp://localhost:5000/picture                                      | procedural-generated picture with default parameters              |
| rtsp://localhost:5000/picture?picture.width=320&picture.height=240 | same with specific dimensions                                     |
| rtsp://localhost:5000/picture?encoder.fps=60                       | same with specific fps                                                     |
| rtsp://localhost:5000/file                                         | streaming default file: ```video_samples\jellyfish-5-mbps-hd-h264.mkv```  |
| rtsp://localhost:5000/file?file=simpsons.mkv                       | streaming specific file ```video_samples\simpsons.mkv```                   |


# bclient
RTSP client aimed to receive video from multiple connections with maximum possible throughput

#### Usage:

$ bclient -c client.yaml
```
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