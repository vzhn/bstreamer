
![bee](site/bee_256.png)
 
 *\*logo by [Mushroomova](https://www.instagram.com/mushroomova_comics/)*

**bstreamer** is a collection of tools intended to help load testing video streaming applications.


## bserver
Is an ip camera emulator. It can stream h264 video files stored in matroska format and live-generated pictures. It is capable to serve 10k connections with up to 40 gbps total bandwidth.


Videofiles must be placed to the ```video``` application directory. You can find some nice sample files on this website: http://jell.yfish.us/ 

#### Usage:
Install software and start server:
```
$ bserver -c server.yaml

time      | client connections | errors       | throughput 
=========================================================
16:13:00  | 4000 [+0; -0]      | 0            | 17.6 GiB   
```

Now server is ready to stream video.  

```shell script
ffplay -rtsp_transport tcp  rtsp://localhost:8554/picture
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
| Parameter | Description | Default |
| ---|----|----|
| network.bind | socket addresses on which the server is listening  | 0.0.0.0:8554 |
| network.sndbuf | Specifies the total per-socket buffer space reserved for sends | 131072 |
| network.threads | number of network-working threads | 1 |
| network.watermarks.high | See Netty's [WriteBufferWaterMark](https://netty.io/4.1/api/io/netty/channel/WriteBufferWaterMark.html) | 131072 |
| network.watermarks.low |   | 65536 |
| network.limits  | Limiting amount of data that can be written when socket is available for write|  |
| network.limits.packets | packet limit | 10 packets |
| network.limits.size | bytes limit | 131072 bytes|
| network.limits.time | chunk length limit | 200 ms |
| streaming | stream  configuration | 2 sources: from filesystem (*.mkv) and from generated video |
| streaming.file.repeat | true if repeat video | true |
| streaming.file.class | filesystem source java class | Generated |
| streaming.file.conf.basedir | default directory | ${application.directory}\video |
| streaming.file.conf.file | default video file | jellyfish-5-mbps-hd-h264.mkv 
| streaming.picture.class | Streaming source java class | Generated |
| streaming.picture.conf.picture.width | picture width| 640 
| streaming.picture.conf.picture.height | picture height | 480
| streaming.picture.conf.encoder.profile | h264 encoder profile | 
| streaming.picture.conf.encoder.bitrate | h264 encoder bitrate | 400000
| streaming.picture.conf.encoder.fps | frames per second | 25
| streaming.picture.conf.encoder.gop_size | GOP size | 10
| streaming.picture.conf.encoder.max_b_frames | number of b-frames in GOP | 1

##### server.yaml:

```
network:
  bind: ["0.0.0.0:8554"]
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
    class: Filesystem
    conf:
      repeat: true
      basedir: video
      file: jellyfish-5-mbps-hd-h264.mkv
  picture:
    class: Generated
    conf:
      picture:
        width: 640
        height: 480
      encoder:
        profile: "baseline"
        bitrate: 400000
        fps: 25
        gop_size: 10
        max_b_frames: 1
```

Any streaming source configuration parameter can be overridden by URI parameter, like this:

| URL                                                                | Description                                                      |
| -------------------------------------------------------------------|------------------------------------------------------------------|
| rtsp://localhost:8554/picture                                      | procedural-generated picture with default parameters              |
| rtsp://localhost:8554/picture?picture.width=320&picture.height=240 | same with specific dimensions                                     |
| rtsp://localhost:8554/picture?encoder.fps=60                       | same with specific fps                                                     |
| rtsp://localhost:8554/file                                         | streaming default file: ```video_samples\jellyfish-5-mbps-hd-h264.mkv```  |
| rtsp://localhost:8554/file?file=simpsons.mkv                       | streaming specific file ```video_samples\simpsons.mkv```                   |


## bclient
RTSP client aimed to receive video from multiple connections with maximum possible throughput

#### Usage:

```
$ bclient -c client.yaml

time     | server connections   | errors      | lost packets | throughput 
=========================================================================
12:58:28 | 4000 [+0:-0]         | 0 [+0]      | 0 [+0]      | 1.4 GiB    
```

##### Settings:
| Parameter | Description | Default |
| ---|----|----|
| network.threads | number of network-working threads | 4 |
| rcvbuf | socket receive buffer size | 131072 |
| connectTimeout | connect timeout | 5 seconds |
| idleTimeout | maximum connection idle timeout | 5 seconds |
| connections.url | rtsp url | rtsp://localhost:8554/file?file=jellyfish-5-mbps-hd-h264.mkv |
| connections.n | number of connections | 2000 |

##### client.yaml:
```
network:
  threads: 4
  rcvbuf: 131072
  connectTimeout: 5000
  idleTimeout: 5000
connections:
 # 2000 connections to video from a file
 - url: rtsp://localhost:8554/file?file=jellyfish-5-mbps-hd-h264.mkv
   n: 2000
# 2000 connections to live generated picture
 - url: rtsp://localhost:8554/picture
   n: 2000
```