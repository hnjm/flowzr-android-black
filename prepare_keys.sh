#!/bin/sh
sed -i -b s/"db-n4li1bbi9l4rn49"/"db-your_drop_box_account_id"/ AndroidManifest.xml
sed -i -b s/"n4li1bbi9l4rn49"/"your_drop_box_account_id"/ src/com/flowzr/export/dropbox/Dropbox.java
sed -i -b s/"nn94nymigkkbhgk"/"your_drop_box_account_key"/ src/com/flowzr/export/dropbox/Dropbox.java
sed -i -b s/"AIzaSyBD06B06eal5OrFtJwi58Zqsg_wsRq8jpo"/"your_gmap_key"/ AndroidManifest.xml

echo "Done, hidden keys from store"