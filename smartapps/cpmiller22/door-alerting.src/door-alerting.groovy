// Automatically generated. Make future change here.
definition(
    name: "Door Alerting",
    namespace: "cpmiller22",
    author: "Chris Miller",
    description: "Send an alert when an unused door stays open for more than a certain amount of time.",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

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
                    section("Doors to check"){
						input "doors", "capability.contactSensor", title: "Which Door?", multiple: true, required: true
    					}
                    section("How long to wait") {
                    	input "alertDelay", "number", title: "Alert Delay?", multiple: true, required: true
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
	subscribe(doors, "contact", doorEvent)
}

def updated() {
	unsubscribe()
	subscribe(doors, "contact", doorEvent)
}

def doorEvent(evt) {
	//log.debug evt.value
    if (evt.value == "open") {
    log.debug "door is open"
    def alertDelaySeconds = alertDelay * 60
    log.debug "delay is seconds ${alertDelaySeconds}"
    runIn(alertDelaySeconds, "openTooLong")
	}
	else {
    log.debug "closed event do nothing"
    }
}

def openTooLong(evt) {
	//log.debug "Door open too long!"
    checkDoor()
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
        sendPush(message)
         if (location.contactBookEnabled) {
        	sendNotificationToContacts(message, recipients)
    	}
    	else {
        	//sendSms(phone1, message)
        }
      }
      else {
    	log.info "Security Check Successful: No open doors or locks detected." 
    	//sendPush("Evening Check Successful: No open doors or locks detected.")
	}
    
 }