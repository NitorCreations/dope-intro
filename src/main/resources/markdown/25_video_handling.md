# Video handling #

 * The build system finds video elements at compile time
    * Records the location and size of the placeholder image
 * At runtime during presentation initialization the engine extracts the video into a temporary file
 * As you traverse past the slide with the video, the presentation zooms in on the placeholder and starts an external player
    * ```videoplayer {video}```
    * on my Raspberry Pi this is ```/usr/bin/videoplayer```

```bash
#!/bin/bash

exec /usr/bin/omxplayer "$@"
```

