
#/bin/bash

curl -vk -X GET https://public.michir.aws.maileva.net:8443/auth/realms/demo/protocol/openid-connect/userinfo -H "Authorization: Bearer $1"
