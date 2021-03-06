/**
 *  Evening Monitor
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
    name: "Evening Monitor",
    namespace: "cpmiller22",
    author: "Chris Miller",
    description: "App to send alert if any doors are opened or unlocked after a specified time in the evening..",
    /*category: "Safety &amp; Security",*/
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/good-night.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/good-night.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/good-night.png"
    )

preferences{
       section("Time to run security check") {
            input name: "theTime", type: "time", title: "Time to execute every day"
        }
        section("Doors to check"){
            input name: "doors", type: "capability.contactSensor", title: "Which Door?", multiple: true, required: false
        }
        section("Locks to check") {
            input name: "locks", type: "capability.lock", title: "Which Locks?", multiple: true, required: true
        }
        section("Success Notifications") {
            input("recipients", "contact", title: "Send notifications to", required: false) {
                input name: "sendSuccessPushMessage", type: "enum", title: "Send a push notification?", options: ["Yes", "No"], required: true
                input name: "sendSuccessSMSMessage", type: "enum", title: "Send a text notification?", options: ["Yes", "No"], required: true
            }
        }
        section("Fail Notifications") {
            input("recipients", "contact", title: "Send notifications to", required: false) {
                input name: "sendFailMessage", type: "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
                input name: "sendFailSMSMessage", type: "enum", title: "Send a text notification?", options: ["Yes", "No"], required: true
            }
        }
        section("Phone Numbers") {
            input("recipients", "contact", title: "Send notifications to") {
                input name: "phone1", type: "phone", title: "Phone number 1", multiple: true, required: false
                input name: "phone2", type: "phone", title: "Phone number 2", multiple: true, required: false
            }
        }
}


def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    schedule(theTime, performCheck)
}

def performCheck() {

	def openDevices = getStatus()
    
    if (openDevices) {
        //format the open devices list for notifications.
        def list = openDevices.size() > 1 ? "are" : "is"
		def message = "Evening Check Failed: ${openDevices.join(', ')} ${list} open"
    	log.info message
        sendFailMessage(message)
 	} else {
		def message = "Evening Check Successful: No open doors or locks detected."
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