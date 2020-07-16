/**
 *  Presence Alert
 *
 *  Copyright 2020 Chris Miller
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Presence Alert",
    namespace: "cpmiller22",
    author: "Chris Miller",
    description: "Identifies when mobile presence senors are all away, then performs a check to determine if all doors and locks are closed and provides and then sends an alert",
    category: "",
    //iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    //iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    //iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage@2x.png"
    )
    

preferences{
	page("Settings","") {
        section("Devices for presence") {
            input "presence", "capability.presenceSensor", title: "Which sensor?", multiple: true, required: true
        }
        section("Doors to check"){
            input "doors", "capability.contactSensor", title: "Which Door?", multiple: true, required: false
        }
        section("Locks to check") {
            input "locks", "capability.lock", title: "Which Locks?", multiple: true, required: true
        }
        section("Success Notifications") {
            input("recipients", "contact", title: "Send notifications to", required: false) {
                input "sendSuccessPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: true
                input "sendSuccessSMSMessage", "enum", title: "Send a text notification?", options: ["Yes", "No"], required: true
            }
        }
        section("Fail Notifications") {
            input("recipients", "contact", title: "Send notifications to", required: false) {
                input "sendFailMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
                input "sendFailSMSMessage", "enum", title: "Send a text notification?", options: ["Yes", "No"], required: true
            }
        }
        section("Phone Numbers") {
            input("recipients", "contact", title: "Send notifications to") {
                input "phone1", "phone", title: "Phone number 1", multiple: true, required: false
                input "phone2", "phone", title: "Phone number 2", multiple: true, required: false
            }
        }   
	}
}

def installed() {
	subscribe(presence, "presence", presenceHandler)
}

def updated() {
	unsubscribe()
	subscribe(presence, "presence", presenceHandler)
}

def presenceHandler(evt) {
	if (evt.value == "present") {
        log.debug "${evt.displayName} has arrived at the ${location}"
	} else if (evt.value == "not present") {
		log.debug "${evt.displayName} has left the ${location}"
        if (everyoneIsAway()) {
                log.debug "Everyone is away"
                performCheck()
            }
	}
}

private everyoneIsAway() {
    def result = true
    // iterate over our people variable that we defined
    // in the preferences method
    for (device in presence) {
    //log.debug "${device.label} is ${device.currentPresence}"
        if (device.currentPresence == "present") {
            log.debug "${device} is still at ${location}"
            // someone is present, so set our our result
            // variable to false and terminate the loop.
            result = false
            break
        }
    }
    //log.debug "everyoneIsAway: $result"
    return result
}

private performCheck() {

	def openDevices = getStatus()
    
    if (openDevices) {
        //format the open devices list for notifications.
        def list = openDevices.size() > 1 ? "are" : "is"
		def message = "Away Check Failed: ${openDevices.join(', ')} ${list} open"
    	log.info message
        sendFailMessage(message)
 	} else {
		def message = "Away Check Successful: No open doors or locks detected."
    	log.info message 
    	sendSuccessMessage(message)
	}
}

private getStatus() {
	//Find all the doors that are open.
    def openDevices = doors.findAll { it?.latestValue("contact") == "open" }
    //log.debug "open doors: ${openDevices}"
    def openLocks = locks.findAll {it?.latestValue("lock") == "unlocked" }
    //log.debug "open locks: ${openLocks}"
    //Find all the open locks and add them to the open device list.
    for (item in openLocks) {
    	openDevices.add(item)
    }
    //log.debug "open items: ${open}"
    //Group all the open doors & locks in a list, insert "is" or "Are" if the list contains more than one entry.
    //def list = openDevices.size() > 1 ? "are" : "is"
    return openDevices
}

private sendSuccessMessage(msg) {
        if (sendSuccessPushMessage == "Yes") {
        	sendPush(msg)
            }
        if (sendSuccessSMSMessage == "Yes") {
        	sendSMS(phone1, msg)
            sendSMS(phone2, msg)
            }
 }
 
private sendFailMessage(msg) {
        if (sendFailPushMessage == "Yes") {
        	sendPush(msg)
            }
        if (sendFailSMSMessage == "Yes") {
        	sendSms(phone1, msg)
            sendSms(phone2, msg)
            }
 }