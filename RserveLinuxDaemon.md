# Few steps to enable Rserve daemon at startup of your Linux server #

  1. install R from http://cran.r-project.org (tested with 2.9)
  1. install Rserve as user:
    1. get Rserve 0.6 from rforge: `wget http://www.rforge.net/src/contrib/Rserve_0.6-0.tar.gz`
    1. install and compile it (need r-base-dev, not only r-base installed) `R CMD INSTALL Rserve_0.6-0.tar.gz`
  1. create `Rserve.sh` startup script in `/home/user/R/x86_64-pc-linux-gnu-library/2.9/Rserve`:
```
#!/bin/bash
/usr/bin/R CMD Rserve --vanilla --RS-conf Rserve.conf
```
  1. check that `Rserve.sh` is executable, try to launch it and verify Rserve is available (`ps -u user | grep Rserve` should return something, then kill Rserve)
  1. create `Rserve.conf` file in `/home/user/R/x86_64-pc-linux-gnu-library/2.9/Rserve`
```
workdir tmp
remote enable
fileio enable
```
  1. create as root `/etc/init.d/Rserved`
```
#!/bin/sh

echo " * Launching Rserve daemon ..."

RSERVE_HOME=/home/user/R/x86_64-pc-linux-gnu-library/2.9/Rserve

start-stop-daemon --start --chdir $RSERVE_HOME --chuid user --exec $RSERVE_HOME/Rserve.sh > /var/log/Rserve.log 2>&1 &
echo " * Rserve daemon running ..."

exit 0
```
  1. check that `Rserved` is executable, try to launch it and verify Rserve is available (`ps -u user | grep Rserve` should return something, then kill Rserve)
  1. link Rserved in /etc/rc2.d (for ubuntu, maybe other /etc/rc.d for others linux distribution): as root `cd /etc/rc2.d; ln -s ../init.d/Rserved S99Rserved`
  1. reboot your server to verify Rserve was correclty launched at startup.
Now, you can use this linux server as a backend computing engine for your Java applications on desktop.
```
Rsession s = Rsession.newRemoteInstance(System.out,RserverConf.parse("R://myLinuxServer"));
HashMap<String,Object> vars = new HashMap<String,Object>();
vars.put("a",1.0);
vars.put("b",1.0);
double[] rand = (double[]) s.eval("rnorm(10,a,b)",vars);
```