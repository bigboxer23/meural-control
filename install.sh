#!/usr/bin/env bash
host=[your host/IP here]

mvn package
ssh -t pi@$host -o StrictHostKeyChecking=no "mkdir /home/pi/com"
ssh -t pi@$host -o StrictHostKeyChecking=no "mkdir /home/pi/com/bigboxer23"
ssh -t pi@$host -o StrictHostKeyChecking=no "mkdir /home/pi/com/bigboxer23/meural-control"
ssh -t pi@$host -o StrictHostKeyChecking=no "mkdir /home/pi/com/bigboxer23/meural-control/1.0"
scp -o StrictHostKeyChecking=no -r scripts/meural.service pi@$host:/lib/systemd/system
ssh -t pi@$host -o StrictHostKeyChecking=no "sudo systemctl daemon-reload"
ssh -t pi@$host -o StrictHostKeyChecking=no "sudo systemctl enable meural.service"
#This can transfer your google auth tokens directory to a remote server which can't directly run the OAuth flow
#scp -o StrictHostKeyChecking=no -r tokens pi@$host:/home/pi/com/bigboxer23/meural-control/1.0