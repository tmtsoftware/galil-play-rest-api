#curl --header "Content-type: application/json" --request POST --data @axis-default.json http://localhost:9000/v1/gs/setConfig?axis=A -v -s
#sleep 3s
#curl --header "Content-type: application/json" --request POST --data @axis-default.json http://localhost:9000/v1/gs/setConfig?axis=B -v -s
#sleep 3s
#curl --header "Content-type: application/json" --request POST --data @axis-default.json http://localhost:9000/v1/gs/setConfig?axis=C -v -s
#sleep 3s
curl --header "Content-type: application/json" --request POST --data @axis-default.json http://localhost:9000/v1/gs/setConfig?axis=D -v -s
sleep  3s
curl --header "Content-type: application/json" --request POST --data @axis-default.json http://localhost:9000/v1/gs/setConfig?axis=E -v -s
sleep 3s
curl --header "Content-type: application/json" --request POST --data @axis-default.json http://localhost:9000/v1/gs/setConfig?axis=F -v -s
sleep 3s
curl --header "Content-type: application/json" --request POST --data @axis-default.json http://localhost:9000/v1/gs/setConfig?axis=G -v -s
sleep 3s
curl --header "Content-type: application/json" --request POST --data @axis-default.json http://localhost:9000/v1/gs/setConfig?axis=H -v -s
