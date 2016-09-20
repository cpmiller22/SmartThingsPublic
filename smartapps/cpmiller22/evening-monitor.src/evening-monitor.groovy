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
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


/*preferences {
	section("Title") {
		// TODO: put inputs here
	}
}
*/
/*preferences {
/*    section("Turn on when motion detected:") {
        input "themotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Time to check") {
    	input "theTime", "time", title: "Time to execute every day"
        }
    section("Items to check") {
        input "theGarageDoor", "capability.garageDoorControl", required: true
    }
}
*/
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
                    /*section("Routine") {
                        log.trace actions
                		// use the actions as the options for an enum input
                		input "action", "enum", title: "Select a trigger routine", options: actions
                    	} */
                    section("Time to run security check") {
    						input "theTime", "time", title: "Time to execute every day"
        				}
                    section("Doors to check"){
						input "doors", "capability.contactSensor", title: "Which Door?", multiple: true, required: true
    					}
                    section("Locks to check") {
                    	input "locks", "capability.lock", title: "Which Locks?", multiple: true, required: true
                        }
                    
                    /** Will fix this section later...
                    section("Notification"){
        				input "sendPush", "bool",title: "Send Push Notification?", required: true
        				input "phone", "phone", title: "Phone Number (for SMS, optional)", required: false
        				input "pushAndPhone", "enum", title: "Both Push and SMS?", required: false, options: ["Yes", "No"]
   						}
                        **/    
                    
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
	// TODO: subscribe to attributes, devices, locations, etc.
    /*subscribe(themotion, "motion.active", motionDetectedHandler)*/
    schedule(theTime, checkDoor)
}

def checkDoor() {
	//Find all the doors that are open.
    def open = doors.findAll { it?.latestValue("contact") == "open" }
    //log.debug "open doors: ${open}"
    def openLocks = locks.findAll {it?.latestValue("lock") == "unlocked" }
    //log.debug "open locks: ${openLocks}"
    //open.add("test item")
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
        //Hard code push notification for now. 
        if (sendPush) {
     		sendPush(message)
     		}
      }else {
    log.info "Security Check Successful: No open doors or locks detected." 
    sendNotificationEvent("Security Check Successful: No open doors or locks detected.")
	}
    
 }
// TODO: implement event handlers