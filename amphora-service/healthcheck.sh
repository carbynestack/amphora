#!/bin/sh

#
# Copyright (c) 2021 - for information on the respective copyright owner
# see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
#
# SPDX-License-Identifier: Apache-2.0
#

_init () {
    scheme="http://"
    address="$(netstat -nplt 2>/dev/null | awk ' /(.*\/java)/ { gsub(":::","127.0.0.1:",$4); print $4}')"
    resource="/actuator/health"
}

poke () {
    http_response=$(curl -H "User-Agent: Mozilla" -s -k -o /dev/null -I -w "%{http_code}" \
        ${scheme}${address}${resource})

    [ "$http_response" = "200" ]
}

_init && poke
