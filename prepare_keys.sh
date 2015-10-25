#!/bin/sh
sed -i -b s/"db-n4li1bbi9l4rn49"/"db-$1"/ AndroidManifest.xml
sed -i -b s/"n4li1bbi9l4rn49"/"$1"/ src/com/flowzr/export/dropbox/Dropbox.java
sed -i -b s/"nn94nymigkkbhgk"/"$2"/ src/com/flowzr/export/dropbox/Dropbox.java
echo "Done, don't forget to call bzr revert after building apk"