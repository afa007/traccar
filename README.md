
###

```
java -cp /root/soft/h2/h2-1.4.193.jar org.h2.tools.Server -web -webPort 9081 -webAllowOthers -tcp -tcpPort 9082 -tcpAllowOthers -baseDir /root/github/traccar/data

nohup java -cp tracker-server-jar-with-dependencies.jar org.traccar.Main ./setup/traccar.xml &

java -cp tracker-server-jar-with-dependencies.jar org.traccar.Main ./setup/traccar.xml

```