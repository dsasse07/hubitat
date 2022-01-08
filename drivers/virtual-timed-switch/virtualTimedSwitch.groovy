//https://github.com/

//Force State Update preference will send an event everytime you manually push a form button or app tries to do something with the device.  Ie.  If the device is already On, and an app tries to turn it On, it will send a On/Open/Motion/Present event. 

metadata {
    definition (name: "Virtual Timed Switch", namespace: "dsasse07", author: "Daniel Sasse") {
        capability "Switch"		//"on", "off"
        capability "TimedSession"
 	}   
    
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "forceUpdate", type: "bool", title: "Force State Update", description: "Send event everytime, regardless of current status. ie Send/Do On even if already On.",  defaultValue: false
        input name: "autoOffTimer", type: "integer", title: "Enable Auto-Off", description: "Automatically turns off the specified time in seconds.", defaultValue: 0
    } 
}

def off() {
    sendEvent(name: "switch", value: "off", isStateChange: forceUpdate)
    sendEvent(name: "timedSession", value: "stop")
    logTxt "turned Off"
}

def on() {
    sendEvent(name: "switch", value: "on", isStateChange: forceUpdate)
    logTxt "turned On"
    if (autoOff.toInteger()>0){
        setTimeRemaining(autoOff.toInteger())
        sendEvent(name: "timedSession", value: "start")
        runIn(autoOff.toInteger(), off)
    }
}

/**
Attributes

sessionStatus - ENUM ["stopped", "canceled", "running", "paused"]
timeRemaining - NUMBER
Commands

cancel()
pause()
setTimeRemaining(NUMBER)
NUMBER (NUMBER) - NUMBER
start()
stop()
*/


def installed() {
}

void logTxt(String msg) {
	if (logEnable) log.info "${device.displayName} ${msg}"
}

//Use only if you are on 2.2.8.141 or later.  device.deleteCurrentState() is new to that version and will not work on older versions.  
def configure(){  
    logTxt "configured. State values reset."
}
