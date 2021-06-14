# gw-app
Prerequisites : gw.properties to be configured accordingly

#############################
##      GW-APP  Setup       #
#############################

json.body.key.username=admin 			#username of installed aims
json.body.period=5				#free to update value
json.body.count=12				#free to update value
json.body.filename=data_ClientPollPram 		#do not change unless gateway having any update
json.body.extension=.json  			#do not change unless gateway having any update

gw.target.protocol=http			#do not change unless gateway having any update
gw.target.port=443				#do not change unless gateway having any update
gw.target.uri=api/v2/gw/poll 			#do not change unless gateway having any update

cs.target.protocol=http
cs.target.ip=52.141.19.179			#change aims server IP
cs.target.port:3000				#do not change unless protal having any update
cs.target.uri=aims_plus/gw/all?stationCode=all  #do not change unless protal having any update
cs.target.final=${cs.target.protocol}://${cs.target.ip}:${cs.target.port}/${cs.target.uri}		#do not change

#execution threshold : no of times to be repeated [once it finised task, it will start again to max execution.limit]
execution.limit=1				#do not change unless you want repeated operation 




Step 1 : extract folder [env] into C: Drive
Step 2 : open cmd and change directory to [env] i,e- c:/cd env
Step 3 : java -jar -Daims.root.path=C:\ api-v2-gw.jar

OPEN browse type url below:
Execution  : [CSV FILE EXECUTION STEPS] or [AUTO EXECUTION STEPS] 
Logging    : Check log inside /env/log/api-v2-gw.log


###############################
##  CSV FILE EXECUTION STEPS  #
###############################

Service-1-STEP-1 	: http://localhost:8080/csv/gw/download
Service description	: download prepared gateway list in csv format,file name would be gw_connect.csv. it may contain CONNECTED,DISCONNECTED,NOT_READY,UNREGISTERED based on download cases. 
			: it can be filter out by using excel. "gwIP","period","count" fields are required in same sequence in gw_connect.csv file.


Download case 1		: http://localhost:8080/csv/gw/download?status=CONNECTED
			: it will download CONNECTED gateway list

Download case 2		: http://localhost:8080/csv/gw/download?status=DISCONNECTED
			: it will download DISCONNECTED gateway list

Download  case 3	: http://localhost:8080/csv/gw/download?status=NOT_READY
			: it will download NOT_READY gateway list

Download  case 4	: http://localhost:8080/csv/gw/download?status=UNREGISTERED
			: it will download UNREGISTERED gateway list

Download  case 5	: http://localhost:8080/csv/gw/download?status=ALL
			: it will also download all gateway list

Download  case 6	: http://localhost:8080/csv/gw/download?status=ANY-OTHER-PARAM
			: nothing downloadable
			
Download  case 7	: http://localhost:8080/csv/gw/download
			: without status parameter nothing downloadable



Service-2-STEP-2	: http://localhost:8080/csv/gw/run
Service description	: paste gw_connect.csv  file inside C:\env\csv_data  folder and run above service,it will execute prepared gateway list. CSV filename must be gw_connect.csv 
			: gw_connect.csv must not be left open while executing above process, otherwise it will not delete processed file and throw warning  -  
			  [The process cannot access the file because it is being used by another process.]


Service-3-FINAL-STEP	: http://localhost:8080/csv/gw/export
Service description	: Export CSV executed Gateway Execution Status Report, downloadable file name would be gw_connect_job_status_CURRENT_DATE_TIME.csv 





###############################
##    AUTO EXECUTION STEPS    #
###############################

# it does not export status report - it will perform well. 
Service-1-STEP-1 	: http://localhost:8080/execute		
Service description	: it will automatically download all connected gateway & will call based on configuration provided in gw.properties file inside C:\env\
			: period & count will be read from gw.properties,you may track execution activity by checking log for detail execution,better to check with BARETAIL.exe log viewer


# it does export status report - compromised performance
Service-1-STEP-1 	: http://localhost:8080/execute/save		
Service description	: it will automatically download all connected gateway & call based on configuration provided in gw.properties file inside [C:\env\],it also saves data for exporting report.


Service-2-FINAL-STEP	: http://localhost:8080/export
Service description	: Export AUTO executed Gateway Execution Status Report, downloadable file name would be gw_connect_status_auto_saved_CURRENT_DATE_TIME.csv 

