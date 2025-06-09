# OnDemand Cloud based Jenkins Instance
## Goal
A developer should be able to start a Jenkins instance somewhere in the cloud and execute some jobs on this instance.
After the instance is going to be idle for a certain time, automagically a snapshot of this instance should be taken and the instance should be taken down and killed to reduce cost.
Next time a developer is starting the instsnce, the snapshot is used to create one.

## Realization
[Hetzner Cloud](https://www.hetzner.com/cloud) and the [APIs of Hetzner Cloud](https://docs.hetzner.cloud/) are used to make it real.

There is a script to [start a Jenkins](start-jenkins.sh) instance from a formerly saved snapshot. 
The than started jenkins instance will have a cron job running periodically, checking if Jenkins is being idel for the last 10 minutes at least and if so, it is taking a snapshot and killing itself afterwards. The script running by cron is [killmyself.sh](killmyself.sh).

## How to utilize the OnDemand Cloud Jenkins
Before the [start-jenkins.sh](start-jenkins.sh) can be used, an environment varibale `CLOUD_API_TOKEN` has to be set with the API token. This could be done in your `.bashrc` the following way:
```shell
export CLOUD_API_TOKEN='<the token>'
```
Starting the Jenkins will take some seconds. After it is started successfully, it can be reached by OpenVPN under http://10.9.0.1:8080.

The auto kill cronjob will write a log. By reading this log you can determine the status of the jenkins instance, if it is busy, idle or in the phase of shutting down.
The log can be shown this way:
```shell
ssh -o "StrictHostKeyChecking=no" root@10.9.0.1 'tail -f /var/log/killmyself.log'
```

If the automatic kill should not happen, the following command can be used to switch off the kill job:
```shell
ssh -o "StrictHostKeyChecking=no" root@10.9.0.1 "crontab -l | sed -r 's/^(\*.*\/root\/killmyself\.sh)/# \1/g' | crontab"
```

Don't forget to remove this `#` again, so the instance can be killed and doesn't cost money anymore.
```shell
ssh -o "StrictHostKeyChecking=no" root@10.9.0.1 "crontab -l | sed -r 's/^# (\*.*\/root\/killmyself\.sh)/\1/g' | crontab"
```
