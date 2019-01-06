/**
 * Das Keyboard
 *
 * Copyright 2018 Jules Taplin
 * Author: Jules Taplin
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * v0.01 - Initial Release
 */


 preferences {
	//input(name:"networkAddress","string","IP Address",required:true)
	input(name:"clientID","string","Client ID",required:true)
	input(name:"clientSecret","string","Client Secret",required:true)
 }
metadata {
	definition (name: "Das Keyboard", namespace: "uk.org.ourhouse", author: "Jules Taplin") {
		capability "Notification"
//		capability "Polling"
//		capability "Refresh"
		command "setOffline"
		command "setOnline"
		command "connectToAPI"
		}

		simulator {
			// Not sure what this does yet.
		}

		tiles (scale: 2) {
		    	multiAttributeTile(name:"rich-control"){
			tileAttribute ("mode", key: "PRIMARY_CONTROL") {
	            attributeState "online", label: "Das Keyboard", action: "", icon:  null, backgroundColor: "#F3C200"
                attributeState "offline", label: "Das Keyboard", action: "", icon: null, backgroundColor: "#F3F3F3"
			}

        }
		//valueTile("networkAddress", "device.currentIP", decoration: "flat", height: 2, width: 4, inactiveLabel: false) {
		//	state "default", label:'${currentValue}', height: 1, width: 2, inactiveLabel: false
		//}

       		standardTile("presence", "device.mode", width: 2, height: 2, canChangeBackground: true) {
           		state "default", icon:null
		}

		main (["presence"])
		details(["rich-control","networkAddress"])
	}

}
/*
private getApiPath() { ":27301/api/1.0/" }
private getCreateSignalUrl() { apiPath+"/signal" }
private getSignalsUrl() { apiPath+"/signals" }
private getShadowsUrl() { apiPath+"/sigals/shadows" }
*/
private getVendorTokenPath()		{ "${apiUrl}/oauth/1.4/token" }
private getVendorSignalPath()		{ "${apiUrl}/api/1.0/signals" }
private getApiUrl()	        		{ "https://q.daskeyboard.com" }
private getAuthScope() 					{ [ "basic" ] }
private	getVendorName()				{ "Das Keyboard" }


// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
    if (description)
    {
    	unschedule("setOffline")
    }
	// TODO: handle 'battery' attribute
	// TODO: handle 'button' attribute
	// TODO: handle 'status' attribute
	// TODO: handle 'level' attribute
	// TODO: handle 'level' attribute

}

// handle commands
def setOnline()
{
	log.debug("set online");
    sendEvent(name:"mode", value:"online")
  	unschedule("setOffline")
}
def setOffline(){
	log.debug("set offline");
    sendEvent(name:"mode", value:"offline")
}

def connectToAPI()
{
	log.debug "Connecting to API"
	getTokens()
}


def deviceNotification(data)
{

	       def createSignalParams = [
            //method: 'POST',
            uri   : apiUrl,
            path  : vendorSignalPath,
			contentType: "application/json",
			requestContentType: "application/json",
			headers: ["Authorization": "Bearer ${state.authToken}"],
			   body : [name: data,
                    pid: "DK5QPID",
					zoneId: "151", // Space Bar
					color: "#0F0",
					effect: "BLINK"
        ]
]
	        try {
            httpPost(createSignalParams) { resp ->
                if(resp.status == 200) {
                    log.debug "Notification Accepted"
					log.debug "Payload was: ${resp.data}"
            }
			}

       } catch (groovyx.net.http.HttpResponseException e) {
            log.error "sendNotificationError() >> Error: e.statusCode ${e.statusCode}"
            log.debug e.response.data;

        }




}


def getTokens() {
    log.debug "Acquiring Access Token & Refresh Token"

    if(0 /* state.refreshToken */) {
        log.warn "RefreshToken already exists"
    } else {
        def refreshParams = [
            //method: 'POST',
            uri   : apiUrl,
            path  : vendorTokenPath,
			contentType: "application/json",
            body : [grant_type: 'client_credentials',
                    //refresh_token: "${state.refreshToken}",
                    client_id : clientID,
                    client_secret: clientSecret,
//                    redirect_uri: callbackUrl],
        ]
]
        log.debug refreshParams

        def notificationMessage = "is disconnected from SmartThings, because the access credential changed or was lost. Please go to the LaMetric (Connect) SmartApp and re-enter your account login credentials."
        //changed to httpPost
        try {
            def jsonMap
            httpPost(refreshParams) { resp ->
                if(resp.status == 200) {
                    log.debug "Token refreshed...calling saved RestAction now!"
					log.debug "Data = ${resp.data}"
//                    jsonMap = resp.data
                    if(resp.data) {
//					log.debug "Data = ${resp.data}"

                        state.refreshToken = resp.data['refresh_token']
                        state.authToken = resp.data['access_token']
						log.debug("Tokens Grabbed - refresh = ${resp?.data?.refresh_token}, authToken = ${resp?.data?.access_token}")
						runIn(resp.data['expires_in'],"getTokens")
/*                        if(state.action && state.action != "") {
                            log.debug "Executing next action: ${state.action}"

                            "${state.action}"()

                            state.action = ""
                        }
*/
                    } else {
                        log.warn ("No data in refresh token!");
                    }
                    state.action = ""
                }
            }
        } catch (groovyx.net.http.HttpResponseException e) {
            log.error "refreshAuthToken() >> Error: e.statusCode ${e.statusCode}"
            log.debug e.response.data;
            def reAttemptPeriod = 300 // in sec
            if (e.statusCode != 401) { // this issue might comes from exceed 20sec app execution, connectivity issue etc.
                runIn(reAttemptPeriod, "getTokens")
            } else if (e.statusCode == 401) { // unauthorized
                state.reAttempt = state.reAttempt + 1
                log.warn "reAttempt refreshAuthToken to try = ${state.reAttempt}"
                if (state.reAttempt <= 3) {
                    runIn(reAttemptPeriod, "getTokens")
                } else {
                    state.reAttempt = 0
                }
            }
        }
    }


}
