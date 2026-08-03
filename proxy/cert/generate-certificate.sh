#!/bin/bash

openssl req -x509 -days 365 -nodes -batch -newkey rsa:4096 \
  -out proxy.crt \
  -keyout proxy.key \
  -config proxy.conf
