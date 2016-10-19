/**
 *  Evening Monitor
 *
 *  Copyright 2016 Chris Miller
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
    page(name: "selectActions")
}

def selectActions() {
    dynamicPage(name: "selectActions", install: true, uninstall: true) {

        // Get the available routines
            def actions = location.helloHome?.getPhrases()*.label
            if (actions) {
            // sort them alphabetically
            actions.sort()
                    section("Time to run security check") {
    						input "theTime", "time", title: "Time to execute every day"
        				}
                    section("Doors to check"){
						input "doors", "capability.contactSensor", title: "Which Door?", multiple: true, required: true
    					}
                    section("Locks to check") {
                    	input "locks", "capability.lock", title: "Which Locks?", multiple: true, required: true
                        }
                     section("Text me at...") {
        				input("recipients", "contact", title: "Send notifications to") {
            				input "phone1", "phone", title: "Phone number?", multiple: true
        					}
                        }    
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
    schedule(theTime, checkDoor)
}

def checkDoor() {
	//Find all the doors that are open.
    def open = doors.findAll { it?.latestValue("contact") == "open" }
    //log.debug "open doors: ${open}"
    def openLocks = locks.findAll {it?.latestValue("lock") == "unlocked" }
    //log.debug "open locks: ${openLocks}"
    for (item in openLocks) {
    	open.add(item)
        //log.debug "${item}"
    }
    //log.debug "open items: ${open}"
    //Group all the open doors in a list, insert "is" or "Are" if the list contains more than one entry.
    def list = open.size() > 1 ? "are" : "is"
    if (open) {
    	//format the list and push it.
		def message = "Security Check Failed: ${open.join(', ')} ${list} open"
    	log.info message
        //sendPush(message)
        if (location.contactBookEnabled) {
        	sendNotificationToContacts(message, recipients)
    	}
    	else {
        	//sendSms(phone1, message)
        }
      }
      else {
    	log.info "Security Check Successful: No open doors or locks detected." 
    	sendPush("Evening Check Successful: No open doors or locks detected.")
	}
    
 }
// TODO: implement event handlers