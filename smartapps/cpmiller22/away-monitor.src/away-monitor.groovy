/**
 *  Away Monitor
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
    name: "Away Monitor",
    namespace: "cpmiller22",
    author: "Chris Miller",
    description: "App to monitor and alert if doors are open and/or unlocked when people are away",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    
// TODO: implement event handlers
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
                    section("Devices for presence") {
    						input "people", "capability.presenceSensor", multiple: true, required: true
        				}
                    section("Doors to check"){
						input "doors", "capability.contactSensor", title: "Which Door?", multiple: true, required: true
    					}
                    section("Locks to check") {
                    	input "locks", "capability.lock", title: "Which Locks?", multiple: true, required: true
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
    subscribe(presence, "presence", checkSensors)
}

def checkSensors(evt) {
	 log.debug "evt.name: $evt.value"
     if (evt.value == "not present") {
            log.debug "checking if everyone is away"
            if (everyoneIsAway()) {
                log.debug "Everyone is away, checking locks/doors"
                checkDoor()
            }
        else {
            log.debug "Not everyone is away"
        }
    }
    else {
        log.debug "present event; doing nothing"
    }
}

private everyoneIsAway() {
    def result = true
    // iterate over our people variable that we defined
    // in the preferences method
    for (person in people) {
    	log.debug person
        if (person.currentPresence == "present") {
            // someone is present, so set our our result
            // variable to false and terminate the loop.
            result = false
            break
        }
    }
    log.debug "everyoneIsAway: $result"
    return result
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